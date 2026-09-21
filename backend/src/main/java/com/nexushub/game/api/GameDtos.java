package com.nexushub.game.api;

import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.Move;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;

/** 游戏 OpenAPI 契约对应 DTO；牌型细节必须由规则引擎再次规范化。 */
public final class GameDtos {
  private GameDtos() {}

  public record GameCreateRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String puzzleVersionId,
      @NotNull @Min(0) @Max(0) Integer playerSeat,
      @NotNull @Min(0) @Max(1) Integer firstSeat,
      @NotBlank @Pattern(regexp = "TRAINING|OPTIMAL") String mode,
      String proofResultId,
      Boolean swapSeats) {}

  public record IdRequest(@NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String id) {}

  public record GameVersionRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String gameId,
      @NotNull @Min(0) Integer expectedStateVersion) {}

  public record PlayRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String gameId,
      @NotNull @Min(0) Integer expectedStateVersion,
      @NotNull Move move) {}

  /** 重开沿用当前对局和题目快照；OPTIMAL 会继续执行逐回合实时求解。 */
  public record GameRestartRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String gameId,
      @NotNull @Min(0) Integer expectedStateVersion,
      @NotBlank @Pattern(regexp = "TRAINING|OPTIMAL") String mode,
      @Pattern(regexp = "^[1-9][0-9]{0,18}$") String proofResultId) {}

  public record LegalMovesRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String gameId,
      @NotNull @Min(0) Integer expectedStateVersion,
      @Min(1) Integer pageNum,
      @Min(1) @Max(100) Integer pageSize) {}

  public record LegalMove(Move move, String proofStatus, String winnerSide) {}

  public record LegalMovePage(List<LegalMove> items, long total, int pageNum, int pageSize) {}

  public record ReplayRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String gameId,
      @Min(1) @Max(100000) Integer pageNum,
      @Min(1) @Max(100) Integer pageSize) {}

  public record GameAction(
      int sequenceNo,
      int actorSeat,
      String actorType,
      Move move,
      int beforeVersion,
      int afterVersion,
      GameState afterState,
      String decisionQuality,
      Instant createTime) {}

  public record Replay(
      Game game,
      GameState initialState,
      List<GameAction> actions,
      long total,
      int pageNum,
      int pageSize) {}

  /** 同步提示结果；提示接口仍只给出规则合法建议，严格求解由 OPTIMAL 机器人回合负责。 */
  public record HintResponse(
      Move move, String proofStatus, String winnerSide, int hintCount, int stateVersion) {}

  public record Game(
      String id,
      String puzzleVersionId,
      int playerSeat,
      int firstSeat,
      String mode,
      String status,
      int stateVersion,
      GameState state,
      String winnerSide,
      String robotStatus,
      int hintCount,
      List<String> allowedActions,
      Instant createTime) {}
}
