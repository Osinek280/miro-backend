package com.example.miro.board.service;

import com.example.miro.board.dto.board.BoardSnapshotDto;
import com.example.miro.board.dto.camera.CameraDto;
import com.example.miro.board.dto.drawing.DrawObjectDto;
import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.board.repository.DrawObjectRepository;
import com.example.miro.board.service.snapshot.BoardOperationApplier;
import com.example.miro.board.service.snapshot.DrawObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BoardSnapshotService {
  private static final Logger log = LoggerFactory.getLogger(BoardSnapshotService.class);
  private final DrawObjectRepository drawObjectRepository;
  private final BoardMemberRepository memberRepository;
  private final ObjectMapper objectMapper;
  private final BoardRepository boardRepository;
  private final DrawObjectMapper drawObjectMapper;
  private final BoardOperationApplier boardOperationApplier;

  public BoardSnapshotDto getSnapshot(UUID boardId, UUID userId) {
    BoardMember member = getMember(boardId, userId);

    List<DrawObjectDto> objects = drawObjectRepository
        .findAllByBoardId(boardId)
        .stream()
        .map(drawObjectMapper::toDto)
        .toList();

    CameraDto camera = new CameraDto(member.getCameraX(), member.getCameraY(), member.getZoom());

    return new BoardSnapshotDto(boardId, Instant.now(), objects, camera);
  }

  @Transactional
  public void applyOperation(UUID boardId, byte[] payload) {
    OperationDto op;
    try {
      op = objectMapper.readValue(payload, OperationDto.class);
    } catch (Exception e) {
      log.warn("Failed to deserialize operation for board {}: {}", boardId, e.getMessage());
      return;
    }

    Board board = boardRepository.findWithObjectsById(boardId)
        .orElseThrow(() -> new NoSuchElementException("Board not found: " + boardId));

    boardOperationApplier.apply(board, op);
    boardRepository.save(board);
  }

  private BoardMember getMember(UUID boardId, UUID userId) {
    return memberRepository.findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of board: " + boardId));
  }
}
