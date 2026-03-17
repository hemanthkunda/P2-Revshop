
package com.rev.app.service.Impl;

import com.rev.app.entity.Cart;
import com.rev.app.entity.CartItem;
import com.rev.app.entity.Product;
import com.rev.app.entity.User;
import com.rev.app.repository.ICartItemRepository;
import com.rev.app.repository.ICartRepository;
import com.rev.app.repository.IProductRepository;
import com.rev.app.repository.IUserRepository;
import com.rev.app.service.Interface.ICartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class CartServiceImpl implements ICartService {

    @Autowired
    private ICartRepository cartRepo;
    @Autowired
    private ICartItemRepository itemRepo;
    @Autowired
    private IProductRepository productRepo;
    @Autowired
    private IUserRepository userRepo;

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Integer qty) {

        log.info("Adding {} of product {} to cart for user {}", qty, productId, userId);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Cart cart = cartRepo.findByUser(user)
                .orElseGet(() -> {
                    log.debug("Creating new cart for user {}", userId);
                    return cartRepo.save(Cart.builder().user(user).build());
                });

        // 🔹 Load cart items from DB
        List<CartItem> items = itemRepo.findByCart(cart);

        CartItem existingItem = items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + qty);
            itemRepo.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(qty);
            itemRepo.save(newItem);
        }
    }
    @Override
    public Cart getCartByUserId(Long userId) {
        log.debug("Fetching cart for user {}", userId);
        User u = userRepo.findById(userId).orElseThrow(() -> {
            log.warn("User {} not found when fetching cart.", userId);
            return new RuntimeException("User not found");
        });
        return cartRepo.findByUser(u).orElse(null);
    }

    @Override
    @Transactional
    public void updateCartItemQuantity(Long cartItemId, Integer qty) {
        log.info("Updating cart item {} to quantity {}", cartItemId, qty);
        CartItem item = itemRepo.findById(cartItemId).orElseThrow(() -> {
            log.warn("CartItem {} not found for update.", cartItemId);
            return new RuntimeException("CartItem not found");
        });
        if (qty <= 0) {
            removeCartItem(cartItemId);
        } else {
            item.setQuantity(qty);
            itemRepo.save(item);
        }
    }

    @Override
    @Transactional
    public void removeCartItem(Long cartItemId) {
        log.info("Removing cart item {}", cartItemId);
        CartItem item = itemRepo.findById(cartItemId).orElse(null);
        if (item != null) {
            Cart cart = item.getCart();
            cart.getItems().remove(item);
            cartRepo.save(cart);
        }
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        Cart cart = getCartByUserId(userId);
        if (cart != null && cart.getItems() != null) {
            cart.getItems().clear();
            cartRepo.save(cart);
        }
    }
}
