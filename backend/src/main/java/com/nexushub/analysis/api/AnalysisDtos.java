package com.nexushub.analysis.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexushub.rules.domain.Move;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/** 分析任务请求与结果 DTO；UNKNOWN 不允许携带胜方或 proofValue。 */
public final class AnalysisDtos {
  private AnalysisDtos() {}

  public record Budget(
      @NotNull @Min(100) @Max(60000) Integer timeLimitMs,
      @NotNull @Min(1000) @Max(5000000) Long nodeLimit,
      @NotNull @Min(16) @Max(128) Integer memoryLimitMb) {}

  public record SolveCreateRequest(@NotNull JsonNode source, Budget budget) {}

  public record TaskSearchRequest(
      @Min(1) @Max(100000) Integer pageNum,
      @Min(1) @Max(100) Integer pageSize,
      String taskStatus) {}

  public record SolveStats(long elapsedMs, long queueTimeMs, long visitedNodes, long cacheHits) {}

  public record WinningLine(int lineNo, List<Move> moves, boolean verified) {}

  public record SolveResult(
      String id,
      String proofStatus,
      String winnerSide,
      Integer proofValue,
      String strategyStatus,
      String rootNodeId,
      Move recommendedMove,
      List<WinningLine> winningLines,
      String solverVersion,
      String rulesetVersion,
      SolveStats stats) {}

  public record EffectiveBudget(int timeLimitMs, long nodeLimit, int memoryLimitMb) {}

  public record SolveTask(
      String id,
      String taskStatus,
      String purpose,
      EffectiveBudget effectiveBudget,
      String stateHash,
      SolveResult result,
      String terminationReason,
      SolveStats stats,
      Instant createTime,
      Instant startTime,
      Instant finishTime) {}

  public record TaskPage(List<SolveTask> items, long total, int pageNum, int pageSize) {}
}
