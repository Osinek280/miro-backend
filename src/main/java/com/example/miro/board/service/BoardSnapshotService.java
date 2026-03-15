package com.example.miro.board.service;

import com.example.miro.board.dto.BoardSnapshotDto;
import com.example.miro.board.dto.CameraDto;
import com.example.miro.board.dto.DrawObjectDto;
import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.entities.DrawObject;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.board.repository.DrawObjectRepository;
import com.example.miro.user.AppUser;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class BoardSnapshotService {
  private final DrawObjectRepository drawObjectRepository;
  private final BoardMemberRepository memberRepository;

  public BoardSnapshotDto getSnapshot(UUID boardId, UUID userId) {
    BoardMember member = getMember(boardId, userId);

    List<DrawObjectDto> objects = drawObjectRepository
        .findLiveByBoardId(boardId)
        .stream()
        .map(this::toDto)
        .toList();

    CameraDto camera = new CameraDto(member.getCameraX(), member.getCameraY(), member.getZoom());

    return new BoardSnapshotDto(boardId, Instant.now(), objects, camera);
  }

  private BoardMember getMember(UUID boardId, UUID userId) {
    return memberRepository.findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of board: " + boardId));
  }

  private DrawObjectDto toDto(DrawObject e) {
    return new DrawObjectDto(
        e.getId(),
        e.getType().name().toLowerCase(),
        e.getPoints(),
        e.getColor(),
        e.getSize(),
        e.isTombstone(),
        e.getPositionTimestamp()
    );
  }
}
