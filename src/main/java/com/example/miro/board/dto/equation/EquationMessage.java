package com.example.miro.board.dto.equation;

import lombok.Data;

@Data
public class EquationMessage {
  private String id;
  private String latex;
  private String action;
  private String userId;
}
