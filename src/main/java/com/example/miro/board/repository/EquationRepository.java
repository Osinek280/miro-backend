package com.example.miro.board.repository;

import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.equation.Equation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquationRepository extends JpaRepository<Equation, UUID> {
  void deleteByIdAndBoard(UUID id, Board board);
  @Query("SELECT e FROM Equation e WHERE e.board.id = :boardId")
  List<Equation> findByBoardId(@Param("boardId") UUID boardId);
}
