package com.example.Miro.utility;

import com.example.Miro.entities.AppUser;
import com.example.Miro.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class DataSeeder {
  @Autowired
  private UserRepository userRepository;

  @Autowired
  private BCryptPasswordEncoder passwordEncoder;
  @PostConstruct
  public void seedData() {
    createUsers();
  }


  private void createUsers() {
    AppUser user = AppUser.builder()
        .email("admin@admin.com")
        .firstname("Adam")
        .lastname("Admin")
        .password(passwordEncoder.encode("admin"))
        .build();

    userRepository.save(user);
  }
}
