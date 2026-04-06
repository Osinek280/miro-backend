package com.example.miro.board.service;

import com.example.miro.board.dto.board.BoardSnapshotDto;
import com.example.miro.board.dto.camera.CameraDto;
import com.example.miro.board.dto.drawing.DrawObjectDto;
import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.*;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.board.repository.BoardRepository;
import com.example.miro.board.repository.DrawObjectRepository;
import com.example.miro.board.utils.WireCodec;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BoardSnapshotService {
  private static final Logger log = LoggerFactory.getLogger(BoardSnapshotService.class);
  private final DrawObjectRepository drawObjectRepository;
  private final BoardMemberRepository memberRepository;
  private final ObjectMapper objectMapper;
  private final BoardRepository boardRepository;

  public BoardSnapshotDto getSnapshot(UUID boardId, UUID userId) {
    BoardMember member = getMember(boardId, userId);

    List<DrawObjectDto> objects = drawObjectRepository
        .findAllByBoardId(boardId)
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
    } catch (Exception e) {
      log.warn("Failed to deserialize operation for board {}: {}", boardId, e.getMessage());
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
    Map<UUID, DrawObject> existing = board.getObjects().stream()
        .collect(Collectors.toMap(DrawObject::getId, o -> o));

    for (var wire : op.objects()) {
      UUID id = wire.id();
      DrawObjectType type = parseType(wire.type());
      DrawObjectData data = toData(wire);
      Instant incomingTs = toInstant(wire.positionTimestamp());

      if (existing.containsKey(id)) {
        DrawObject obj = existing.get(id);

        Instant currentTs = obj.getPositionTimestamp() != null
            ? obj.getPositionTimestamp()
            : Instant.EPOCH;

        if (incomingTs.isBefore(currentTs)) continue;

        obj.setType(type);
        obj.setData(data);
        obj.setPositionTimestamp(toInstant(wire.positionTimestamp()));
      } else {
        DrawObject obj = DrawObject.builder()
            .id(id)
            .board(board)
            .type(type)
            .data(data)
            .positionTimestamp(toInstant(wire.positionTimestamp()))
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

      DrawObjectData moved;
      DrawObjectData currentData = obj.getData();
      if (currentData instanceof PathData p) {
        moved = new PathData(
            p.points().stream()
                .map(pt -> new Point(pt.x() + op.dx(), pt.y() + op.dy()))
                .toList(),
            p.color(),
            p.size()
        );
      } else if (currentData instanceof ImageData img) {
        moved = new ImageData(
            img.x() + op.dx(),
            img.y() + op.dy(),
            img.width(),
            img.height(),
            img.rotation(),
            img.src()
        );
      } else {
        throw new IllegalStateException("Unsupported draw object data type: " + currentData.getClass());
      }

      obj.setData(moved);
      obj.setPositionTimestamp(opTs);
    }
  }

  private BoardMember getMember(UUID boardId, UUID userId) {
    return memberRepository.findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new AccessDeniedException("Not a member of board: " + boardId));
  }

  private DrawObjectData toData(OperationDto.DrawObjectWireDto wire) {
    return switch (parseType(wire.type())) {
      case PATH -> new PathData(
          WireCodec.decodePoints(requiredString(wire.pointsEncoded(), "pointsEncoded", wire.id())),
          requiredString(wire.color(), "color", wire.id()),
          requiredInteger(wire.size(), "size", wire.id())
      );
      case IMAGE -> new ImageData(
          requiredDouble(wire.x(), "x", wire.id()),
          requiredDouble(wire.y(), "y", wire.id()),
          requiredDouble(wire.width(), "width", wire.id()),
          requiredDouble(wire.height(), "height", wire.id()),
          requiredDouble(wire.rotation(), "rotation", wire.id()),
          requiredString(wire.src(), "src", wire.id())
      );
    };
  }

  private DrawObjectType parseType(String rawType) {
    if (rawType == null || rawType.isBlank()) {
      throw new IllegalArgumentException("Object type cannot be null or blank");
    }
    return DrawObjectType.valueOf(rawType.toUpperCase(Locale.ROOT));
  }

  private Instant toInstant(long epochMillis) {
    return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : Instant.EPOCH;
  }

  private double requiredDouble(Double value, String fieldName, UUID objectId) {
    if (value == null) {
      throw new IllegalArgumentException("Missing field '" + fieldName + "' for image object " + objectId);
    }
    return value;
  }

  private int requiredInteger(Integer value, String fieldName, UUID objectId) {
    if (value == null) {
      throw new IllegalArgumentException("Missing field '" + fieldName + "' for path object " + objectId);
    }
    return value;
  }

  private String requiredString(String value, String fieldName, UUID objectId) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Missing field '" + fieldName + "' for object " + objectId);
    }
    return value;
  }

  private DrawObjectDto toDto(DrawObject e) {
    DrawObjectData data = e.getData();
    if (data instanceof PathData p) {
      return new DrawObjectDto.Path(
          e.getId(),
          e.getType(),
          p.points(),
          p.color(),
          p.size(),
          e.getPositionTimestamp()
      );
    }
    if (data instanceof ImageData img) {
      return new DrawObjectDto.Image(
          e.getId(),
          e.getType(),
          img.x(),
          img.y(),
          img.width(),
          img.height(),
          img.rotation(),
          img.src(),
          e.getPositionTimestamp()
      );
    }
    throw new IllegalStateException("Unsupported draw object data type: " + data.getClass());
  }
}
