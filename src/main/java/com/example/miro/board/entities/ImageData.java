package com.example.miro.board.entities;

public record ImageData(
    double x,
    double y,
    double width,
    double height,
    double rotation,
    String src
) implements DrawObjectData {
}
