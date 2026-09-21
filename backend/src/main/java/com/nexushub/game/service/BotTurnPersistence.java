package com.nexushub.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.common.constants.GameConstants;
import com.nexushub.game.persistence.GameActionRow;
import com.nexushub.game.persistence.GameMapper;
import com.nexushub.game.persistence.GameRow;
import com.nexushub.rules.domain.GameTransition;
import com.nexushub.rules.domain.Move;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 机器人落子持久化事务；求解已经在事务外完成。 */
@Service
public class BotTurnPersistence {
  private final GameMapper games;
  private final ObjectMapper json;

  public BotTurnPersistence(GameMapper games, ObjectMapper json) {
    this.games = games;
    this.json = json;
  }

  @Transactional
  public void persist(GameRow queued, GameTransition transition, Move move) {
    long gameId = queued.getId();
    int beforeVersion = queued.getStateVersion();
    String afterJson = write(transition.state());
    boolean finished = transition.winnerSide() != null;
    String status = finished ? GameConstants.STATUS_FINISHED : GameConstants.STATUS_ACTIVE;
    String winner = finished ? transition.winnerSide().name() : null;
    String robotStatus =
        finished
            ? GameConstants.ROBOT_IDLE
            : (transition.state().currentSeat() == 1
                ? GameConstants.ROBOT_QUEUED
                : GameConstants.ROBOT_IDLE);

    if (games.updateAfterAction(gameId, beforeVersion, afterJson, status, winner, robotStatus)
        != 1) {
      throw new IllegalStateException("机器人落子时对局版本冲突");
    }

    GameActionRow action = new GameActionRow();
    action.setId(games.nextActionId());
    action.setGameId(gameId);
    action.setSequenceNo(beforeVersion + 1);
    action.setActorSeat(1);
    action.setActorType(GameConstants.ACTOR_BOT);
    action.setDecisionQuality(
        GameConstants.MODE_OPTIMAL.equals(queued.getMode())
            ? GameConstants.DECISION_OPTIMAL
            : GameConstants.DECISION_HEURISTIC);
    action.setBeforeVersion(beforeVersion);
    action.setAfterVersion(beforeVersion + 1);
    action.setMoveJson(write(move));
    action.setAfterStateJson(afterJson);
    action.setRequestKey("bot-" + gameId + "-" + beforeVersion);
    games.insertAction(action);
  }

  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("状态序列化失败", e);
    }
  }
}
