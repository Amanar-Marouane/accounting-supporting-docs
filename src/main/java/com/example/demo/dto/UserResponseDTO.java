package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private User.Role role;
    private Long societeId;
    private boolean active;
    private LocalDateTime createdAt;
}