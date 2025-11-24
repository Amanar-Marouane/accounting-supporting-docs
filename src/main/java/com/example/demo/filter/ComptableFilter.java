package com.example.demo.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.constants.Routes;
import com.example.demo.util.AppLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ComptableFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean shouldFilter = Routes.comptable_routes.stream().anyMatch(path::startsWith);

        if (shouldFilter) {
            try {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                Authentication auth = securityContext.getAuthentication();

                if (auth == null) {
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Non authentifié",
                            "Aucune authentification trouvée",
                            "UNAUTHORIZED",
                            path);
                    return;
                }

                Object principal = auth.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    boolean hasComptableRole = userDetails.getAuthorities().stream()
                            .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_COMPTABLE"));

                    if (!hasComptableRole) {
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                "Accès refusé",
                                "Accès réservé aux comptables uniquement",
                                "FORBIDDEN",
                                path);
                        return;
                    }
                } else {
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Authentification invalide",
                            "Détails utilisateur invalides",
                            "INVALID_AUTH",
                            path);
                    return;
                }
            } catch (Exception e) {
                AppLogger.error(String.format("ComptableFilter error: %s", e.getMessage()));
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "Erreur interne",
                        "Une erreur s'est produite lors de la vérification des permissions",
                        "INTERNAL_ERROR",
                        path);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String error,
            String message, String code, String path) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        errorResponse.put("status", status);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("path", path);

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
