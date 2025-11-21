package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.constants.Routes;
import com.example.demo.contract.JwtContract;
import com.example.demo.filter.ComptableFilter;
import com.example.demo.filter.JwtFilter;
import com.example.demo.filter.LoginFilter;
import com.example.demo.filter.LogoutAuthFilter;
import com.example.demo.filter.SocieteFilter;
import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.service.JwtBlacklistService;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class SecurityConfig {

    JwtFilter jwtFilter;
    ComptableFilter comptableFilter;
    SocieteFilter societeFilter;
    JwtContract jwtService;
    CustomUserDetailsService userDetailsService;
    JwtBlacklistService jwtBlacklistService;

    @Bean
    public SecurityFilterChain basicAuthSecurityFilterChain(HttpSecurity http,
            AuthenticationConfiguration authConfig) throws Exception {

        // Create LoginFilter
        LoginFilter loginFilter = new LoginFilter(authenticationManager(authConfig), jwtService);

        // Create LogoutFilter
        LogoutAuthFilter logoutFilter = new LogoutAuthFilter(jwtBlacklistService);

        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(request -> {
                    Routes.open_routes.forEach(pr -> request.requestMatchers(pr).permitAll());
                    request.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class) // Login filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // JWT filter
                .addFilterBefore(logoutFilter, UsernamePasswordAuthenticationFilter.class) // Logout filter
                .addFilterAfter(comptableFilter, JwtFilter.class)
                .addFilterAfter(societeFilter, ComptableFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
