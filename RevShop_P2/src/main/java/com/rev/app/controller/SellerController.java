package com.rev.app.controller;

import com.rev.app.entity.*;
import com.rev.app.repository.IOrderItemRepository;
import com.rev.app.service.Interface.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/seller")
@Slf4j
public class SellerController {

    @Autowired
    private IProductService productService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private INotificationService notificationService;
    @Autowired
    private IOrderItemRepository orderItemRepo; // Using repo directly for simplicity to get items by product

    // --- Dashboard Overview ---
    @GetMapping("/dashboard")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showDashboard(HttpSession session, Model model) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");
        log.info("Seller {} loaded dashboard.", seller.getEmail());

        List<Product> products = productService.getProductsBySeller(seller.getId());
        model.addAttribute("totalProducts", products.size());

        long lowStockCount = products.stream().filter(p -> p.getQuantity() <= 5).count();
        model.addAttribute("lowStockCount", lowStockCount);

        // Efficiently find all OrderItems for this seller in one query
        List<OrderItem> allSellerOrderItems = orderItemRepo.findByProductSellerId(seller.getId());

        long totalOrders = allSellerOrderItems.stream()
                .filter(oi -> oi.getStatus() != OrderItem.OrderStatus.CANCELLED 
                           && oi.getStatus() != OrderItem.OrderStatus.RETURNED
                           && oi.getStatus() != OrderItem.OrderStatus.REFUNDED)
                .map(oi -> oi.getOrder().getId())
                .distinct()
                .count();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (OrderItem oi : allSellerOrderItems) {
            if (oi.getStatus() != OrderItem.OrderStatus.CANCELLED 
                && oi.getStatus() != OrderItem.OrderStatus.RETURNED
                && oi.getStatus() != OrderItem.OrderStatus.REFUNDED) {
                BigDecimal itemTotal = oi.getPrice().multiply(new BigDecimal(oi.getQuantity()));
                totalRevenue = totalRevenue.add(itemTotal);
            }
        }

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", totalRevenue.setScale(2, java.math.RoundingMode.HALF_UP).toString());

        // Recent generic orders
        List<OrderItem> recentOrderItems = allSellerOrderItems.stream()
                .sorted((oi1, oi2) -> oi2.getOrder().getCreatedAt().compareTo(oi1.getOrder().getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentOrderItems", recentOrderItems);

        // Notifications
        List<Notification> notifications = notificationService.getNotificationsForUser(seller.getId());
        model.addAttribute("notifications", notifications);

        return "seller/dashboard";
    }

    // --- Inventory Management ---
    @GetMapping("/inventory")
    public String showInventory(HttpSession session, Model model) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");
        log.info("Seller {} accessed inventory page.", seller.getEmail());

        List<Product> products = productService.getProductsBySeller(seller.getId());
        model.addAttribute("products", products);

        return "seller/inventory";
    }

    @GetMapping("/product/new")
    public String showAddProductForm(HttpSession session, Model model) {
        if (!isSeller(session))
            return "redirect:/login";

        model.addAttribute("product", new Product());
        model.addAttribute("categories", Product.Category.values());
        return "seller/product-form";
    }

    @PostMapping("/product/add")
    public String addProduct(@ModelAttribute Product product,
                             @RequestParam String categoryName,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");
        log.info("Seller {} is adding a new product: {}", seller.getEmail(), product.getName());

        try {
            // Priority 1: Physical File Upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = saveImageFile(imageFile);
                product.setImageUrl(uploadedUrl);
            }
            // Priority 2: Automatically maps to product.getImageUrl() from Thymeleaf form
            else if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
                throw new RuntimeException("An Image File or an Image URL is required.");
            }

