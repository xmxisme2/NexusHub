package com.nexushub.analysis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.analysis.api.AnalysisDtos.*;
import com.nexushub.analysis.persistence.SolveResultRow;
import com.nexushub.analysis.persistence.SolveTaskMapper;
import com.nexushub.analysis.persistence.SolveTaskRow;
import com.nexushub.analysis.solver.MinimaxSolver;
import com.nexushub.common.constants.AnalysisConstants;
import com.nexushub.common.constants.PuzzleConstants;
import com.nexushub.common.constants.RulesConstants;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.puzzle.persistence.PuzzleMapper;
import com.nexushub.puzzle.persistence.PuzzleRow;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.Move;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 求解任务编排、已有题目入队和结果映射；搜索本身由纯 Java MinimaxSolver 执行。 */
@Service
public class AnalysisService {
  /** 对外暴露的当前求解器版本；参与任务幂等和缓存键。 */
  public static final String SOLVER_VERSION = AnalysisConstants.SOLVER_VERSION;

  private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
  private final SolveTaskMapper tasks;
  private final PuzzleMapper puzzles;
  private final ObjectMapper json;

  public AnalysisService(SolveTaskMapper tasks, PuzzleMapper puzzles, ObjectMapper json) {
    this.tasks = tasks;
    this.puzzles = puzzles;
    this.json = json;
  }

  /** 启动时为所有已发布版本创建一次证明任务；已有同版本任务不会重复入队。 */
  @EventListener(ApplicationReadyEvent.class)
  public void enqueuePublishedPuzzlesOnReady() {
    try {
      enqueuePublishedPuzzles();
    } catch (Exception e) {
      log.error("初始化残局证明任务失败", e);
    }
  }

  @Transactional
  public int enqueuePublishedPuzzles() {
    int created = 0;
    for (PuzzleRow puzzle : puzzles.listPublishedVersions()) {
      if (enqueuePuzzleVersion(puzzle)) created++;
    }
    return created;
  }

  /** 发布动作完成后立即补建对应证明任务；幂等依赖版本与 solverVersion。 */
  @Transactional
  public boolean enqueuePuzzleVersion(PuzzleRow puzzle) {
    if (puzzle == null
        || puzzle.getVersionId() == null
        || !PuzzleConstants.STATUS_PUBLISHED.equals(puzzle.getStatus())
        || tasks.findByPuzzleVersion(puzzle.getVersionId(), SOLVER_VERSION) != null) return false;
    SolveTaskRow row =
        newTask(
            puzzle.getOwnerId(),
            AnalysisConstants.SOURCE_PUZZLE_VERSION,
            puzzle.getVersionId(),
            puzzle.getInitialStateJson(),
            puzzle.getStateHash(),
            30000,
            1_000_000L,
            64);
    tasks.insertTask(row);
    return true;
  }

  @Transactional
  public SolveTask create(SolveCreateRequest request, UserRow actor) {
    JsonNode source = request.source();
    if (source == null || !source.has("type")) {
      throw new IllegalArgumentException("求解来源不能为空");
    }
    String type = source.path("type").asText();
    PuzzleRow puzzle = null;
    String snapshot;
    String hash;
    Long puzzleVersionId = null;
    if (AnalysisConstants.SOURCE_PUZZLE_VERSION.equals(type)) {
      puzzleVersionId = Long.parseLong(source.path("puzzleVersionId").asText());
      puzzle = puzzles.findByVersionId(puzzleVersionId);
      if (puzzle == null
          || (!PuzzleConstants.STATUS_PUBLISHED.equals(puzzle.getStatus())
              && !actor.getId().equals(puzzle.getOwnerId())))
        throw new NoSuchElementException("题目版本不存在或不可访问");
      snapshot = puzzle.getInitialStateJson();
      hash = puzzle.getStateHash();
    } else if (AnalysisConstants.SOURCE_CUSTOM.equals(type)) {
      snapshot = source.path("state").toString();
      validateState(snapshot);
      hash = hash(snapshot);
    } else {
      throw new IllegalArgumentException("当前仅支持 PUZZLE_VERSION 或 CUSTOM 求解来源");
    }
    Budget b = request.budget() == null ? new Budget(30000, 1_000_000L, 64) : request.budget();
    String sourceType =
        AnalysisConstants.SOURCE_PUZZLE_VERSION.equals(type)
            ? AnalysisConstants.SOURCE_PUZZLE_VERSION
            : AnalysisConstants.SOURCE_CUSTOM;
    SolveTaskRow row =
        newTask(
            actor.getId(),
            sourceType,
            puzzleVersionId,
            snapshot,
            hash,
            b.timeLimitMs(),
            b.nodeLimit(),
            b.memoryLimitMb());
    tasks.insertTask(row);
    return toTask(tasks.findTask(row.getId()));
  }

