package com.example.miro.board.controller;

import com.example.miro.board.dto.*;
import com.example.miro.board.service.BoardService;
import com.example.miro.board.service.BoardSnapshotService;
import com.example.miro.user.AppUser;
import com.example.miro.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
  private final BoardService boardService;
  private final UserRepository userRepository;
  private final BoardSnapshotService boardSnapshotService;

  @GetMapping
  public List<BoardViewDto> myBoards(@AuthenticationPrincipal UserDetails userDetails) {
    AppUser user = userRepository.findByEmail(userDetails.getUsername())
        .orElseThrow(() -> new RuntimeException("User not found"));

    System.out.println(user.getEmail());

    return boardService.getMyBoards(user);
  }

  @PostMapping
  public ResponseEntity<UUID> create(@RequestBody CreateBoardRequest req,
                                     @AuthenticationPrincipal AppUser user) {
    UUID id = boardService.createBoard(req.name(), user);
    return ResponseEntity.created(URI.create("/api/boards/" + id)).body(id);
  }

  @PatchMapping("/{boardId}/rename")
  public ResponseEntity<Void> rename(@PathVariable UUID boardId,
                                     @RequestBody RenameBoardRequest req,
                                     @AuthenticationPrincipal AppUser user) {
    boardService.renameBoard(boardId, req.name(), user);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{boardId}")
  public ResponseEntity<Void> delete(@PathVariable UUID boardId,
                                     @AuthenticationPrincipal AppUser user) {
    boardService.deleteBoard(boardId, user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{boardId}/snapshot")
  public ResponseEntity<BoardSnapshotDto> getSnapshot(@PathVariable UUID boardId,
                                                      @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(boardSnapshotService.getSnapshot(boardId, user.getId()));
  }
}
