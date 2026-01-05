package com.example.miro.auth.controller;

import com.example.miro.auth.dto.AuthenticationResponse;
import com.example.miro.auth.dto.LoginRequest;
import com.example.miro.auth.dto.RegisterRequest;
import com.example.miro.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest req) {
    AuthenticationResponse authenticationResponse = authService.register(req);
    return ResponseEntity.ok(authenticationResponse);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest req) {
    AuthenticationResponse authenticationResponse = authService.login(req);
    return ResponseEntity.ok(authenticationResponse);
  }
}
