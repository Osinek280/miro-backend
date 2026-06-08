package com.example.miro.security.websocket;

import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.repository.BoardMemberRepository;
import com.example.miro.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private static final Pattern BOARD_DESTINATION_PATTERN =
      Pattern.compile("^/(app|topic)/(draw|cursor)/([0-9a-fA-F\\-]{36})$");

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final BoardMemberRepository boardMemberRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    switch (accessor.getCommand()) {
      case CONNECT -> {
        // the only place where we access DB and validate JWT
        authenticateAndLoadRoles(accessor);
      }
      case SUBSCRIBE -> {
        // only checking principal — no DB access
        WsUserPrincipal principal = extractPrincipal(accessor.getUser());
        checkBoardAccess(principal, accessor.getDestination(), false);
      }
      case SEND -> {
        // only checking principal — no DB access
        WsUserPrincipal principal = extractPrincipal(accessor.getUser());
        checkBoardAccess(principal, accessor.getDestination(), true);
      }
      default -> {
        // DISCONNECT, UNSUBSCRIBE etc. — just pass through
      }
    }

    return message;
  }

  // -------------------------------------------------------------------------
  // CONNECT — the only moment with DB access
  // -------------------------------------------------------------------------
  private void authenticateAndLoadRoles(StompHeaderAccessor accessor) {
    String authHeader = accessor.getFirstNativeHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new AccessDeniedException("Missing or invalid Authorization header");
    }

    String jwt = authHeader.substring(7).trim();
    if (jwt.isBlank()) {
      throw new AccessDeniedException("Empty bearer token");
    }

    String email = jwtService.extractUsername(jwt);
    if (email == null || email.isBlank()) {
      throw new AccessDeniedException("Invalid token subject");
    }

    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
    if (!jwtService.isTokenValid(jwt, userDetails)) {
      throw new AccessDeniedException("Invalid or expired token");
    }

    UUID userId = jwtService.extractUserId(jwt);

    Map<UUID, com.example.miro.board.entities.Role> boardRoles =
        boardMemberRepository.findAllByUserId(userId)
            .stream()
            .collect(Collectors.toMap(
                m -> m.getBoard().getId(),
                BoardMember::getRole
            ));

    WsUserPrincipal principal = new WsUserPrincipal(userId, email);
    principal.loadBoardRoles(boardRoles);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            userDetails.getAuthorities()
        );
    accessor.setUser(auth);
  }

  private void checkBoardAccess(WsUserPrincipal principal, String destination, boolean requireWrite) {
    if (destination == null) return;

    Matcher matcher = BOARD_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      // unknown destination — blocked
      throw new AccessDeniedException("unknown destination: " + destination);
    }

    UUID boardId = UUID.fromString(matcher.group(3));

    if (!principal.hasAccess(boardId)) {
      throw new AccessDeniedException("Access denied to board: " + boardId);
    }

    if (requireWrite && !principal.canWrite(boardId)) {
      throw new AccessDeniedException(
          "User with VIEWER role cannot send messages to board: " + boardId);
    }
  }

  private WsUserPrincipal extractPrincipal(Principal rawPrincipal) {
    if (!(rawPrincipal instanceof Authentication auth)
        || !(auth.getPrincipal() instanceof WsUserPrincipal principal)) {
      throw new AccessDeniedException("Unauthorized WebSocket connection");
    }
    return principal;
  }
}