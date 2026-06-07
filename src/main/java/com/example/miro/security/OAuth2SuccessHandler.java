package com.example.miro.security;

import com.example.miro.user.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    OAuthPrincipal principal = (OAuthPrincipal) authentication.getPrincipal();

    String provider = ((OAuth2AuthenticationToken) authentication)
        .getAuthorizedClientRegistrationId()
        .toUpperCase(); // "GOOGLE" or "GITHUB"

    System.out.println(
        "Email: %s | Name: %s | Avatar: %s | Provider: %s"
            .formatted(
                principal.getEmail(),
                principal.getName(),
                principal.getAvatarUrl(),
                provider
            )
    );

    response.sendRedirect("http://localhost:5173/dashboard");
  }
}
