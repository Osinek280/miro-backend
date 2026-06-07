package com.example.miro.board.service.snapshot;

import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardOperationApplier {
  private static final double POINT_SCALE = 1000.0;
  private final DrawObjectMapper drawObjectMapper;

  public void apply(Board board, OperationDto operation) {
    if (operation instanceof OperationDto.AddOp addOp) {
      applyAdd(board, addOp);
    } else if (operation instanceof OperationDto.RemoveOp removeOp) {
      applyRemove(board, removeOp);
    } else if (operation instanceof OperationDto.TranslateOp translateOp) {
      applyTranslate(board, translateOp);
    } else if (operation instanceof OperationDto.ScaleBoundsOp scaleBoundsOp) {
      applyScaleBounds(board, scaleBoundsOp);
    } else if (operation instanceof OperationDto.RotateOp rotateOp) {
      applyRotate(board, rotateOp);
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
    Instant operationTimestamp = toInstant(operation.timestamp());

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
                .map(point -> new Point(
                    roundScalar(point.x() + operation.dx()),
                    roundScalar(point.y() + operation.dy())
                ))
                .toList(),
            path.color(),
            path.size()
        );
      } else if (currentData instanceof ImageData image) {
        moved = new ImageData(
            roundScalar(image.x() + operation.dx()),
            roundScalar(image.y() + operation.dy()),
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
    long baseTimestamp = batch.timestamp() > 0 ? batch.timestamp() : System.currentTimeMillis();
    List<StampedOperation> flattened = new ArrayList<>();
    long[] index = {0L};
    for (OperationDto operation : batch.operations()) {
      collectFlattened(operation, baseTimestamp, index, flattened);
    }
    flattened.sort(Comparator
        .comparingLong(StampedOperation::timestamp)
        .thenComparing(stamped -> safeString(stamped.operation().opId()))
        .thenComparingLong(StampedOperation::order));
    return flattened.stream()
        .map(StampedOperation::operation)
        .toList();
  }

  private void applyScaleBounds(Board board, OperationDto.ScaleBoundsOp operation) {
    if (operation.oldBounds() == null || operation.newBounds() == null) {
      return;
    }

    Set<UUID> ids = new HashSet<>(operation.ids());
    Instant operationTimestamp = toInstant(operation.timestamp());
    double oldWidth = operation.oldBounds().maxX() - operation.oldBounds().minX();
    double oldHeight = operation.oldBounds().maxY() - operation.oldBounds().minY();
    if (Math.abs(oldWidth) < 1e-9 || Math.abs(oldHeight) < 1e-9) {
      return;
    }

    double newWidth = operation.newBounds().maxX() - operation.newBounds().minX();
    double newHeight = operation.newBounds().maxY() - operation.newBounds().minY();
    double sx = newWidth / oldWidth;
    double sy = newHeight / oldHeight;

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

      DrawObjectData currentData = object.getData();
      if (currentData instanceof PathData path) {
        List<Point> scaledPoints = path.points().stream()
            .map(point -> mapPointBetweenBounds(point, operation.oldBounds(), operation.newBounds(), sx, sy))
            .toList();
        object.setData(new PathData(scaledPoints, path.color(), path.size()));
        object.setPositionTimestamp(operationTimestamp);
      } else if (currentData instanceof ImageData image) {
        Point topLeft = mapPointBetweenBounds(
            new Point(image.x(), image.y()),
            operation.oldBounds(),
            operation.newBounds(),
            sx,
            sy
        );
        ImageData scaledImage = new ImageData(
            topLeft.x(),
            topLeft.y(),
            roundScalar(image.width() * sx),
            roundScalar(image.height() * sy),
            image.rotation(),
            image.src()
        );
        object.setData(scaledImage);
        object.setPositionTimestamp(operationTimestamp);
      } else {
        throw new IllegalStateException("Unsupported draw object data type: " + currentData.getClass());
      }
    }
  }

  private void applyRotate(Board board, OperationDto.RotateOp operation) {
    if (operation.center() == null) {
      return;
    }

    Set<UUID> ids = new HashSet<>(operation.ids());
    Instant operationTimestamp = toInstant(operation.timestamp());
    Point center = new Point(operation.center().x(), operation.center().y());

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

      DrawObjectData currentData = object.getData();
      if (currentData instanceof PathData path) {
        List<Point> rotatedPoints = path.points().stream()
            .map(point -> roundPoint(rotatePointAround(point, center, operation.deltaRadians())))
            .toList();
        object.setData(new PathData(rotatedPoints, path.color(), path.size()));
        object.setPositionTimestamp(operationTimestamp);
      } else if (currentData instanceof ImageData image) {
        Point imageCenterBefore = new Point(image.x() + image.width() / 2.0, image.y() + image.height() / 2.0);
        Point imageCenterAfter = rotatePointAround(imageCenterBefore, center, operation.deltaRadians());
        ImageData rotatedImage = new ImageData(
            roundScalar(imageCenterAfter.x() - image.width() / 2.0),
            roundScalar(imageCenterAfter.y() - image.height() / 2.0),
            image.width(),
            image.height(),
            image.rotation() + operation.deltaRadians(),
            image.src()
        );
        object.setData(rotatedImage);
        object.setPositionTimestamp(operationTimestamp);
      } else {
        throw new IllegalStateException("Unsupported draw object data type: " + currentData.getClass());
      }
    }
  }

  private Instant toInstant(long epochMillis) {
    return epochMillis > 0 ? Instant.ofEpochMilli(epochMillis) : Instant.EPOCH;
  }

  private double roundScalar(double value) {
    return Math.round(value * POINT_SCALE) / POINT_SCALE;
  }

  private Point roundPoint(Point point) {
    return new Point(roundScalar(point.x()), roundScalar(point.y()));
  }

  private Point mapPointBetweenBounds(
      Point point,
      OperationDto.BoundsRect oldBounds,
      OperationDto.BoundsRect newBounds,
      double sx,
      double sy
  ) {
    double mappedX = newBounds.minX() + (point.x() - oldBounds.minX()) * sx;
    double mappedY = newBounds.minY() + (point.y() - oldBounds.minY()) * sy;
    return new Point(roundScalar(mappedX), roundScalar(mappedY));
  }

  private Point rotatePointAround(Point point, Point center, double radians) {
    double cos = Math.cos(radians);
    double sin = Math.sin(radians);
    double dx = point.x() - center.x();
    double dy = point.y() - center.y();
    return new Point(
        center.x() + cos * dx - sin * dy,
        center.y() + sin * dx + cos * dy
    );
  }

  private void collectFlattened(
      OperationDto operation,
      long baseTimestamp,
      long[] index,
      List<StampedOperation> out
  ) {
    if (operation instanceof OperationDto.BatchOp nestedBatch) {
      for (OperationDto nested : nestedBatch.operations()) {
        collectFlattened(nested, baseTimestamp, index, out);
      }
      return;
    }

    long stampedTimestamp = operation.timestamp() > 0
        ? operation.timestamp()
        : baseTimestamp + index[0];
    index[0]++;
    out.add(new StampedOperation(restampOperation(operation, stampedTimestamp), stampedTimestamp, index[0]));
  }

  private OperationDto restampOperation(OperationDto operation, long timestamp) {
    if (operation instanceof OperationDto.AddOp addOp) {
      return new OperationDto.AddOp(addOp.opId(), timestamp, addOp.userId(), addOp.objects());
    }
    if (operation instanceof OperationDto.RemoveOp removeOp) {
      return new OperationDto.RemoveOp(removeOp.opId(), timestamp, removeOp.userId(), removeOp.ids());
    }
    if (operation instanceof OperationDto.TranslateOp translateOp) {
      return new OperationDto.TranslateOp(
          translateOp.opId(),
          timestamp,
          translateOp.userId(),
          translateOp.ids(),
          translateOp.dx(),
          translateOp.dy()
      );
    }
    if (operation instanceof OperationDto.ScaleBoundsOp scaleBoundsOp) {
      return new OperationDto.ScaleBoundsOp(
          scaleBoundsOp.opId(),
          timestamp,
          scaleBoundsOp.userId(),
          scaleBoundsOp.ids(),
          scaleBoundsOp.oldBounds(),
          scaleBoundsOp.newBounds()
      );
    }
    if (operation instanceof OperationDto.RotateOp rotateOp) {
      return new OperationDto.RotateOp(
          rotateOp.opId(),
          timestamp,
          rotateOp.userId(),
          rotateOp.ids(),
          rotateOp.center(),
          rotateOp.deltaRadians()
      );
    }
    return operation;
  }

  private String safeString(String value) {
    return value == null ? "" : value;
  }

  private record StampedOperation(OperationDto operation, long timestamp, long order) {
  }
}
