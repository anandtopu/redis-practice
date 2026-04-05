package com.example.api.leaderboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

  private final StringRedisTemplate redis;

  public LeaderboardService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public double incrementScore(String board, String member, double delta) {
    String boardName = Objects.requireNonNull(board, "board");
    String memberName = Objects.requireNonNull(member, "member");
    String key = boardKey(boardName);
    Double score = redis.opsForZSet().incrementScore(key, memberName, delta);
    return score == null ? 0.0 : score;
  }

  public List<LeaderboardEntry> top(String board, long n) {
    String boardName = Objects.requireNonNull(board, "board");
    String key = boardKey(boardName);
    Set<ZSetOperations.TypedTuple<String>> tuples = redis.opsForZSet()
        .reverseRangeWithScores(key, 0, n - 1);

    List<LeaderboardEntry> entries = new ArrayList<>();
    if (tuples == null) {
      return entries;
    }

    long rank = 1;
    for (ZSetOperations.TypedTuple<String> t : tuples) {
      String member = t.getValue();
      Double score = t.getScore();
      if (member != null) {
        entries.add(new LeaderboardEntry(rank, member, score == null ? 0.0 : score));
        rank++;
      }
    }

    return entries;
  }

  private static String boardKey(String board) {
    String boardName = Objects.requireNonNull(board, "board");
    return "lb:" + boardName;
  }
}
