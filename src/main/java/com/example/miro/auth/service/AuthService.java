package com.example.miro.auth.service;

import com.example.miro.auth.dto.AuthTokens;
import com.example.miro.auth.dto.LoginRequest;
import com.example.miro.auth.dto.RegisterRequest;
import com.example.miro.refreshToken.RefreshToken;
import com.example.miro.refreshToken.RefreshTokenService;
import com.example.miro.user.AppUser;
import com.example.miro.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;

  public AuthTokens register(RegisterRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalStateException("Email already in use");
    }

    AppUser user = new AppUser();
    user.setFirstname(request.getFirstname());
    user.setLastname(request.getLastname());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    userRepository.save(user);

    String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
    String refreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthTokens(accessToken, refreshToken);
  }

  public AuthTokens login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );

    AppUser user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
    String refreshToken = refreshTokenService.createRefreshToken(user);

    return new AuthTokens(accessToken, refreshToken);
  }

  @Transactional
  public AuthTokens refresh(String refreshToken) {
    RefreshToken oldToken = refreshTokenService.verifyToken(refreshToken);
    String newRefreshToken = refreshTokenService.rotateToken(oldToken);

    AppUser user = oldToken.getUser();

    String accessToken = jwtService.generateToken(user.getEmail(), user.getId());

    return new AuthTokens(accessToken, newRefreshToken);
  }
}
