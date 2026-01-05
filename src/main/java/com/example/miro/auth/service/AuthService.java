package com.example.miro.auth.service;

import com.example.miro.auth.dto.AuthenticationResponse;
import com.example.miro.auth.dto.LoginRequest;
import com.example.miro.auth.dto.RegisterRequest;
import com.example.miro.user.AppUser;
import com.example.miro.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthenticationResponse register(RegisterRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalStateException("Email already in use");
    }

    AppUser user = new AppUser();
    user.setFirstname(request.getFirstname());
    user.setLastname(request.getLastname());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    userRepository.save(user);

    String accessToken = jwtService.generateToken(user.getEmail());

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .build();
  }

  public AuthenticationResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );

    AppUser user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    String accessToken = jwtService.generateToken(user.getEmail());

    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .build();
  }
}
