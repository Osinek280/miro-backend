package com.example.miro.board.repository;

import com.example.miro.board.entities.DrawObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrawObjectRepository extends JpaRepository<DrawObject, UUID> {
  List<DrawObject> findAllByBoardId(UUID boardId);
}
