package com.example.miro.board.service.snapshot;

import com.example.miro.board.dto.drawing.DrawObjectDto;
import com.example.miro.board.dto.drawing.OperationDto;
import com.example.miro.board.entities.DrawObject;
import com.example.miro.board.entities.DrawObjectData;
import com.example.miro.board.entities.DrawObjectType;
import com.example.miro.board.entities.ImageData;
import com.example.miro.board.entities.PathData;
import com.example.miro.board.utils.WireCodec;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class DrawObjectMapper {

  public DrawObjectData toData(OperationDto.DrawObjectWireDto wire) {
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

  public DrawObjectType parseType(String rawType) {
    if (rawType == null || rawType.isBlank()) {
      throw new IllegalArgumentException("Object type cannot be null or blank");
    }
    return DrawObjectType.valueOf(rawType.toUpperCase(Locale.ROOT));
  }

  public DrawObjectDto toDto(DrawObject object) {
    DrawObjectData data = object.getData();
    if (data instanceof PathData path) {
      return new DrawObjectDto.Path(
          object.getId(),
          object.getType(),
          path.points(),
          path.color(),
          path.size(),
          object.getPositionTimestamp()
      );
    }
    if (data instanceof ImageData image) {
      return new DrawObjectDto.Image(
          object.getId(),
          object.getType(),
          image.x(),
          image.y(),
          image.width(),
          image.height(),
          image.rotation(),
          image.src(),
          object.getPositionTimestamp()
      );
    }
    throw new IllegalStateException("Unsupported draw object data type: " + data.getClass());
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
}
