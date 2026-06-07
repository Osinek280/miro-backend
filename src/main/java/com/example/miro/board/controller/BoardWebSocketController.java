package com.example.miro.board.controller;

import com.example.miro.board.service.BoardSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BoardWebSocketController {

  private final BoardSnapshotService boardSnapshotService;

  @MessageMapping("/draw/{boardId}")
  @SendTo("/topic/draw/{boardId}")
  public byte[] draw(@DestinationVariable UUID boardId,
                     byte[] payload,
                     Principal principal) {
    boardSnapshotService.applyOperation(boardId, payload);
    System.out.println(principal.getName());
    return payload;
  }

  @MessageMapping("/cursor/{boardId}")
  @SendTo("/topic/cursor/{boardId}")
  public byte[] cursor(@DestinationVariable String boardId,
                       byte[] payload) {
    return payload;
  }
}
