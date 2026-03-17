package com.rev.app.controller;

import com.rev.app.entity.*;
import com.rev.app.repository.IOrderItemRepository;
import com.rev.app.service.Interface.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/")
@Slf4j
public class BuyerController {

    @Autowired
    private IProductService productService;
    @Autowired
    private ICartService cartService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private IAddressService addressService;
    @Autowired
    private IFavoriteService favoriteService;
    @Autowired
    private IReviewService reviewService;
    @Autowired
    private IOrderItemRepository orderItemRepo;

    // --- Product Catalog ---
    @GetMapping("/products")
    public String showProducts(@RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            Model model) {
        log.info("Loading product catalog. Category filter: {}, Keyword search: {}", category, keyword);
        List<Product> products = productService.getAllProducts();

        if (category != null && !category.isEmpty()) {
            products = products.stream().filter(p -> p.getCategory().name().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }
        if (keyword != null && !keyword.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                            p.getDescription().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if ("price_asc".equals(sort)) {
            products.sort((p1, p2) -> Double.compare(p1.getPrice().doubleValue(), p2.getPrice().doubleValue()));
        } else if ("price_desc".equals(sort)) {
            products.sort((p1, p2) -> Double.compare(p2.getPrice().doubleValue(), p1.getPrice().doubleValue()));
        } else if ("newest".equals(sort)) {
            products.sort((p1, p2) -> {
                java.time.LocalDateTime d1 = p1.getCreatedAt() != null ? p1.getCreatedAt() : java.time.LocalDateTime.MIN;
                java.time.LocalDateTime d2 = p2.getCreatedAt() != null ? p2.getCreatedAt() : java.time.LocalDateTime.MIN;
                return d2.compareTo(d1);
            });
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", Product.Category.values());
        return "buyer/products";
    }

    @GetMapping("/product/{id}")
    public String showProductDetails(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);

        List<Review> reviews = reviewService.getReviewsByProductId(id);
        model.addAttribute("reviews", reviews);
        model.addAttribute("newReview", new Review());

        return "buyer/product-details";
    }

    @PostMapping("/product/review")
    public String addReview(@ModelAttribute("newReview") Review review, @RequestParam Long productId,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        try {
            reviewService.addReview(review, user.getId(), productId);
            redirectAttributes.addFlashAttribute("msg", "Review added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not add review.");
        }
        return "redirect:/product/" + productId;
    }

    // --- Cart Management ---
    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        Cart cart = cartService.getCartByUserId(user.getId());
        model.addAttribute("cart", cart);

        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            double subtotal = cart.getItems().stream().mapToDouble(i -> {
                double price = (i.getProduct().getDiscountedPrice() != null)
                        ? i.getProduct().getDiscountedPrice().doubleValue()
                        : i.getProduct().getPrice().doubleValue();
                return price * i.getQuantity();
            }).sum();

            model.addAttribute("subtotal", String.format("%.2f", subtotal));
            model.addAttribute("total", String.format("%.2f", subtotal));
        }

        return "buyer/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId, @RequestParam int quantity,
                            HttpSession session, RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");

        System.out.println("USER IN SESSION = " + user);   // ⭐ ADD THIS

        if (user == null) {
            return "redirect:/login";
        }

        cartService.addToCart(user.getId(), productId, quantity);

        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long cartItemId, @RequestParam int quantity,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        cartService.updateCartItemQuantity(cartItemId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long cartItemId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        cartService.removeCartItem(cartItemId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        cartService.clearCart(user.getId());
        return "redirect:/cart";
    }

    // --- Checkout & Orders ---
    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        Cart cart = cartService.getCartByUserId(user.getId());
        if (cart == null || cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Your cart is empty.");
            return "redirect:/cart";
        }

        List<Address> addresses = addressService.getAddressesByUserId(user.getId());
        model.addAttribute("addresses", addresses);
        model.addAttribute("cart", cart);

        // Recalculate totals
        double subtotal = cart.getItems().stream().mapToDouble(i -> {
            double price = (i.getProduct().getDiscountedPrice() != null)
                    ? i.getProduct().getDiscountedPrice().doubleValue()
                    : i.getProduct().getPrice().doubleValue();
            return price * i.getQuantity();
        }).sum();

        model.addAttribute("subtotal", String.format("%.2f", subtotal));
        model.addAttribute("total", String.format("%.2f", subtotal));

        return "buyer/checkout";
    }

    @PostMapping("/checkout/process")
    public String processCheckout(@RequestParam Long shippingAddressId,
            @RequestParam(required = false) Long billingAddressId,
            @RequestParam String paymentMethod,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("Unauthenticated user attempted to process checkout.");
            return "redirect:/login";
        }

        log.info("User {} processing checkout payment {} to address {}", user.getEmail(), paymentMethod,
                shippingAddressId);
        if (billingAddressId == null) {
            billingAddressId = shippingAddressId; // Same as shipping
        }

        try {
            orderService.placeOrder(user.getId(), shippingAddressId, billingAddressId, paymentMethod);
            log.info("Order placed successfully by user {}", user.getEmail());
            redirectAttributes.addFlashAttribute("msg", "Order placed successfully!");
            return "redirect:/orders";
        } catch (Exception e) {
            log.error("Failed to process checkout for user {}: {}", user.getEmail(), e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/checkout";
        }
    }

    @GetMapping("/orders")
    public String viewOrders(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        List<Order> orders = orderService.getOrdersByUser(user.getId());
        model.addAttribute("orders", orders);

        // Fetch order items for each order
        Map<Long, List<OrderItem>> orderItemsMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, orderItemRepo::findByOrder));
        model.addAttribute("orderItemsMap", orderItemsMap);

