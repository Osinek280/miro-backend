package com.example.miro.board.entities.equation;

import jakarta.persistence.Embeddable;
import lombok.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class Color {
  private int r;
  private int g;
  private int b;
  private int a;
}
