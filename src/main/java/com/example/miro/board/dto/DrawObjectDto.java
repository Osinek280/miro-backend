package com.example.miro.board.dto;

import com.example.miro.board.entities.DrawObjectType;
import com.example.miro.board.entities.Point;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DrawObjectDto(
    UUID id,
    DrawObjectType type,
    List<Point> points,
    String color,
    int size,
    boolean tombstone,
    Instant positionTimestamp
) {}