        // Calculate Returnable Orders (At least one item is Delivered & Within 7 Days)
        List<Long> returnableOrderIds = new ArrayList<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemsMap.get(o.getId());
            boolean anyDelivered = items != null && items.stream().anyMatch(i -> i.getStatus() == OrderItem.OrderStatus.DELIVERED);
            
            if (anyDelivered) {
                // Return window is simplified to 7 days from order creation for now
                long daysSinceOrder = ChronoUnit.DAYS.between(o.getCreatedAt(), LocalDateTime.now());
                if (daysSinceOrder <= 7) {
                    returnableOrderIds.add(o.getId());
                }
            }
        }
        model.addAttribute("returnableOrderIds", returnableOrderIds);
        
        // Summary Metrics
        long totalOrders = orders.size();
        int totalItemsBought = 0;
        double totalSpentValue = 0.0;
        String latestStatusLabel = "N/A";

        if (!orders.isEmpty()) {
            List<OrderItem> firstOrderItems = orderItemsMap.get(orders.get(0).getId());
            if (firstOrderItems != null && !firstOrderItems.isEmpty()) {
                latestStatusLabel = firstOrderItems.get(0).getStatus().name();
            }
            
            for (List<OrderItem> itemsList : orderItemsMap.values()) {
                for (OrderItem item : itemsList) {
                    if (item.getStatus() != OrderItem.OrderStatus.CANCELLED 
                        && item.getStatus() != OrderItem.OrderStatus.RETURNED
                        && item.getStatus() != OrderItem.OrderStatus.REFUNDED) {
                        totalItemsBought += item.getQuantity();
                        totalSpentValue += item.getPrice().doubleValue() * item.getQuantity();
                    }
                }
            }
        }

        model.addAttribute("totalOrdersCount", totalOrders);
        model.addAttribute("totalItemsCount", totalItemsBought);
        model.addAttribute("totalSpent", String.format("%.2f", totalSpentValue));
        model.addAttribute("latestStatus", latestStatusLabel);

        return "buyer/orders";
    }

    // --- Cancel Order ---
    @GetMapping("/orders/item/cancel/{id}")
    public String cancelOrderItem(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        try {
            orderService.cancelOrderItem(id);
            redirectAttributes.addFlashAttribute("msg", "Item cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/orders/item/return/{id}")
    public String returnOrderItem(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        try {
            orderService.returnOrderItem(id);
            redirectAttributes.addFlashAttribute("msg", "Return request submitted for the item. Pending seller approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/orders/item/cancel-return/{id}")
    public String cancelReturnOrderItem(@PathVariable Long id,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        try {
            orderService.cancelReturnRequest(id);
            redirectAttributes.addFlashAttribute("msg", "Return request cancelled. Item status restored.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders";
    }
    // --- Favorites ---
    @PostMapping("/favorites/add")
    public String addToFavorites(@RequestParam Long productId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        try {
            favoriteService.addToFavorites(user.getId(), productId);
            redirectAttributes.addFlashAttribute("msg", "Product added to wishlist!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not add to wishlist.");
        }
        return "redirect:/product/" + productId;
    }

    @GetMapping("/favorites")
    public String viewFavorites(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        List<Favorite> favorites = favoriteService.getFavoritesByUserId(user.getId());
        model.addAttribute("favorites", favorites);
        return "buyer/favorites";
    }

    @PostMapping("/favorites/remove")
    public String removeFromFavorites(@RequestParam Long productId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        try {
            favoriteService.removeFromFavorites(user.getId(), productId);
            redirectAttributes.addFlashAttribute("msg", "Product removed from wishlist.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not remove from wishlist.");
        }
        return "redirect:/favorites";
    }

    @GetMapping("/order/invoice/{id}")
    public String showInvoice(@PathVariable Long id,
                             @RequestParam(required = false) Long orderItemId,
                             HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null)
            return "redirect:/login";

        Order order = orderService.getOrderById(id);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            log.warn("Unauthorized access attempt to invoice {} by user {}", id, user.getEmail());
            return "redirect:/orders";
        }

        List<OrderItem> items = orderItemRepo.findByOrder(order);

        // Filter for specific item if requested
        if (orderItemId != null) {
            items = items.stream()
                    .filter(i -> i.getId().equals(orderItemId))
                    .collect(Collectors.toList());
        }

        double itemsTotal = items.stream()
                .mapToDouble(i -> i.getPrice().doubleValue() * i.getQuantity())
                .sum();

        model.addAttribute("order", order);
        model.addAttribute("orderItems", items);
        model.addAttribute("itemsTotal", String.format("%.2f", itemsTotal));

        return "buyer/invoice";
    }

}