  public TaskPage search(TaskSearchRequest request, UserRow actor) {
    int page = request.pageNum() == null ? 1 : request.pageNum(),
        size = request.pageSize() == null ? 20 : request.pageSize();
    List<SolveTask> items =
        tasks.search(actor.getId(), request.taskStatus(), (page - 1) * size, size).stream()
            .map(this::toTask)
            .toList();
    return new TaskPage(items, tasks.count(actor.getId(), request.taskStatus()), page, size);
  }

  public SolveTask detail(String id, UserRow actor) {
    SolveTaskRow row = tasks.findTask(Long.parseLong(id));
    if (row == null || !actor.getId().equals(row.getOwnerId()))
      throw new NoSuchElementException("分析任务不存在");
    return toTask(row);
  }

  /** 题库详情使用的公开策略读取；权限由题目发布状态和调用方范围先行校验。 */
  public SolveTask proofStrategy(long puzzleVersionId, UserRow actor) {
    SolveTaskRow row = tasks.findByPuzzleVersion(puzzleVersionId, SOLVER_VERSION);
    if (row == null) throw new NoSuchElementException("该残局尚未生成证明策略");
    SolveTask task = toTask(row);
    if (task.result() == null
        || !AnalysisConstants.PROOF_PROVEN.equals(task.result().proofStatus())
        || !task.result().winningLines().isEmpty()) return task;
    // 旧版本只保存了根结论时，按同一快照和预算即时补出可读的代表路线。
    try {
      GameState state = json.readValue(row.getStateSnapshot(), GameState.class);
      MinimaxSolver.Outcome outcome =
          new MinimaxSolver()
              .solve(state, new MinimaxSolver.Budget(row.getTimeLimitMs(), row.getNodeLimit()));
      if (!AnalysisConstants.PROOF_PROVEN.equals(outcome.proofStatus())
          || outcome.winningLines().isEmpty()) return task;
      SolveResult old = task.result();
      SolveResult enriched =
          new SolveResult(
              old.id(),
              old.proofStatus(),
              old.winnerSide(),
              old.proofValue(),
              old.strategyStatus(),
              old.rootNodeId(),
              outcome.recommendedMove(),
              toWinningLines(outcome.winningLines()),
              old.solverVersion(),
              old.rulesetVersion(),
              old.stats());
      return new SolveTask(
          task.id(),
          task.taskStatus(),
          task.purpose(),
          task.effectiveBudget(),
          task.stateHash(),
          enriched,
          task.terminationReason(),
          task.stats(),
          task.createTime(),
          task.startTime(),
          task.finishTime());
    } catch (Exception ignored) {
      return task;
    }
  }

  @Transactional
  public SolveTask cancel(String id, UserRow actor) {
    if (tasks.cancel(Long.parseLong(id), actor.getId()) != 1)
      throw new IllegalStateException("任务不存在或已结束");
    return detail(id, actor);
  }

  /** Worker 以租约令牌完成结果；结果和任务终态在同一事务中提交。 */
  @Transactional
  public void execute(SolveTaskRow task, String leaseToken) {
    try {
      GameState state = json.readValue(task.getStateSnapshot(), GameState.class);
      MinimaxSolver.Outcome outcome =
          new MinimaxSolver()
              .solve(state, new MinimaxSolver.Budget(task.getTimeLimitMs(), task.getNodeLimit()));
      String stats =
          write(
              Map.of(
                  "elapsedMs",
                  outcome.elapsedMs(),
                  "queueTimeMs",
                  0,
                  "visitedNodes",
                  outcome.visitedNodes(),
                  "cacheHits",
                  outcome.cacheHits()));
      SolveResultRow result = new SolveResultRow();
      result.setId(task.getId() + 1_000_000L);
      result.setTaskId(task.getId());
      result.setProofStatus(outcome.proofStatus());
      result.setWinnerSide(outcome.winner() == null ? null : outcome.winner().name());
      result.setProofValue(outcome.proofValue());
      result.setStrategyStatus(outcome.proofStatus().equals("PROVEN") ? "PARTIAL" : "NONE");
      result.setWinningLinesJson(writeLines(outcome.winningLines()));
      result.setStatsJson(stats);
      result.setVerifiedTime(outcome.proofStatus().equals("PROVEN") ? Instant.now() : null);
      tasks.insertResult(result);
      tasks.complete(task.getId(), leaseToken, outcome.terminationReason(), stats);
    } catch (Exception e) {
      tasks.fail(
          task.getId(),
          leaseToken,
          "SOLVER_ERROR",
          write(Map.of("elapsedMs", 0, "queueTimeMs", 0, "visitedNodes", 0, "cacheHits", 0)));
      log.warn("求解任务 {} 失败", task.getId(), e);
    }
  }

