package com.rev.app.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {

    @GetMapping("/access-denied")
    public String showAccessDeniedPage(HttpSession session, Model model) {
        String message = (String) session.getAttribute("accessDeniedMessage");
        if (message == null) {
            message = "You do not have permission to access this resource.";
        }
        model.addAttribute("errorMessage", message);
        // Clear the message after displaying it
        session.removeAttribute("accessDeniedMessage");
        return "access-denied";
    }
}
