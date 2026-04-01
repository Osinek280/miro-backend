package com.example.miro.board.dto.drawing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.UUID;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OperationDto.AddOp.class,       name = "add"),
    @JsonSubTypes.Type(value = OperationDto.RemoveOp.class,    name = "remove"),
    @JsonSubTypes.Type(value = OperationDto.TranslateOp.class, name = "translate"),
    @JsonSubTypes.Type(value = OperationDto.BatchOp.class,     name = "batch"),
})
public sealed interface OperationDto permits
    OperationDto.AddOp,
    OperationDto.RemoveOp,
    OperationDto.TranslateOp,
    OperationDto.BatchOp
{
  String opId();
  long timestamp();
  String userId();

  record DrawObjectWireDto(
      UUID id,
      String type,
      String pointsEncoded,   // base64 Int32 delta pairs
      String color,
      int size,
      boolean tombstone,
      long positionTimestamp
  ) {}

  record AddOp(
      String opId,
      long timestamp,
      String userId,
      List<DrawObjectWireDto> objects
  ) implements OperationDto {}

  record RemoveOp(
      String opId,
      long timestamp,
      String userId,
      List<UUID> ids
  ) implements OperationDto {}

  record TranslateOp(
      String opId,
      long timestamp,
      String userId,
      List<UUID> ids,
      double dx,
      double dy
  ) implements OperationDto {}

  record BatchOp(
      String opId,
      long timestamp,
      String userId,
      List<OperationDto> operations
  ) implements OperationDto {}
}