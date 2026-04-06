package com.example.miro.board.service.snapshot;

import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.DrawObject;
import com.example.miro.board.entities.DrawObjectData;
import com.example.miro.board.entities.DrawObjectType;
import com.example.miro.board.entities.ImageData;
import com.example.miro.board.entities.PathData;
import com.example.miro.board.entities.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardOperationApplier {
  private final DrawObjectMapper drawObjectMapper;

  public void apply(Board board, OperationDto operation) {
    if (operation instanceof OperationDto.AddOp addOp) {
      applyAdd(board, addOp);
    } else if (operation instanceof OperationDto.RemoveOp removeOp) {
      applyRemove(board, removeOp);
    } else if (operation instanceof OperationDto.TranslateOp translateOp) {
      applyTranslate(board, translateOp);
    } else if (operation instanceof OperationDto.BatchOp batchOp) {
      flattenBatch(batchOp).forEach(child -> apply(board, child));
    } else {
      throw new IllegalArgumentException("Unknown operation type: " + operation.getClass());
    }
  }

  private void applyAdd(Board board, OperationDto.AddOp operation) {
    Map<UUID, DrawObject> existing = board.getObjects().stream()
        .collect(Collectors.toMap(DrawObject::getId, object -> object));

    for (var wire : operation.objects()) {
      UUID id = wire.id();
      DrawObjectType type = drawObjectMapper.parseType(wire.type());
      DrawObjectData data = drawObjectMapper.toData(wire);
      Instant incomingTimestamp = toInstant(wire.positionTimestamp());

      if (existing.containsKey(id)) {
        DrawObject object = existing.get(id);
        Instant currentTimestamp = object.getPositionTimestamp() != null
            ? object.getPositionTimestamp()
            : Instant.EPOCH;

        if (incomingTimestamp.isBefore(currentTimestamp)) {
          continue;
        }

        object.setType(type);
        object.setData(data);
        object.setPositionTimestamp(incomingTimestamp);
      } else {
        DrawObject object = DrawObject.builder()
            .id(id)
            .board(board)
            .type(type)
            .data(data)
            .positionTimestamp(incomingTimestamp)
            .build();
        board.getObjects().add(object);
        existing.put(id, object);
      }
    }
  }

  private void applyRemove(Board board, OperationDto.RemoveOp operation) {
    Set<UUID> ids = new HashSet<>(operation.ids());
    board.getObjects().removeIf(object -> ids.contains(object.getId()));
  }

  private void applyTranslate(Board board, OperationDto.TranslateOp operation) {
    Set<UUID> ids = new HashSet<>(operation.ids());
    Instant operationTimestamp = Instant.ofEpochMilli(operation.timestamp());

    for (DrawObject object : board.getObjects()) {
      if (!ids.contains(object.getId())) {
        continue;
      }

      Instant objectTimestamp = object.getPositionTimestamp() != null
          ? object.getPositionTimestamp()
          : Instant.EPOCH;

      if (operationTimestamp.isBefore(objectTimestamp)) {
        continue;
      }

      DrawObjectData moved;
      DrawObjectData currentData = object.getData();
      if (currentData instanceof PathData path) {
        moved = new PathData(
            path.points().stream()
                .map(point -> new Point(point.x() + operation.dx(), point.y() + operation.dy()))
                .toList(),
            path.color(),
            path.size()
        );
      } else if (currentData instanceof ImageData image) {
        moved = new ImageData(
            image.x() + operation.dx(),
            image.y() + operation.dy(),
            image.width(),
            image.height(),
            image.rotation(),
            image.src()
        );
      } else {
        throw new IllegalStateException("Unsupported draw object data type: " + currentData.getClass());
      }

      object.setData(moved);
      object.setPositionTimestamp(operationTimestamp);
    }
  }

  private List<OperationDto> flattenBatch(OperationDto.BatchOp batch) {
    List<OperationDto> flattened = new ArrayList<>();
    for (OperationDto operation : batch.operations()) {
      if (operation instanceof OperationDto.BatchOp nestedBatch) {
        flattened.addAll(flattenBatch(nestedBatch));
      } else {
        flattened.add(operation);
      }
    }
    flattened.sort(Comparator.comparingLong(OperationDto::timestamp)
        .thenComparing(OperationDto::opId));
    return flattened;
  }

  private Instant toInstant(long epochMillis) {
    return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : Instant.EPOCH;
  }
}
