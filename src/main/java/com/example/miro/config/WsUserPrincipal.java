package com.example.miro.config;

import java.security.Principal;
import java.util.UUID;

public class WsUserPrincipal implements Principal {
  private final UUID userId;
  private final String email;

  public WsUserPrincipal(UUID userId, String email) {
    this.userId = userId;
    this.email = email;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String getName() {
    return email;
  }
}
