package com.example.miro.board.entities;

import java.util.List;

public record PathData(
    List<Point> points,
    String color,
    int size
) implements DrawObjectData {
}
