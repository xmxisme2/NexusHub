package com.nexushub.analysis.solver;

import com.nexushub.common.constants.RulesConstants;
import com.nexushub.rules.domain.GameRules;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.GameTransition;
import com.nexushub.rules.domain.Move;
import com.nexushub.rules.domain.Seat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 小残局 V1 的完整双人 Minimax。每个节点都要求对手分支覆盖后才返回 PROVEN， 超过时间或节点预算返回 UNKNOWN；搜索不依赖 Spring、数据库或 HTTP。 */
public final class MinimaxSolver {
  public record Budget(long timeLimitMs, long nodeLimit) {}

  public record Outcome(
      String proofStatus,
      Seat winner,
      Integer proofValue,
      Move recommendedMove,
      List<List<Move>> winningLines,
      long visitedNodes,
      long cacheHits,
      long elapsedMs,
      String terminationReason) {}

  private record Key(String value) {}

  private record Node(int value, boolean proven, List<List<Move>> lines, Move recommendedMove) {}

  private final Map<Key, Node> cache = new HashMap<>();
  private long startedAt;
  private long nodeLimit;
  private long deadline;
  private long visited;
  private long cacheHits;
  private boolean exhausted;

  public Outcome solve(GameState root, Budget budget) {
    if (root == null) throw new IllegalArgumentException("求解局面不能为空");
    startedAt = System.nanoTime();
    nodeLimit = Math.max(1, budget.nodeLimit());
    deadline = startedAt + Math.max(1, budget.timeLimitMs()) * 1_000_000L;
    visited = 0;
    cacheHits = 0;
    exhausted = false;
    cache.clear();
    Node result;
    try {
      result = search(root);
    } catch (BudgetExceeded ignored) {
      result = new Node(0, false, List.of(), null);
    }
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    String reason =
        result.proven() ? "PROVED" : (visited >= nodeLimit ? "NODE_LIMIT" : "TIME_LIMIT");
    Seat winner = result.proven() ? (result.value() > 0 ? Seat.USER : Seat.BOT) : null;
    Integer proofValue = result.proven() ? result.value() : null;
    Move recommended = result.proven() ? result.recommendedMove() : null;
    return new Outcome(
        result.proven() ? "PROVEN" : "UNKNOWN",
        winner,
        proofValue,
        recommended,
        result.lines(),
        visited,
        cacheHits,
        elapsed,
        reason);
  }

  private Node search(GameState state) {
    checkBudget();
    int terminal = terminalValue(state);
    if (terminal != 0) {
      return new Node(terminal, true, List.of(List.of()), null);
    }
    Key key = new Key(keyOf(state));
    Node cached = cache.get(key);
    if (cached != null) {
      cacheHits++;
      return cached;
    }
    visited++;
    List<Move> moves = legalMoves(state);
    boolean max = state.currentSeat() == 0;
    boolean allProven = true;
    List<List<Move>> winningLines = new ArrayList<>();
    Move fallbackMove = null;
    List<List<Move>> fallbackLines = List.of();
    for (Move move : moves) {
      checkBudget();
      GameTransition transition = GameRules.apply(state, move);
      Node child = search(transition.state());
      if (!child.proven()) {
        allProven = false;
        continue;
      }
      int value = child.value();
      if (fallbackMove == null) {
        fallbackMove = move;
        fallbackLines = child.lines();
      }
      boolean immediateWin = max ? value > 0 : value < 0;
      if (immediateWin) {
        for (List<Move> line : child.lines()) {
          if (winningLines.size() >= 5) break;
          List<Move> next = new ArrayList<>();
          next.add(move);
          next.addAll(line);
          winningLines.add(List.copyOf(next));
        }
        Node proven = new Node(max ? 1 : -1, true, List.copyOf(winningLines), move);
        cache.put(key, proven);
        return proven;
      }
    }
    if (allProven) {
      int value = max ? -1 : 1;
      List<List<Move>> lines = new ArrayList<>();
      if (fallbackMove != null) {
        for (List<Move> line : fallbackLines) {
          if (lines.size() >= 5) break;
          List<Move> next = new ArrayList<>();
          next.add(fallbackMove);
          next.addAll(line);
          lines.add(List.copyOf(next));
        }
      }
      Node proven = new Node(value, true, List.copyOf(lines), fallbackMove);
      cache.put(key, proven);
      return proven;
    }
    Node unknown = new Node(0, false, List.of(), null);
    cache.put(key, unknown);
    return unknown;
  }

  private List<Move> legalMoves(GameState state) {
    List<Move> moves = new ArrayList<>(GameRules.legalMoves(state));
    if (!state.isFirstMove() && state.targetMove() != null) moves.add(Move.pass());
    return moves;
  }

  private void checkBudget() {
    if (visited >= nodeLimit || System.nanoTime() >= deadline) {
      exhausted = true;
      throw new BudgetExceeded();
    }
  }

  private int terminalValue(GameState state) {
    if (total(state.hands()[0]) == 0) return 1;
    if (total(state.hands()[1]) == 0) return -1;
    return 0;
  }

  private int total(int[] cards) {
    int n = 0;
    for (int c : cards) n += c;
    return n;
  }

  private String keyOf(GameState s) {
    return Arrays.deepToString(s.hands())
        + '|'
        + s.currentSeat()
        + '|'
        + s.firstSeat()
        + '|'
        + s.isFirstMove()
        + '|'
        + s.targetMove()
        + '|'
        + s.lastPlaySeat()
        + '|'
        + s.consecutivePasses()
        + '|'
        + RulesConstants.CLASSIC_V1;
  }

  private static final class BudgetExceeded extends RuntimeException {}
}
