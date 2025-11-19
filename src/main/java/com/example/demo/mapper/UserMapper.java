package com.example.demo.mapper;

import com.example.demo.dto.UserResponseDTO;
import com.example.demo.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "societe.id", target = "societeId")
    UserResponseDTO toResponseDTO(User user);
}