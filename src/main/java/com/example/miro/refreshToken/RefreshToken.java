package com.example.miro.refreshToken;

import com.example.miro.user.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
  @Id
  @GeneratedValue
  public UUID id;

  @ManyToOne(fetch=FetchType.LAZY)
  private AppUser user;

  @Column(nullable = false, unique = true)
  public String token;

  private Instant expiresAt;
  private Instant revokedAt;

  @ManyToOne
  private RefreshToken replacedBy;
}
