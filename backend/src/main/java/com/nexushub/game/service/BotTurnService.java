package com.nexushub.game.service;

import com.nexushub.game.persistence.GameMapper;
import com.nexushub.game.persistence.GameRow;
import org.springframework.stereotype.Service;

/** 机器人回合调度入口。领取动作和落子事务分开，落子失败时可以可靠地标记 FAILED， 不会因为事务回滚而让坏任务无限重复排队。 */
@Service
public class BotTurnService {
  private final GameMapper games;
  private final BotTurnExecutor executor;

  public BotTurnService(GameMapper games, BotTurnExecutor executor) {
    this.games = games;
    this.executor = executor;
  }

  public boolean process(GameRow queued) {
    if (queued == null || queued.getId() == null || queued.getStateVersion() == null) return false;
    long gameId = queued.getId();
    int beforeVersion = queued.getStateVersion();
    if (games.claimRobotTurn(gameId, beforeVersion) != 1) return false;
    try {
      executor.execute(queued);
      return true;
    } catch (RobotProofRequiredException proofRequired) {
      games.markRobotNeedsProof(gameId, beforeVersion);
      return true;
    } catch (RuntimeException failure) {
      games.markRobotFailed(gameId, beforeVersion);
      throw failure;
    }
  }
}
