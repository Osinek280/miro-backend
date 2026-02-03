package com.example.miro.board;

import com.example.miro.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardMemberRepository extends JpaRepository<BoardMember, UUID> {
  List<BoardMember> findByUserOrderByLastOpenedAtDesc(AppUser user);

  Optional<BoardMember> findByBoardIdAndUser(UUID boardId, AppUser user);
}
