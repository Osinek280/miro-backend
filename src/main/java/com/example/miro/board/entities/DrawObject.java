package com.example.miro.board.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "draw_objects")
public class DrawObject {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @Enumerated(EnumType.STRING)
  private DrawObjectType type;
  private String color;
  private int size;
  private boolean tombstone;
  private Instant positionTimestamp;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private List<Point> points;
}
