package com.example.miro.board.entities;

import com.example.miro.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "board_membership",
    uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"})
)
@Entity
public class BoardMember {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant joinedAt;

  private Instant lastOpenedAt;

  @Column(nullable = false)
  private double cameraX = 0.0;

  @Column(nullable = false)
  private double cameraY = 0.0;

  @Column(nullable = false)
  private double zoom = 1.0;
}
