package com.example.miro.board.service;

import com.example.miro.board.dto.board.BoardViewDto;
import com.example.miro.board.dto.camera.CameraDto;
import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.entities.Role;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.user.AppUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BoardService {
  private final BoardRepository boardRepo;
  private final BoardMemberRepository memberRepo;

  public List<BoardViewDto> getMyBoards(AppUser user) {
    return memberRepo
        .findByUserOrderByLastOpenedAtDesc(user)
        .stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public void deleteBoard(UUID boardId, AppUser user) {
    Board board = boardRepo.findById(boardId)
        .orElseThrow(() -> new IllegalArgumentException("Board not found"));

    if (!board.getOwner().getId().equals(user.getId())) {
      throw new SecurityException("Access denied");
    }

    boardRepo.delete(board);
  }

  public void renameBoard(UUID boardId, String newName, AppUser user) {
    Board board = boardRepo.findById(boardId)
        .orElseThrow(() -> new RuntimeException("Board not found"));

    if (!board.getOwner().getId().equals(user.getId())) {
      throw new RuntimeException("No permission");
    }

    board.setName(newName);
    boardRepo.save(board);
  }

  public UUID createBoard(String name, AppUser owner) {

    Board board = boardRepo.save(
        Board.builder()
            .name(name)
            .owner(owner)
            .build()
    );

    memberRepo.save(
        BoardMember.builder()
            .board(board)
            .user(owner)
            .role(Role.OWNER)
            .cameraX(0.0)
            .cameraY(0.0)
            .zoom(1.0)
            .joinedAt(Instant.now())
            .lastOpenedAt(Instant.now())
            .build()
    );

    return board.getId();
  }

  public void saveCamera(UUID boardId, UUID userId, CameraDto camera) {

    BoardMember member = memberRepo
        .findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new RuntimeException("No access"));

    member.setLastOpenedAt(Instant.now());
    member.setCameraX(camera.offsetX());
    member.setCameraY(camera.offsetY());
    member.setZoom(camera.zoom());

    memberRepo.save(member);
  }


  private BoardViewDto toDto(BoardMember m) {
    return new BoardViewDto(
        m.getBoard().getId(),
        m.getBoard().getName(),
        m.getRole(),
        m.getLastOpenedAt(),
        m.getCameraX(),
        m.getCameraY(),
        m.getZoom()
    );
  }
}
