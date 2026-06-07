package com.example.miro.board.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PathData.class, name = "PATH"),
    @JsonSubTypes.Type(value = ImageData.class, name = "IMAGE"),
})
public sealed interface DrawObjectData extends Serializable permits PathData, ImageData {
}
