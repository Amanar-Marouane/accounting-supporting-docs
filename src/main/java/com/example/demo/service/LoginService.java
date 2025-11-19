package com.example.demo.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.contract.LoginContract;
import com.example.demo.entity.User;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class LoginService implements LoginContract {

    UserRepository userRepo;
    PasswordEncoder passwordEncoder;
    JwtService jwtUtility;

    @Override
    public String Login(String email, String password) {
        User user = userRepo.findByEmail(email);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return jwtUtility.generateToken(
                Map.of("role", user.getRole().toString()),
                email);
    }

    @Override
    public void Logout(String token) {
        jwtUtility.invalidate(token);
    }
}
