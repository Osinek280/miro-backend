package com.example.miro.board.dto.board;

import com.example.miro.board.dto.camera.CameraDto;
import com.example.miro.board.dto.drawing.DrawObjectDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardSnapshotDto(
    UUID boardId,
    Instant serverTimestamp,
    List<DrawObjectDto> objects,
    CameraDto camera
) {
}
