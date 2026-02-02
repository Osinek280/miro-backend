package com.example.miro.board;

import com.example.miro.user.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  @ManyToOne(optional = false)
  private Board board;

  @ManyToOne(optional = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  private Role role;
}