            product.setCategory(Product.Category.valueOf(categoryName));
            product.setSeller(seller);
            // created and updated normally handled by @PrePersist
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            productService.addProduct(product);
            log.info("Product {} added successfully by {}", product.getName(), seller.getEmail());
            redirectAttributes.addFlashAttribute("msg", "Product added successfully!");
        } catch (Exception e) {
            log.error("Failed to add product {}: {}", product.getName(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to add product: " + e.getMessage());
        }
        return "redirect:/seller/inventory";
    }

    @GetMapping("/product/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");

        Product product = productService.getProductById(id);
        if (!product.getSeller().getId().equals(seller.getId())) {
            log.warn("Seller {} attempted unauthorized edit of product {}", seller.getEmail(), id);
            redirectAttributes.addFlashAttribute("error", "Unauthorized to edit this product.");
            return "redirect:/seller/inventory";
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", Product.Category.values());
        return "seller/product-form";
    }

    @PostMapping("/product/update")
    public String updateProduct(@ModelAttribute Product product,
                                @RequestParam String categoryName,
                                @RequestParam("imageFile") MultipartFile imageFile,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");
        log.info("Seller {} updating product ID {}", seller.getEmail(), product.getId());

        try {
            Product existingProduct = productService.getProductById(product.getId());
            if (!existingProduct.getSeller().getId().equals(seller.getId())) {
                log.warn("Unauthorized attempt by {} to update product {}", seller.getEmail(), product.getId());
                throw new RuntimeException("Unauthorized");
            }

            if (!existingProduct.getSeller().getId().equals(seller.getId())) {
                throw new RuntimeException("Unauthorized");
            }

            existingProduct.setName(product.getName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setDiscountedPrice(product.getDiscountedPrice());
            existingProduct.setQuantity(product.getQuantity());
            // Priority 1: Physical File Upload
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = saveImageFile(imageFile);
                existingProduct.setImageUrl(uploadedUrl);
            } 
            // Priority 2: Let it fall back to the given String URL in the form
            else if (product.getImageUrl() != null && !product.getImageUrl().trim().isEmpty()) {
                existingProduct.setImageUrl(product.getImageUrl());
            }

            existingProduct.setCategory(Product.Category.valueOf(categoryName));
            existingProduct.setUpdatedAt(LocalDateTime.now());

            productService.updateProduct(existingProduct);
            redirectAttributes.addFlashAttribute("msg", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update product.");
        }
        return "redirect:/seller/inventory";
    }

    @PostMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");

        try {
            Product product = productService.getProductById(id);
            if (!product.getSeller().getId().equals(seller.getId())) {
                throw new RuntimeException("Unauthorized");
            }
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("msg", "Product deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to delete product (It might be associated with orders).");
        }
        return "redirect:/seller/inventory";
    }

    // --- Order Management ---
    @GetMapping("/orders")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showSellerOrders(HttpSession session, Model model) {
        if (!isSeller(session))
            return "redirect:/login";
        User seller = (User) session.getAttribute("user");

        List<OrderItem> sellerOrderItems = orderItemRepo.findByProductSellerId(seller.getId());

        // Sort by order date desc
        sellerOrderItems.sort((oi1, oi2) -> oi2.getOrder().getCreatedAt().compareTo(oi1.getOrder().getCreatedAt()));

        model.addAttribute("sellerOrderItems", sellerOrderItems);
        return "seller/seller-orders";
    }

    @PostMapping("/order/updateStatus")
    public String updateOrderStatus(@RequestParam Long orderItemId, @RequestParam String status,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isSeller(session))
            return "redirect:/login";

        try {
            orderService.updateOrderItemStatus(orderItemId, status);
            redirectAttributes.addFlashAttribute("msg", "Order item status updated to " + status);
        } catch (Exception e) {
            log.error("Failed to update item status to {}: {}", status, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update order item status.");
        }
        return "redirect:/seller/orders";
    }

    private boolean isSeller(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && user.getRole() == User.Role.SELLER;
    }

    private String saveImageFile(MultipartFile file) throws IOException {
        String uploadDir = "uploads/";
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(newFilename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + newFilename;
    }
}
