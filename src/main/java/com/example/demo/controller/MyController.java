package com.example.demo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/me")
    public String getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "No authenticated user";
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return "Authenticated user: " + userDetails.getUsername() + ", roles: " + userDetails.getAuthorities();
        }

        // fallback if principal is just a string
        return "Authenticated principal: " + principal.toString();
    }
}
