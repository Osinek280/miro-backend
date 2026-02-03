package com.example.miro.board.controller;

import com.example.miro.board.dto.BoardViewDto;
import com.example.miro.board.dto.CameraRequest;
import com.example.miro.board.dto.CreateBoardRequest;
import com.example.miro.board.service.BoardService;
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

  @PostMapping("/{id}/open")
  public BoardViewDto open(
      @PathVariable UUID id,
      @RequestBody CameraRequest camera,
      @AuthenticationPrincipal AppUser user
  ) {
    return boardService.openBoard(
        id, user,
        camera.x(), camera.y(), camera.zoom()
    );
  }
}
