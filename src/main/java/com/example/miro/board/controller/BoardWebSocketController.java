package com.example.miro.board.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class BoardWebSocketController {
  @MessageMapping("/draw")
  @SendTo("/topic/draw")
  public String draw(String data) {
    System.out.println("Get: " + data);
    return data;
  }
}
