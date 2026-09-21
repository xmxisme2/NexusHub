package com.nexushub.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.common.constants.GameConstants;
import com.nexushub.common.constants.PuzzleConstants;
import com.nexushub.game.api.GameDtos.*;
import com.nexushub.game.persistence.GameActionRow;
import com.nexushub.game.persistence.GameMapper;
import com.nexushub.game.persistence.GameRow;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.puzzle.persistence.PuzzleMapper;
import com.nexushub.puzzle.persistence.PuzzleRow;
import com.nexushub.rules.domain.BotStrategy;
import com.nexushub.rules.domain.GameRules;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.GameTransition;
import com.nexushub.rules.domain.Move;
import com.nexushub.rules.domain.MoveType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 对局事务编排：数据库快照与行动事件始终一起提交，牌局胜负只由规则层产生。 */
@Service
public class GameService {
  private final GameMapper games;
  private final PuzzleMapper puzzles;
  private final ObjectMapper json;

  public GameService(GameMapper games, PuzzleMapper puzzles, ObjectMapper json) {
    this.games = games;
    this.puzzles = puzzles;
    this.json = json;
  }

  @Transactional
  public Game create(GameCreateRequest request, UserRow actor) {
    validateMode(request.mode());
    PuzzleRow puzzle = puzzles.findByVersionId(Long.parseLong(request.puzzleVersionId()));
    if (puzzle == null
        || PuzzleConstants.STATUS_ARCHIVED.equals(puzzle.getStatus())
        || (!PuzzleConstants.STATUS_PUBLISHED.equals(puzzle.getStatus())
            && !canRead(actor, puzzle))) throw new java.util.NoSuchElementException("题目版本不存在或不可访问");
    int originalFirstSeat = request.firstSeat();
    if (!allowedSeats(puzzle).contains(originalFirstSeat))
      throw new IllegalArgumentException("题目不允许选择该先手座位");
    GameState puzzleState = readState(puzzle.getInitialStateJson());
    boolean swapSeats = Boolean.TRUE.equals(request.swapSeats());
    int effectiveFirstSeat = swapSeats ? 1 - originalFirstSeat : originalFirstSeat;
    int[][] hands = puzzleState.hands();
    if (swapSeats) {
      int[] userHand = hands[0];
      hands[0] = hands[1];
      hands[1] = userHand;
    }
    GameState initial = GameRules.initial(hands, effectiveFirstSeat);
    GameRow row = new GameRow();
    row.setId(games.nextGameId());
    row.setOwnerId(actor.getId());
    row.setPuzzleVersionId(puzzle.getVersionId());
    row.setFirstSeat(effectiveFirstSeat);
    row.setMode(request.mode());
    row.setStatus(GameConstants.STATUS_ACTIVE);
    row.setInitialStateJson(write(initial));
    row.setCurrentStateJson(write(initial));
    row.setStateVersion(0);
    row.setRobotStatus(
        effectiveFirstSeat == 1 ? GameConstants.ROBOT_QUEUED : GameConstants.ROBOT_IDLE);
    row.setHintCount(0);
    games.insertGame(row);
    return toGame(games.findGame(row.getId()));
  }

  public Game detail(IdRequest request, UserRow actor) {
    return toGame(owned(Long.parseLong(request.id()), actor));
  }

  /** 读取当前用户自己的行动事件，复盘只使用持久化的状态快照，不重新推断历史。 */
  public Replay replay(ReplayRequest request, UserRow actor) {
    GameRow row = owned(Long.parseLong(request.gameId()), actor);
    int page = request.pageNum() == null ? 1 : request.pageNum();
    int size = request.pageSize() == null ? 20 : request.pageSize();
    int offset = (page - 1) * size;
    List<GameAction> actions =
        games.listActions(row.getId(), offset, size).stream().map(this::toAction).toList();
    return new Replay(
        toGame(row),
        readState(row.getInitialStateJson()),
        actions,
        games.countActions(row.getId()),
        page,
        size);
  }

