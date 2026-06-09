package com.example.miro.auth.dto;

import com.example.miro.user.entity.AuthProvider;

import java.util.UUID;

public record UserDto(UUID id, String name, String email, String avatarUrl,
                      AuthProvider provider) {
}
