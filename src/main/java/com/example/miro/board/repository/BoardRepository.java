package com.example.miro.board.repository;

import com.example.miro.board.entities.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
  @Query("SELECT b FROM Board b LEFT JOIN FETCH b.objects WHERE b.id = :id")
  Optional<Board> findWithObjectsById(@Param("id") UUID id);
}
