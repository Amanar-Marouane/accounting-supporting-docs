package com.example.demo.filter;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.example.demo.constants.Routes;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocieteFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean shouldFilter = Routes.societe_routes.stream().anyMatch(path::startsWith);

        if (shouldFilter) {
            try {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                Authentication auth = securityContext.getAuthentication();

                if (auth == null) {
                    throw new AuthenticationException("Aucune authentification trouvée") {
                    };
                }

                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    boolean hasSocieteRole = userDetails.getAuthorities().stream()
                            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_SOCIETE"));

                    if (!hasSocieteRole) {
                        throw new AccessDeniedException("Accès réservé aux sociétés uniquement");
                    }
                } else {
                    throw new AuthenticationException("Détails utilisateur invalides") {
                    };
                }
            } catch (Exception e) {
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
