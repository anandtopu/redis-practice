package com.example.api.leaderboard;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeaderboardController {

  private final LeaderboardService leaderboardService;

  public LeaderboardController(LeaderboardService leaderboardService) {
    this.leaderboardService = leaderboardService;
  }

  @PostMapping("/leaderboard/incr")
  public ResponseEntity<?> incr(@Valid @RequestBody IncrementScoreRequest request) {
    double score = leaderboardService.incrementScore(request.board(), request.member(), request.delta());
    return ResponseEntity.ok(Map.of("board", request.board(), "member", request.member(), "score", score));
  }

  @GetMapping("/leaderboard/top")
  public ResponseEntity<?> top(@RequestParam String board, @RequestParam(defaultValue = "10") long n) {
    List<LeaderboardEntry> entries = leaderboardService.top(board, n);
    return ResponseEntity.ok(entries);
  }
}
