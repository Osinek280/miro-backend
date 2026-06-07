package com.example.miro.security.websocket;

import com.example.miro.board.entities.Role;
import lombok.Getter;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class WsUserPrincipal implements Principal {

  private final UUID userId;
  private final String email;

  private final Map<UUID, Role> boardRoles = new ConcurrentHashMap<>();

  public WsUserPrincipal(UUID userId, String email) {
    this.userId = userId;
    this.email = email;
  }

  public void loadBoardRoles(Map<UUID, Role> roles) {
    boardRoles.putAll(roles);
  }

  public boolean hasAccess(UUID boardId) {
    return boardRoles.containsKey(boardId);
  }

  public boolean canWrite(UUID boardId) {
    Role role = boardRoles.get(boardId);
    return role != null && role != Role.VIEWER;
  }

  @Override
  public String getName() {
    return email;
  }
}