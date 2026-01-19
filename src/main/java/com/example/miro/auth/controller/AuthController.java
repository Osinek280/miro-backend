package com.example.miro.auth.controller;

import com.example.miro.auth.dto.AuthTokens;
import com.example.miro.auth.dto.AuthenticationResponse;
import com.example.miro.auth.dto.LoginRequest;
import com.example.miro.auth.dto.RegisterRequest;
import com.example.miro.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest req) {
    AuthTokens tokens = authService.register(req);

    ResponseCookie jwtCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(24 * 60 * 60)
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(
            AuthenticationResponse.builder()
                .accessToken(tokens.accessToken())
                .build()
        );
  }

  @PostMapping("/login")
  public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest req) {
    AuthTokens tokens = authService.login(req);

    ResponseCookie jwtCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(24 * 60 * 60)
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
        .body(
            AuthenticationResponse.builder()
                .accessToken(tokens.accessToken())
                .build()
        );
  }

  @PostMapping("/refresh")
  public ResponseEntity<Void> refreshToken(HttpServletRequest request,
                                        HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      System.out.println("Cookies in request:");
      for (Cookie c : cookies) {
        System.out.println("Cookie: " + c.getName() + " = " + c.getValue());
      }
    } else {
      System.out.println("No cookies in request");
    }

    String refreshToken = Arrays.stream(Optional.ofNullable(cookies).orElse(new Cookie[0]))
        .filter(c -> c.getName().equals("refresh_token"))
        .findFirst()
        .map(Cookie::getValue)
        .orElseThrow(() -> {
          System.out.println("Refresh token missing!");
          return new RuntimeException("Refresh token missing");
        });

    System.out.println("Refresh token found: " + refreshToken);

    String userAgent = request.getHeader("User-Agent");
    String ipAddress = request.getRemoteAddr();
    System.out.println("User-Agent: " + userAgent + ", IP: " + ipAddress);

    AuthTokens authResp = authService.refresh(refreshToken);

    ResponseCookie cookie = ResponseCookie.from("refresh_token", authResp.refreshToken())
        .httpOnly(true)
        .secure(true)
        .path("/api/v1/auth")
        .maxAge(14 * 24 * 60 * 60)
        .sameSite("Strict")
        .build();

    System.out.println("Refresh token cookie set in response");

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .build();
  }
}
