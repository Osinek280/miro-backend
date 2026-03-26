package com.example.miro.board.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "draw_objects",
    indexes = {
        @Index(name = "idx_draw_objects_board_id", columnList = "board_id"),
    }
)
public class DrawObject {

  @Id
//  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "board_id", nullable = false)
  private Board board;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DrawObjectType type;

  @Column(nullable = false)
  private String color;

  @Column(nullable = false)
  private int size;

  @Column(nullable = false)
  private boolean tombstone = false;

  private Instant positionTimestamp;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<Point> points = new ArrayList<>();
}
