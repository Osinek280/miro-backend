package com.example.Miro.auth;

import com.example.Miro.config.JwtService;
import com.example.Miro.entities.AppUser;
import com.example.Miro.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthenticationResponse register(RegisterRequest request) {
    AppUser user = new AppUser();
    user.setFirstname(request.getFirstname());
    user.setLastname(request.getLastname());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    userRepository.save(user);

    String token = jwtService.generateToken(user.getEmail());
    return AuthenticationResponse.builder()
        .token(token)
        .build();
  }
//  public UserInfoResponse getUserInfo(String email) {
//    var user = userRepository.findByEmail(email)
//        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
//    return new UserInfoResponse(user.getEmail(), user.getFirstname());
//  }



  public AuthenticationResponse login(LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.email(),
              request.password()
          )
      );

      AppUser user = userRepository.findByEmail(request.email())
          .orElseThrow(() -> new RuntimeException("User not found"));

      String token = jwtService.generateToken(user.getEmail());

      return AuthenticationResponse.builder()
          .token(token)
          .build();

    } catch (Exception e) {
      System.out.println("Login failed: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }
}