  private SolveTaskRow newTask(
      long ownerId,
      String sourceType,
      Long versionId,
      String snapshot,
      String stateHash,
      int time,
      long nodes,
      int memory) {
    SolveTaskRow row = new SolveTaskRow();
    row.setId(tasks.nextTaskId());
    row.setOwnerId(ownerId);
    row.setPurpose(AnalysisConstants.PURPOSE_ANALYSIS);
    row.setSourceType(sourceType);
    row.setPuzzleVersionId(versionId);
    row.setStateSnapshot(snapshot);
    row.setStateHash(stateHash);
    row.setRulesetVersion(RulesConstants.CLASSIC_V1);
    row.setSolverVersion(SOLVER_VERSION);
    row.setTimeLimitMs(time);
    row.setNodeLimit(nodes);
    row.setMemoryLimitMb(memory);
    row.setStatsJson("{\"elapsedMs\":0,\"queueTimeMs\":0,\"visitedNodes\":0,\"cacheHits\":0}");
    return row;
  }

  private void validateState(String value) {
    try {
      GameState state = json.readValue(value, GameState.class);
      com.nexushub.rules.domain.GameRules.initial(state.hands(), state.firstSeat());
    } catch (Exception e) {
      throw new IllegalArgumentException("自定义局面非法", e);
    }
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String write(Object v) {
    try {
      return json.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String writeLines(List<List<com.nexushub.rules.domain.Move>> lines) {
    List<WinningLine> out = new ArrayList<>();
    int i = 1;
    for (List<com.nexushub.rules.domain.Move> line : lines) {
      out.add(new WinningLine(i++, line, true));
      if (out.size() == 5) break;
    }
    return write(out);
  }

  private List<WinningLine> toWinningLines(List<List<Move>> lines) {
    List<WinningLine> out = new ArrayList<>();
    int i = 1;
    for (List<Move> line : lines) {
      out.add(new WinningLine(i++, line, true));
      if (out.size() == 5) break;
    }
    return List.copyOf(out);
  }

  private SolveTask toTask(SolveTaskRow row) {
    SolveResultRow r = tasks.findResult(row.getId());
    return new SolveTask(
        String.valueOf(row.getId()),
        row.getTaskStatus(),
        row.getPurpose(),
        new EffectiveBudget(row.getTimeLimitMs(), row.getNodeLimit(), row.getMemoryLimitMb()),
        row.getStateHash(),
        r == null ? null : toResult(r, row),
        row.getTerminationReason(),
        stats(row.getStatsJson()),
        row.getCreateTime(),
        row.getStartTime(),
        row.getFinishTime());
  }

  private SolveResult toResult(SolveResultRow r, SolveTaskRow task) {
    try {
      List<WinningLine> lines = json.readValue(r.getWinningLinesJson(), new TypeReference<>() {});
      Move move =
          lines.isEmpty() || lines.get(0).moves().isEmpty() ? null : lines.get(0).moves().get(0);
      return new SolveResult(
          String.valueOf(r.getId()),
          r.getProofStatus(),
          r.getWinnerSide(),
          r.getProofValue(),
          r.getStrategyStatus(),
          r.getRootNodeId() == null ? null : String.valueOf(r.getRootNodeId()),
          move,
          lines,
          task.getSolverVersion(),
          task.getRulesetVersion(),
          stats(r.getStatsJson()));
    } catch (Exception e) {
      throw new IllegalStateException("分析结果损坏", e);
    }
  }

  private SolveStats stats(String raw) {
    try {
      return json.readValue(raw, new TypeReference<>() {});
    } catch (Exception e) {
      return new SolveStats(0, 0, 0, 0);
    }
  }
}
