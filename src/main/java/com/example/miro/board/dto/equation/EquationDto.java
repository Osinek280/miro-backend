package com.example.miro.board.dto.equation;

import java.util.UUID;

public record EquationDto(
    UUID id,
    String latex
//    ColorDto color
) {
}
