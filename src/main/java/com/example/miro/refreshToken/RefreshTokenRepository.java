package com.example.miro.refreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  @Query("""
    SELECT rt FROM RefreshToken rt
    WHERE rt.token = :token
      AND rt.revokedAt IS NULL
      AND rt.expiresAt > CURRENT_TIMESTAMP
  """)
  Optional<RefreshToken> findValidByToken(@Param("token") String token);

  // 2️⃣ Wszystkie aktywne tokeny użytkownika (logout-all)
  @Query("""
    SELECT rt FROM RefreshToken rt
    WHERE rt.user.id = :userId
      AND rt.revokedAt IS NULL
      AND rt.expiresAt > CURRENT_TIMESTAMP
  """)
  List<RefreshToken> findAllValidByUserId(@Param("userId") Long userId);

  // 3️⃣ Revoke wszystkich tokenów użytkownika
  @Modifying
  @Query("""
    UPDATE RefreshToken rt
    SET rt.revokedAt = CURRENT_TIMESTAMP
    WHERE rt.user.id = :userId
      AND rt.revokedAt IS NULL
  """)
  int revokeAllByUserId(@Param("userId") Long userId);

  // 4️⃣ Detekcja reuse attack (token już cofnięty)
  @Query("""
    SELECT rt FROM RefreshToken rt
    WHERE rt.token = :token
      AND rt.revokedAt IS NOT NULL
  """)
  Optional<RefreshToken> findRevokedByToken(@Param("token") String token);

  // 5️⃣ Cleanup starych tokenów (cron)
  @Modifying
  @Query("""
    DELETE FROM RefreshToken rt
    WHERE rt.expiresAt < :cutoff
  """)
  int deleteExpired(@Param("cutoff") Instant cutoff);
}