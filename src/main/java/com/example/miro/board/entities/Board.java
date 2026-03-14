package com.example.miro.board.entities;

import com.example.miro.user.AppUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Board {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @Column(nullable = false)
  private String name;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<BoardMember> members;

  @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DrawObject> objects = new ArrayList<>();
}
