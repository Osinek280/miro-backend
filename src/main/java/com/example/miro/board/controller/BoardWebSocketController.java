package com.example.miro.board.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class BoardWebSocketController {
  @MessageMapping("/draw/{boardId}")
  @SendTo("/topic/draw/{boardId}")
  public byte[] draw(@DestinationVariable String boardId,
                     byte[] payload,
                     Principal principal) {
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
