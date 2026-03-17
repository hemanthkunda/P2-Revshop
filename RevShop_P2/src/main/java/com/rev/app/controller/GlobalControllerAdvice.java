package com.rev.app.controller;

import com.rev.app.entity.Cart;
import com.rev.app.entity.Notification;
import com.rev.app.entity.User;
import com.rev.app.service.Interface.ICartService;
import com.rev.app.service.Interface.INotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private ICartService cartService;

    @ModelAttribute("cartCount")
    public int getCartCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && user.getRole() == User.Role.BUYER) {
            Cart cart = cartService.getCartByUserId(user.getId());
            if (cart != null && cart.getItems() != null) {
                return cart.getItems().size();
            }
        }
        return 0;
    }

    @Autowired
    private INotificationService notificationService;

    @ModelAttribute("unreadNotificationCount")
    public int getUnreadNotificationCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            List<Notification> unread = notificationService.getUnreadNotificationsForUser(user.getId());
            return unread != null ? unread.size() : 0;
        }
        return 0;
    }

    @ModelAttribute("latestNotifications")
    public List<Notification> getLatestNotifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            List<Notification> all = notificationService.getNotificationsForUser(user.getId());
            if (all != null) {
                // Return top 5 most recent notifications
                return all.size() > 5 ? all.subList(0, 5) : all;
            }
        }
        return Collections.emptyList();
    }
}
