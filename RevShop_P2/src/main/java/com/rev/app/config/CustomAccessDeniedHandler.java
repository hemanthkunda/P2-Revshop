package com.rev.app.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            // Logged in but doesn't have the required role
            String message = "Access Denied: You do not have permission to access this page.";
            String role = auth.getAuthorities().toString();
            
            if (request.getRequestURI().contains("/seller") && role.contains("ROLE_BUYER")) {
                message = "You are currently logged in as a Buyer. To access Seller features, please log out and sign in with a Seller account.";
            } else if (request.getRequestURI().contains("/buyer") && role.contains("ROLE_SELLER")) {
                message = "You are currently logged in as a Seller. To access Buyer features, please log out and sign in with a Buyer account.";
            }
            
            request.getSession().setAttribute("accessDeniedMessage", message);
        }
        
        response.sendRedirect(request.getContextPath() + "/access-denied");
    }
}
