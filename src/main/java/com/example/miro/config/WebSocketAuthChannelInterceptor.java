package com.example.miro.config;

import com.example.miro.auth.service.JwtService;
import com.example.miro.board.entities.BoardMember;
import com.example.miro.board.entities.Role;
import com.example.miro.board.repository.BoardMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      authenticateConnection(accessor);
      return message;
    }

    if (StompCommand.SEND.equals(accessor.getCommand())
        || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      requireAuthenticatedUser(accessor.getUser());
      authorizeBoardAccess(accessor);
    }

    return message;
  }

  private void authenticateConnection(StompHeaderAccessor accessor) {
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
    WsUserPrincipal principal = new WsUserPrincipal(userId, email);

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            userDetails.getAuthorities()
        );
    accessor.setUser(auth);
  }

  private void requireAuthenticatedUser(Principal principal) {
    if (!(principal instanceof Authentication authentication)
        || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Unauthorized WebSocket action");
    }
  }

  private void authorizeBoardAccess(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null) {
      return;
    }

    Matcher matcher = BOARD_DESTINATION_PATTERN.matcher(destination);
    if (!matcher.matches()) {
      return;
    }

    String boardIdRaw = matcher.group(3);
    UUID boardId = UUID.fromString(boardIdRaw);
    UUID userId = extractUserIdFromPrincipal(accessor.getUser());

    BoardMember member = boardMemberRepository
        .findByBoardIdAndUserId(boardId, userId)
        .orElseThrow(() -> new AccessDeniedException("No access to board"));

    if (StompCommand.SEND.equals(accessor.getCommand())
        && member.getRole() == Role.VIEWER) {
      throw new AccessDeniedException("Insufficient permissions for write action");
    }
  }

  private UUID extractUserIdFromPrincipal(Principal principal) {
    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof WsUserPrincipal wsUserPrincipal)) {
      throw new AccessDeniedException("Unauthorized WebSocket action");
    }
    return wsUserPrincipal.getUserId();
  }
}
