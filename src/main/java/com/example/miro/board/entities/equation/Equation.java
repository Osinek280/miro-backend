package com.example.miro.board.entities.equation;

import com.example.miro.board.entities.Board;
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
@Entity
@Table(
    name = "equations",
    indexes = {
        @Index(name = "idx_equations_board_id", columnList = "board_id")
    }
)
public class Equation {
  @Id
  private UUID id;
  private String expr;
  @Embedded
  private Color color;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;
}