  public LegalMovePage legalMoves(LegalMovesRequest request, UserRow actor) {
    GameRow row = owned(Long.parseLong(request.gameId()), actor);
    if (row.getStateVersion() != request.expectedStateVersion())
      throw new IllegalStateException("对局版本冲突");
    int page = request.pageNum() == null ? 1 : request.pageNum(),
        size = request.pageSize() == null ? 20 : request.pageSize();
    GameState state = readState(row.getCurrentStateJson());
    if (!GameConstants.STATUS_ACTIVE.equals(row.getStatus()) || state.currentSeat() != 0)
      return new LegalMovePage(List.of(), 0, page, size);
    List<Move> all = new ArrayList<>(GameRules.legalMoves(state));
    // 有当前牌墩且不是首手时，PASS 也是用户可选的合法行动。
    if (!state.isFirstMove() && state.targetMove() != null) all.add(Move.pass());
    long start = (long) (page - 1) * size;
    if (start >= all.size()) return new LegalMovePage(List.of(), all.size(), page, size);
    int from = (int) start, to = Math.min(from + size, all.size());
    return new LegalMovePage(
        all.subList(from, to).stream().map(move -> new LegalMove(move, "UNKNOWN", null)).toList(),
        all.size(),
        page,
        size);
  }

  /**
   * 同步 V1 提示：仅依据当前完整局面枚举规则合法行动并选择确定性建议，不执行证明搜索。 提示不改写 current_state、state_version 或
   * game_action；只增加可审计的 hint_count。
   */
  @Transactional
  public HintResponse hint(GameVersionRequest request, String requestKey, UserRow actor) {
    GameRow current = owned(Long.parseLong(request.gameId()), actor);
    if (current.getStateVersion() != request.expectedStateVersion())
      throw new IllegalStateException("对局版本冲突");
    if (!GameConstants.STATUS_ACTIVE.equals(current.getStatus()))
      throw new IllegalStateException("对局已结束或已放弃");
    GameState state = readState(current.getCurrentStateJson());
    if (state.currentSeat() != 0) throw new IllegalStateException("当前不是用户回合");
    Move suggestion = BotStrategy.chooseHint(state);
    if (games.incrementHintCount(current.getId(), request.expectedStateVersion()) != 1) {
      throw new IllegalStateException("对局版本冲突");
    }
    GameRow updated = games.findGame(current.getId());
    return new HintResponse(
        suggestion, "UNKNOWN", null, updated.getHintCount(), updated.getStateVersion());
  }

  @Transactional
  public Game play(PlayRequest request, String requestKey, UserRow actor) {
    if (request.move().type() == MoveType.PASS) throw new IllegalArgumentException("PASS 必须调用不出接口");
    return apply(
        Long.parseLong(request.gameId()),
        request.expectedStateVersion(),
        request.move(),
        requestKey,
        actor);
  }

  @Transactional
  public Game pass(GameVersionRequest request, String requestKey, UserRow actor) {
    return apply(
        Long.parseLong(request.gameId()),
        request.expectedStateVersion(),
        Move.pass(),
        requestKey,
        actor);
  }

  /** 在同一对局内恢复创建时的不可变初始快照。历史行动只做逻辑删除，状态版本递增， 这样已经领取的机器人任务无法用旧版本覆盖重开后的局面。 */
  @Transactional
  public Game restart(GameRestartRequest request, String requestKey, UserRow actor) {
    validateMode(request.mode());
    long gameId = Long.parseLong(request.gameId());
    GameRow current = owned(gameId, actor);
    if (current.getStateVersion() != request.expectedStateVersion())
      throw new IllegalStateException("对局版本冲突");
    GameState initial = readState(current.getInitialStateJson());
    String initialJson = write(initial);
    String robotStatus =
        initial.firstSeat() == 1 ? GameConstants.ROBOT_QUEUED : GameConstants.ROBOT_IDLE;
    if (games.restartGame(
            gameId,
            request.expectedStateVersion(),
            initialJson,
            robotStatus,
            request.mode(),
            actor.getId())
        != 1) {
      throw new IllegalStateException("对局版本冲突");
    }
    games.clearActions(gameId);
    return toGame(games.findGame(gameId));
  }

  /** 弃局只结束权威快照并保留已有行动事件，版本 CAS 可隔离重复请求和机器人旧任务。 */
  @Transactional
  public Game abandon(GameVersionRequest request, String requestKey, UserRow actor) {
    long gameId = Long.parseLong(request.gameId());
    GameRow current = owned(gameId, actor);
    if (!GameConstants.STATUS_ACTIVE.equals(current.getStatus()))
      throw new IllegalStateException("对局已结束或已放弃");
    if (current.getStateVersion() != request.expectedStateVersion())
      throw new IllegalStateException("对局版本冲突");
    if (games.abandonGame(gameId, request.expectedStateVersion(), actor.getId()) != 1)
      throw new IllegalStateException("对局版本冲突");
    return toGame(games.findGame(gameId));
  }

