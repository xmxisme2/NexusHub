package com.nexushub.game.service;

import com.nexushub.game.persistence.GameMapper;
import com.nexushub.game.persistence.GameRow;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时领取机器人回合；单次只取少量任务，避免低配环境阻塞用户请求。 */
@Component
public class BotTurnWorker {
  private static final Logger log = LoggerFactory.getLogger(BotTurnWorker.class);
  private final GameMapper games;
  private final BotTurnService botTurns;

  public BotTurnWorker(GameMapper games, BotTurnService botTurns) {
    this.games = games;
    this.botTurns = botTurns;
  }

  @Scheduled(fixedDelayString = "${nexushub.bot.worker-delay-ms:500}")
  public void poll() {
    List<GameRow> queued = games.findQueuedRobotGames(8);
    for (GameRow game : queued) {
      try {
        botTurns.process(game);
      } catch (RuntimeException failure) {
        log.warn("机器人回合处理失败 gameId={}", game.getId(), failure);
      }
    }
  }
}
