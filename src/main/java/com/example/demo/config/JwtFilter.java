package com.example.demo.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.demo.constants.PermitedRoutes;
import com.example.demo.contract.JwtContract;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.service.JwtBlacklistService;
import com.example.demo.util.AppLogger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    JwtContract jwtUtility;
    CustomUserDetailsService userUtility;
    HandlerExceptionResolver handlerExceptionResolver;
    JwtBlacklistService jwtBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String path = request.getRequestURI();

            boolean isPermitted = PermitedRoutes.routes.stream().anyMatch(path::startsWith);
            if (isPermitted) {
                filterChain.doFilter(request, response);
                return;
            }

            processToken(request);

        } catch (Exception e) {
            AppLogger.error(String.format("Failed to process JWT Token: %s", e.getMessage()));
            // Pass exceptions to response
            handlerExceptionResolver.resolveException(request, response, null, e);
        }

        AppLogger.debug("Processing complete. Return back control to framework");

        // Pass the control back to framework
        filterChain.doFilter(request, response);
    }

    private void processToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        AppLogger.info(String.format("Authorization Header: %s", authHeader));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            AppLogger.info("No Bearer Header, skip processing");
            return;
        }

        // Extract Bearer Token
        final String jwtToken = authHeader.substring(7);

        if (jwtBlacklistService.isBlacklisted(jwtToken)) {
            throw new InvalidTokenException("Token has been invalidated");
        }

        if (jwtUtility.isTokenExpired(jwtToken)) {
            AppLogger.info("Token validity expired");
            return;
        }

        String email = jwtUtility.getEmail(jwtToken);

        if (email == null) {
            AppLogger.info("No email found in JWT Token");
            return;
        }

        AppLogger.info("email found in JWT: " + email);

        // Get existing authentication instance
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            AppLogger.info("Already loggedin: " + email);
            return;
        }

        // Authenticate and create authentication instance
        AppLogger.info(String.format("Create authentication instance for %s", email));
        UserDetails user = userUtility.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null,
                user.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        // Store authentication token for application to use
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}