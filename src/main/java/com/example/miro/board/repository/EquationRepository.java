package com.example.miro.board.repository;

import com.example.miro.board.entities.Board;
import com.example.miro.board.entities.equation.Equation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquationRepository extends JpaRepository<Equation, UUID> {
  void deleteByIdAndBoard(UUID id, Board board);
  List<Equation> findAllByBoard(Board board);
}
