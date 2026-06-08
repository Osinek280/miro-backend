package com.example.miro.auth.dto;

import java.util.UUID;

public record UserDto(UUID id, String name, String email, String avatarUrl,
                      com.example.miro.user.AuthProvider provider) {
}
