package com.example.miro.board;

import com.example.miro.user.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
  List<Board> findByOwner(AppUser owner);

  Page<Board> findAll(Pageable pageable);
}
