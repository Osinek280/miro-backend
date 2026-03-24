package com.example.miro.board.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class BoardWebSocketController {
  @MessageMapping("/draw/{boardId}")
  @SendTo("/topic/draw/{boardId}")
  public byte[] draw(@DestinationVariable String boardId,
                     byte[] payload) {
    return payload;
  }
}
