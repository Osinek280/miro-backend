package com.example.miro.security;

import com.example.miro.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    System.out.println("=== JWT Filter Debug ===");
    System.out.println("Request URI: " + request.getRequestURI());
    System.out.println("Request Method: " + request.getMethod());

    final String authHeader = request.getHeader("Authorization");
    System.out.println("Auth Header: " + authHeader);

    final String jwt;
    final String userEmail;

    if(authHeader == null || !authHeader.startsWith("Bearer ")) {
      System.out.println("No Bearer token, skipping filter");
      filterChain.doFilter(request, response);
      return;
    }

    jwt = authHeader.substring(7);
    userEmail = jwtService.extractUsername(jwt);
    System.out.println("Extracted email: " + userEmail);

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
      System.out.println("User loaded: " + userDetails.getUsername());
      System.out.println("User authorities: " + userDetails.getAuthorities());

      if(jwtService.isTokenValid(jwt, userDetails)) {
        System.out.println("Token is valid!");
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
        authToken.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
        System.out.println("Authentication set in SecurityContext");
      } else {
        System.out.println("Token is INVALID!");
      }
    }

    System.out.println("=== End JWT Filter ===");
    filterChain.doFilter(request, response);
  }

}