  private Game apply(
      long gameId, int expectedVersion, Move move, String requestKey, UserRow actor) {
    GameRow current = owned(gameId, actor);
    if (!"ACTIVE".equals(current.getStatus())) throw new IllegalStateException("对局已结束或已放弃");
    if (current.getStateVersion() != expectedVersion) throw new IllegalStateException("对局版本冲突");
    GameState before = readState(current.getCurrentStateJson());
    if (before.currentSeat() != 0) throw new IllegalStateException("当前不是用户回合");
    GameTransition transition = GameRules.apply(before, move);
    GameState after = transition.state();
    String afterJson = write(after);
    boolean finished = transition.winnerSide() != null;
    String status = finished ? GameConstants.STATUS_FINISHED : GameConstants.STATUS_ACTIVE;
    String winner = finished ? transition.winnerSide().name() : null;
    String robot =
        finished
            ? GameConstants.ROBOT_IDLE
            : (after.currentSeat() == 1 ? GameConstants.ROBOT_QUEUED : GameConstants.ROBOT_IDLE);
    if (games.updateAfterAction(gameId, expectedVersion, afterJson, status, winner, robot) != 1)
      throw new IllegalStateException("对局版本冲突");
    GameActionRow action = new GameActionRow();
    action.setId(games.nextActionId());
    action.setGameId(gameId);
    action.setSequenceNo(expectedVersion + 1);
    action.setActorSeat(0);
    action.setActorType(GameConstants.ACTOR_HUMAN);
    action.setDecisionQuality(GameConstants.ACTOR_HUMAN);
    action.setBeforeVersion(expectedVersion);
    action.setAfterVersion(expectedVersion + 1);
    action.setMoveJson(
        write(move.type() == MoveType.PASS ? Move.pass() : GameRules.canonicalize(move.cards())));
    action.setAfterStateJson(afterJson);
    action.setRequestKey(requestKey);
    games.insertAction(action);
    return toGame(games.findGame(gameId));
  }

  private void validateMode(String mode) {
    if (!GameConstants.MODE_TRAINING.equals(mode) && !GameConstants.MODE_OPTIMAL.equals(mode)) {
      throw new IllegalArgumentException("对局模式非法");
    }
  }

  private GameRow owned(long id, UserRow actor) {
    GameRow row = games.findGame(id);
    if (row == null || !actor.getId().equals(row.getOwnerId()))
      throw new java.util.NoSuchElementException("对局不存在");
    return row;
  }

  private GameAction toAction(GameActionRow row) {
    try {
      return new GameAction(
          row.getSequenceNo(),
          row.getActorSeat(),
          row.getActorType(),
          json.readValue(row.getMoveJson(), Move.class),
          row.getBeforeVersion(),
          row.getAfterVersion(),
          readState(row.getAfterStateJson()),
          row.getDecisionQuality(),
          row.getCreateTime());
    } catch (Exception e) {
      throw new IllegalStateException("行动事件损坏", e);
    }
  }

  private boolean canRead(UserRow actor, PuzzleRow puzzle) {
    return "ADMIN".equals(actor.getRole()) || actor.getId().equals(puzzle.getOwnerId());
  }

  private List<Integer> allowedSeats(PuzzleRow row) {
    try {
      return json.readValue(
          row.getAllowedFirstSeatsJson(),
          json.getTypeFactory().constructCollectionType(List.class, Integer.class));
    } catch (Exception e) {
      throw new IllegalStateException("题目先手配置损坏", e);
    }
  }

  private GameState readState(String raw) {
    try {
      return json.readValue(raw, GameState.class);
    } catch (Exception e) {
      throw new IllegalStateException("对局状态损坏", e);
    }
  }

  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("状态序列化失败", e);
    }
  }

  private Game toGame(GameRow row) {
    GameState state = readState(row.getCurrentStateJson());
    return new Game(
        String.valueOf(row.getId()),
        String.valueOf(row.getPuzzleVersionId()),
        0,
        row.getFirstSeat(),
        row.getMode(),
        row.getStatus(),
        row.getStateVersion(),
        state,
        row.getWinnerSide(),
        row.getRobotStatus(),
        row.getHintCount(),
        actions(row, state),
        row.getCreateTime());
  }

  private List<String> actions(GameRow row, GameState state) {
    if (!"ACTIVE".equals(row.getStatus())) return List.of("RESTART");
    if (state.currentSeat() != 0) return List.of("ABANDON", "RESTART");
    java.util.ArrayList<String> actions =
        new java.util.ArrayList<>(List.of("PLAY", "HINT", "ABANDON", "RESTART"));
    if (!state.isFirstMove() && state.targetMove() != null) actions.add(1, "PASS");
    return List.copyOf(actions);
  }
}
