package com.example.miro.board.service;

import com.example.miro.board.dto.board.BoardSnapshotDto;
import com.example.miro.board.dto.camera.CameraDto;
import com.example.miro.board.dto.drawing.DrawObjectDto;
import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.entities.DrawObject;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.board.repository.DrawObjectRepository;
import com.example.miro.board.utils.WireCodec;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.time.Instant;
import java.util.*;

@Service
@AllArgsConstructor
public class BoardSnapshotService {
  private final DrawObjectRepository drawObjectRepository;
  private final BoardMemberRepository memberRepository;
  private final ObjectMapper objectMapper;
  private final BoardRepository boardRepository;

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

  @Transactional
  public void applyOperation(UUID boardId, byte[] payload) {
    OperationDto op;
    try {
      op = objectMapper.readValue(payload, OperationDto.class);
      System.out.println(op);
    } catch (Exception e) {
      System.out.println("Failed to deserialize operation for board " + boardId + ": " + e.getMessage());
      return;
    }

    Board board = boardRepository.findWithObjectsById(boardId)
        .orElseThrow(() -> new NoSuchElementException("Board not found: " + boardId));

    applyToBoard(board, op);
    boardRepository.save(board);
  }

  private void applyToBoard(Board board, OperationDto op) {
    if (op instanceof OperationDto.AddOp add) {
      applyAdd(board, add);
    } else if (op instanceof OperationDto.RemoveOp remove) {
      applyRemove(board, remove);
    } else if (op instanceof OperationDto.TranslateOp trans) {
      applyTranslate(board, trans);
    } else if (op instanceof OperationDto.BatchOp batch) {
      flattenBatch(batch).forEach(child -> applyToBoard(board, child));
    } else {
      throw new IllegalArgumentException("Unknown operation type: " + op.getClass());
    }
  }

  private List<OperationDto> flattenBatch(OperationDto.BatchOp batch) {
    List<OperationDto> out = new ArrayList<>();
    for (OperationDto op : batch.operations()) {
      if (op instanceof OperationDto.BatchOp nested) {
        out.addAll(flattenBatch(nested));
      } else {
        out.add(op);
      }
    }
    out.sort(Comparator.comparingLong(OperationDto::timestamp)
        .thenComparing(OperationDto::opId));
    return out;
  }


  private void applyAdd(Board board, OperationDto.AddOp op) {
    Map<UUID, DrawObject> existing = new HashMap<>();
    for (DrawObject o : board.getObjects()) {
      existing.put(o.getId(), o);
    }

    for (var wire : op.objects()) {
      UUID id = wire.id();
      List<com.example.miro.board.entities.Point> points =
          WireCodec.decodePoints(wire.pointsEncoded());

      if (existing.containsKey(id)) {
        DrawObject obj = existing.get(id);
        obj.setPoints(points);
        obj.setColor(wire.color());
        obj.setSize(wire.size());
        obj.setPositionTimestamp(Instant.ofEpochMilli(wire.positionTimestamp()));
      } else {
        DrawObject obj = DrawObject.builder()
            .id(id)
            .board(board)
            .type(com.example.miro.board.entities.DrawObjectType
                .valueOf(wire.type().toUpperCase()))
            .points(points)
            .color(wire.color())
            .size(wire.size())
            .positionTimestamp(Instant.ofEpochMilli(wire.positionTimestamp()))
            .build();
        board.getObjects().add(obj);
        existing.put(id, obj);
      }
    }
  }

  // ─── REMOVE (tombstone) ───────────────────────────────────────────────────

  private void applyRemove(Board board, OperationDto.RemoveOp op) {
    Set<UUID> ids = new HashSet<>(op.ids());
    board.getObjects().removeIf(obj -> ids.contains(obj.getId()));
  }

  // ─── TRANSLATE (LWW) ─────────────────────────────────────────────────────

  private void applyTranslate(Board board, OperationDto.TranslateOp op) {
    Set<UUID> ids = new HashSet<>(op.ids());
    Instant opTs = Instant.ofEpochMilli(op.timestamp());

    for (DrawObject obj : board.getObjects()) {
      if (!ids.contains(obj.getId())) continue;

      Instant objTs = obj.getPositionTimestamp() != null
          ? obj.getPositionTimestamp()
          : Instant.EPOCH;

      if (opTs.isBefore(objTs)) continue;

      List<com.example.miro.board.entities.Point> moved = obj.getPoints().stream()
          .map(p -> new com.example.miro.board.entities.Point(
              p.x() + op.dx(),
              p.y() + op.dy()
          ))
          .toList();

      obj.setPoints(moved);
      obj.setPositionTimestamp(opTs);
    }
  }

  private BoardMember getMember(UUID boardId, UUID userId) {
    return memberRepository.findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of board: " + boardId));
  }

  private DrawObjectDto toDto(DrawObject e) {
    return new DrawObjectDto(
        e.getId(),
        e.getType(),
        e.getPoints(),
        e.getColor(),
        e.getSize(),
        false,
        e.getPositionTimestamp()
    );
  }
}
