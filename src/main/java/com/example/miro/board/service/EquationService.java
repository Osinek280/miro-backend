package com.example.miro.board.service;

import com.example.miro.board.dto.equation.EquationMessage;
import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.equation.Equation;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.board.repository.EquationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EquationService {
  private final EquationRepository equationRepository;
  private final BoardRepository boardRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void saveEquation(UUID boardId, byte[] payload) throws Exception {
    EquationMessage data = objectMapper.readValue(payload, EquationMessage.class);

    if ("remove".equals(data.getAction())) {
      remove(UUID.fromString(data.getId()), boardId);
    } else {
      upsert(UUID.fromString(data.getId()), boardId, data.getLatex());
    }
  }

  private void upsert(UUID id, UUID boardId, String latex) {
    Board board = boardRepository.getReferenceById(boardId);
    Equation eq = equationRepository.findById(id)
        .orElse(Equation.builder().id(id).board(board).build());
    eq.setExpr(latex);
    equationRepository.save(eq);
  }

  private void remove(UUID id, UUID boardId) {
    Board board = boardRepository.getReferenceById(boardId);
    equationRepository.deleteByIdAndBoard(id, board);
  }
}
