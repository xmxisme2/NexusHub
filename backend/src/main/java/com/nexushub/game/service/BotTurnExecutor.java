package com.nexushub.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.analysis.solver.MinimaxSolver;
import com.nexushub.common.constants.GameConstants;
import com.nexushub.game.persistence.GameRow;
import com.nexushub.rules.domain.BotStrategy;
import com.nexushub.rules.domain.GameRules;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.GameTransition;
import com.nexushub.rules.domain.Move;
import org.springframework.stereotype.Service;

/** 机器人落子编排；深度求解在数据库事务外运行。 */
@Service
public class BotTurnExecutor {
  private static final long OPTIMAL_TIME_LIMIT_MS = 30_000L;
  private static final long OPTIMAL_NODE_LIMIT = 1_000_000L;

  private final ObjectMapper json;
  private final BotTurnPersistence persistence;

  public BotTurnExecutor(ObjectMapper json, BotTurnPersistence persistence) {
    this.json = json;
    this.persistence = persistence;
  }

  public void execute(GameRow queued) {
    GameState before = readState(queued.getCurrentStateJson());
    if (before.currentSeat() != 1) {
      throw new IllegalStateException("机器人队列与当前座位不一致");
    }

    Move move = chooseMove(queued, before);
    GameTransition transition = GameRules.apply(before, move);
    persistence.persist(queued, transition, move);
  }

  private Move chooseMove(GameRow queued, GameState state) {
    if (!GameConstants.MODE_OPTIMAL.equals(queued.getMode())) {
      return BotStrategy.choose(state);
    }

    MinimaxSolver.Outcome outcome =
        new MinimaxSolver()
            .solve(state, new MinimaxSolver.Budget(OPTIMAL_TIME_LIMIT_MS, OPTIMAL_NODE_LIMIT));
    if (!"PROVEN".equals(outcome.proofStatus()) || outcome.recommendedMove() == null) {
      throw new RobotProofRequiredException("当前机器人回合尚未完成必胜证明");
    }
    return outcome.recommendedMove();
  }

  private GameState readState(String raw) {
    try {
      return json.readValue(raw, GameState.class);
    } catch (Exception e) {
      throw new IllegalStateException("对局状态损坏", e);
    }
  }
}
