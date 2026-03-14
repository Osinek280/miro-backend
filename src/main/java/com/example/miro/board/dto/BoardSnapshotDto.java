package com.example.miro.board.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardSnapshotDto(
    UUID boardId,
    Instant serverTimestamp,
    List<DrawObjectDto> objects
) {
}
