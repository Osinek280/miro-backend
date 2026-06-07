package com.example.miro.security.config;

import com.example.miro.security.oauth.CustomOAuth2UserService;
import com.example.miro.security.oauth.CustomOidcUserService;
import com.example.miro.security.oauth.OAuth2SuccessHandler;
import com.example.miro.security.jwt.JwtAuthenticationFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfiguration {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AuthenticationProvider authenticationProvider;
  private final CustomOAuth2UserService oauth2UserService;
  private final CustomOidcUserService oidcUserService;
  private final OAuth2SuccessHandler oauth2SuccessHandler;
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/auth/**",
                "/ws/**",
                "/v3/api-docs/**",
                "/hello",
//                "/api/boards/*/camera",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/oauth2/**",
                "/login/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .oauth2Login(oauth -> oauth
            .userInfoEndpoint(userInfo -> userInfo
                .userService(oauth2UserService)      // GitHub
                .oidcUserService(oidcUserService)    // Google
            )
            .successHandler(oauth2SuccessHandler)
        );
    return http.build();
  }
}
