package com.example.miro.board.dto.drawing;

import com.example.miro.board.entities.DrawObjectType;
import com.example.miro.board.entities.Point;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DrawObjectDto.Path.class, name = "PATH"),
    @JsonSubTypes.Type(value = DrawObjectDto.Image.class, name = "IMAGE"),
})
public sealed interface DrawObjectDto permits DrawObjectDto.Path, DrawObjectDto.Image {

  record Path(
      UUID id,
      DrawObjectType type,
      List<Point> points,
      String color,
      int size,
      Instant positionTimestamp
  ) implements DrawObjectDto {
  }

  record Image(
      UUID id,
      DrawObjectType type,
      double x,
      double y,
      double width,
      double height,
      double rotation,
      String src,
      Instant positionTimestamp
  ) implements DrawObjectDto {
  }
}