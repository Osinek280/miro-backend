package com.example.miro.user.service;

import com.example.miro.auth.dto.UserDto;
import com.example.miro.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  public UserDto me(AppUser user) {
    return new UserDto(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getProvider()
    );
  }
}
