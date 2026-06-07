package com.example.miro.board.repository;

import com.example.miro.board.entities.BoardMember;
import com.example.miro.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {
  List<BoardMember> findByUserOrderByLastOpenedAtDesc(AppUser user);

  List<BoardMember> findAllByUserId(UUID userId);

  Optional<BoardMember> findByBoardIdAndUserId(UUID boardId, UUID userId);
}
