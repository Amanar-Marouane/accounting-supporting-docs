package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.contract.LoginContract;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    LoginContract loginUtility;

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return loginUtility.Login(request.email, request.password);
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader(value = "Authorization") String token) {
        token = token.substring(7);
        loginUtility.Logout(token);
        return "You Logged out with success";
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }
}
