package com.example.Miro.auth;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService service;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request
  ) {
    return ResponseEntity.ok(service.register(request));
  }

  @PostMapping("/login")
  @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT token.")
  public ResponseEntity<AuthenticationResponse> login(
      @RequestBody LoginRequest request
  ) {
    return ResponseEntity.ok(service.login(request));
  }
//  @GetMapping("/me")
//  @Operation(summary = "Get user info", description = "Returns basic user information (email, firstname).")
//  public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserDetails user) {
//    var userInfo = service.getUserInfo(user.getUsername());
//    return ResponseEntity.ok(userInfo);
//  }
//

}
