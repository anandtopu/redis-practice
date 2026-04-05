package com.example.api.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
    session.setAttribute("userId", request.userId());
    return ResponseEntity.ok(Map.of("sessionId", session.getId(), "userId", request.userId()));
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(HttpSession session) {
    Object userId = session.getAttribute("userId");
    if (userId == null) {
      return ResponseEntity.status(401).body(Map.of("error", "not_logged_in"));
    }
    return ResponseEntity.ok(Map.of("sessionId", session.getId(), "userId", userId));
  }
}
