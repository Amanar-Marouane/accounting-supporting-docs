package com.example.demo.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.demo.constants.Routes;
import com.example.demo.contract.JwtContract;
import com.example.demo.exception.InvalidTokenException;
import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.service.JwtBlacklistService;
import com.example.demo.util.AppLogger;

import io.jsonwebtoken.JwtException;
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

            boolean isPermitted = Routes.open_routes.stream().anyMatch(path::startsWith);
            if (isPermitted) {
                filterChain.doFilter(request, response);
                return;
            }

            processToken(request);

        } catch (Exception e) {
            AppLogger.error(String.format("JWT Filter error: %s - %s", e.getClass().getSimpleName(), e.getMessage()));
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void processToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            AppLogger.debug("No Bearer token found in request");
            return;
        }

        final String jwtToken = authHeader.substring(7);

        try {
            if (jwtBlacklistService.isBlacklisted(jwtToken)) {
                AppLogger.warn("Attempted to use blacklisted token");
                throw new InvalidTokenException("Token has been invalidated");
            }

            if (jwtUtility.isTokenExpired(jwtToken)) {
                AppLogger.warn("Attempted to use expired token");
                return;
            }

            String email = jwtUtility.getEmail(jwtToken);

            if (email == null) {
                AppLogger.warn("Token does not contain valid email");
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null) {
                return;
            }

            UserDetails user = userUtility.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            AppLogger.debug(String.format("Successfully authenticated user: %s", email));

        } catch (JwtException e) {
            AppLogger.error(String.format("JWT validation error: %s", e.getMessage()));
            throw e;
        } catch (Exception e) {
            AppLogger.error(String.format("Unexpected error in token processing: %s", e.getMessage()));
            throw new InvalidTokenException("Error processing token", e);
        }
    }
}