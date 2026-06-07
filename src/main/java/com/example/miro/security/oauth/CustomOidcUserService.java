package com.example.miro.security.oauth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

  @Override
  public OAuthPrincipal loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(request);

    String email = oidcUser.getEmail();
    String name = oidcUser.getFullName();
    String avatarUrl = oidcUser.getAttribute("picture");

    return new OAuthPrincipal(email, name, avatarUrl, oidcUser.getAttributes());
  }
}