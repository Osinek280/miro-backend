package com.example.miro.refreshToken;

import com.example.miro.user.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
  private final RefreshTokenRepository refreshTokenRepository;

  public String createRefreshToken(AppUser user) {
    String refreshToken = generateRandomToken();

    RefreshToken token = RefreshToken.builder()
        .user(user)
        .token(refreshToken)
        .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(token);
    return refreshToken;
  }

  public RefreshToken verifyToken(String refreshToken) {
    return refreshTokenRepository.findValidByToken(refreshToken)
        .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));
  }

  public RefreshToken rotateToken(RefreshToken oldToken, String userAgent, String ipAddress) {
    oldToken.setRevokedAt(Instant.now());
    refreshTokenRepository.save(oldToken);
    return oldToken;
  }

  private String generateRandomToken() {
    byte[] bytes = new byte[64];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
