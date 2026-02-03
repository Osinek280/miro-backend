package com.example.miro.board;

import java.time.Instant;
import java.util.UUID;

public record BoardViewDto(
    UUID id,
    String name,
    Role role,
    Instant lastOpenedAt,
    Double cameraX,
    Double cameraY,
    Double zoom
) {
}
