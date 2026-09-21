package com.nexushub.analysis.solver;

import static org.junit.jupiter.api.Assertions.*;

import com.nexushub.rules.domain.GameRules;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.Seat;
import org.junit.jupiter.api.Test;

class MinimaxSolverTest {
  @Test
  void provesImmediateUserWin() {
    int[] user = new int[15], bot = new int[15];
    user[0] = 1;
    bot[1] = 1;
    GameState state = GameRules.initial(new int[][] {user, bot}, 0);
    MinimaxSolver.Outcome result =
        new MinimaxSolver().solve(state, new MinimaxSolver.Budget(1000, 1000));
    assertEquals("PROVEN", result.proofStatus());
    assertEquals(Seat.USER, result.winner());
    assertEquals(1, result.proofValue());
    assertFalse(result.winningLines().isEmpty());
  }

  @Test
  void returnsUnknownWhenNodeBudgetStopsSearch() {
    int[] user = new int[15], bot = new int[15];
    user[0] = 1;
    bot[1] = 1;
    GameState state = GameRules.initial(new int[][] {user, bot}, 0);
    MinimaxSolver.Outcome result =
        new MinimaxSolver().solve(state, new MinimaxSolver.Budget(1000, 1));
    assertEquals("UNKNOWN", result.proofStatus());
    assertNull(result.winner());
  }

  @Test
  void provesTestPuzzleBotWin() {
    int[] user = {0, 0, 0, 2, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 1};
    int[] bot = {0, 0, 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0};
    GameState state = GameRules.initial(new int[][] {user, bot}, 0);

    MinimaxSolver.Outcome result =
        new MinimaxSolver().solve(state, new MinimaxSolver.Budget(30_000, 1_000_000L));

    assertEquals("PROVEN", result.proofStatus());
    assertEquals(Seat.BOT, result.winner());
    assertNotNull(result.recommendedMove());
  }
}
