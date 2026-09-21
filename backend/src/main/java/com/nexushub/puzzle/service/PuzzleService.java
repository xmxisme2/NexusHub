package com.nexushub.puzzle.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.analysis.service.AnalysisService;
import com.nexushub.analysis.api.AnalysisDtos.SolveTask;
import com.nexushub.common.constants.AnalysisConstants;
import com.nexushub.common.constants.PuzzleConstants;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.puzzle.api.PuzzleDtos.*;
import com.nexushub.puzzle.persistence.PuzzleMapper;
import com.nexushub.puzzle.persistence.PuzzleRow;
import com.nexushub.rules.domain.HandValidationResult;
import com.nexushub.rules.domain.HandValidator;
import com.nexushub.rules.domain.Seat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PuzzleService {
  private final ObjectMapper mapper;
  private final PuzzleMapper puzzles;
  private final AnalysisService analysis;

  public PuzzleService(ObjectMapper mapper, PuzzleMapper puzzles, AnalysisService analysis) {
    this.mapper = mapper;
    this.puzzles = puzzles;
    this.analysis = analysis;
  }

  public Page search(SearchRequest r, UserRow actor) {
    int page = r.pageNum() == null ? 1 : r.pageNum(),
        size = r.pageSize() == null ? 20 : r.pageSize();
    String scope = r.scope() == null ? PuzzleConstants.STATUS_PUBLISHED : r.scope();
    if (!List.of(PuzzleConstants.STATUS_PUBLISHED, "MINE", "MANAGE").contains(scope))
      throw new IllegalArgumentException("题库范围非法");
    if ("MANAGE".equals(scope) && !isAdmin(actor)) throw new SecurityException("需要管理员权限");
    List<Puzzle> all =
        puzzles.search(scope, actor.getId(), r.keyword()).stream()
            .map(row -> toPuzzle(row, actor))
            .toList();
    int from = Math.min((page - 1) * size, all.size());
    return new Page(all.subList(from, Math.min(from + size, all.size())), all.size(), page, size);
  }

  public Puzzle detail(DetailRequest r, UserRow actor) {
    long id = Long.parseLong(r.id());
    Long versionId = r.versionId() == null ? null : Long.parseLong(r.versionId());
    PuzzleRow row = puzzles.find(id, versionId);
    if (row == null) {
      PuzzleRow archived = puzzles.findIncludingDeleted(id, versionId);
      if (archived != null && canManage(actor, archived)) row = archived;
    }
    if (row == null) throw new NoSuchElementException("题目不存在");
    if (row.getDeleted() != null && row.getDeleted() == 1 && !canManage(actor, row))
      throw new NoSuchElementException("题目不存在");
    if (!PuzzleConstants.STATUS_PUBLISHED.equals(row.getStatus()) && !canManage(actor, row))
      throw new NoSuchElementException("题目不存在");
    if (r.versionId() != null
        && !canManage(actor, row)
        && !Objects.equals(row.getVersionId(), row.getPublishedVersionId()))
      throw new NoSuchElementException("题目不存在");
    return toPuzzle(row, actor);
  }

  /** 公开已发布残局的严格证明材料；只返回 PROVEN 结果中的已验证代表分支。 */
  public SolveTask proofStrategy(String versionId, UserRow actor) {
    long parsedVersionId = Long.parseLong(versionId);
    PuzzleRow row = puzzles.findByVersionId(parsedVersionId);
    if (row == null
        || row.getDeleted() != null && row.getDeleted() == 1
        || (!PuzzleConstants.STATUS_PUBLISHED.equals(row.getStatus())
            && !canManage(actor, row))) {
      throw new NoSuchElementException("题目版本不存在或不可访问");
    }
    return analysis.proofStrategy(parsedVersionId, actor);
  }

  public ValidationResult validate(JsonNode state) {
    List<FieldError> errors = new ArrayList<>();
    if (state == null
        || !state.has("hands")
        || !state.get("hands").isArray()
        || state.get("hands").size() != 2) {
      errors.add(new FieldError("state.hands", "INVALID_HANDS", "必须提供两名玩家手牌"));
      return new ValidationResult(false, errors, null, null);
    }
    if (state.get("hands").get(0).size() != 15 || state.get("hands").get(1).size() != 15) {
      errors.add(new FieldError("state.hands", "INVALID_HAND_LENGTH", "每位玩家必须提供15个牌点计数"));
      return new ValidationResult(false, errors, null, null);
    }
    HandValidationResult result =
        HandValidator.validate(
            counts(state.get("hands").get(0)),
            counts(state.get("hands").get(1)),
            state.path("firstSeat").asInt() == 1 ? Seat.BOT : Seat.USER);
    if (!result.valid()) errors.add(new FieldError("state", "INVALID_STATE", result.message()));
    return new ValidationResult(
        errors.isEmpty(),
        errors,
        errors.isEmpty() ? state : null,
        errors.isEmpty() ? hash(state) : null);
  }

  public DuplicateCheck duplicateCheck(DuplicateCheckRequest r) {
    ValidationResult vr = validate(r.state());
    if (!vr.valid()) throw new IllegalArgumentException("残局状态校验失败");
    PuzzleRow duplicate =
        puzzles.findDuplicate(r.state().toString(), parseOptionalId(r.excludePuzzleId()));
    return new DuplicateCheck(
        duplicate != null, duplicate == null ? null : duplicatePuzzle(duplicate));
  }

  @Transactional
  public Puzzle save(SaveRequest r, UserRow actor) {
    ValidationResult vr = validate(r.state());
    if (!vr.valid()) throw new IllegalArgumentException("残局状态校验失败");
    Long exclude = r.id() == null ? null : Long.parseLong(r.id());
    PuzzleRow duplicate = puzzles.findDuplicate(r.state().toString(), exclude);
    if (duplicate != null) throw new IllegalStateException(duplicateMessage(duplicate));
    long id = r.id() == null ? puzzles.nextPuzzleId() : Long.parseLong(r.id());
    PuzzleRow old = r.id() == null ? null : puzzles.find(id, null);
    if (r.id() != null && old == null) {
      PuzzleRow archived = puzzles.findIncludingDeleted(id, null);
      if (archived != null && archived.getDeleted() != null && archived.getDeleted() == 1)
        throw new IllegalStateException("已删除残局不可直接编辑，请先恢复后再修改");
    }
    if (old != null && !canManage(actor, old)) throw new NoSuchElementException("题目不存在");
    if (old != null
        && (r.expectedRowVersion() == null
            || !Objects.equals(old.getRowVersion(), r.expectedRowVersion())))
      throw new IllegalStateException("版本冲突");
    int versionNo = old == null ? 1 : old.getVersionNo() + 1;
    long versionId = puzzles.nextVersionId();
    if (old == null) puzzles.insertPuzzle(id, actor.getId());
    PuzzleRow row = new PuzzleRow();
    row.setId(id);
    row.setVersionId(versionId);
    row.setVersionNo(versionNo);
    row.setTitle(r.title());
    row.setDescription(r.description());
    row.setTagsJson(json(r.tags() == null ? List.of() : r.tags()));
    row.setDifficulty(r.difficulty());
    row.setAllowedFirstSeatsJson(json(r.allowedFirstSeats()));
    row.setInitialStateJson(r.state().toString());
    row.setStateHash(vr.stateHash());
    puzzles.insertVersion(row);
    if (old == null) {
      if (puzzles.updateDraft(id, versionId, 0) != 1) throw new IllegalStateException("保存题目失败");
    } else if (puzzles.updateDraft(id, versionId, r.expectedRowVersion()) != 1)
      throw new IllegalStateException("版本冲突");
    return toPuzzle(puzzles.find(id, versionId), actor);
  }

  @Transactional
  public Puzzle archive(ArchiveRequest r, UserRow actor) {
    if (!isAdmin(actor)) throw new SecurityException("需要管理员权限");
    long id = Long.parseLong(r.id());
    PuzzleRow current = puzzles.find(id, null);
    if (current == null) throw new NoSuchElementException("题目不存在");
    if (puzzles.archive(id, r.expectedRowVersion()) != 1)
      throw new IllegalStateException("题目版本冲突或已删除");
    return toPuzzle(puzzles.findIncludingDeleted(id, current.getVersionId()), actor);
  }

  @Transactional
  public Puzzle restore(ArchiveRequest r, UserRow actor) {
    if (!isAdmin(actor)) throw new SecurityException("需要管理员权限");
    long id = Long.parseLong(r.id());
    PuzzleRow current = puzzles.findIncludingDeleted(id, null);
    if (current == null || current.getDeleted() == null || current.getDeleted() != 1)
      throw new NoSuchElementException("已删除题目不存在");
    if (puzzles.restore(id, r.expectedRowVersion()) != 1)
      throw new IllegalStateException("题目版本冲突或未删除");
    return toPuzzle(puzzles.find(id, null), actor);
  }

  @Transactional
  public Puzzle publish(PublishRequest r, UserRow actor) {
    if (!isAdmin(actor)) throw new SecurityException("需要管理员权限");
    long id = Long.parseLong(r.id()), versionId = Long.parseLong(r.versionId());
    PuzzleRow current = puzzles.find(id, versionId);
    if (current == null) throw new NoSuchElementException("题目或版本不存在");
    if (puzzles.publish(id, versionId, r.expectedRowVersion()) != 1)
      throw new IllegalStateException("版本冲突");
    PuzzleRow published = puzzles.find(id, versionId);
    analysis.enqueuePuzzleVersion(published);
    return toPuzzle(published, actor);
  }

  private Puzzle toPuzzle(PuzzleRow r) {
    return toPuzzle(r, null);
  }

  private Puzzle toPuzzle(PuzzleRow r, UserRow actor) {
    if (r == null) throw new NoSuchElementException("题目不存在");
    return new Puzzle(
        String.valueOf(r.getId()),
        String.valueOf(r.getOwnerId()),
        r.getStatus(),
        r.getRowVersion(),
        r.getLatestVersionId() == null ? null : String.valueOf(r.getLatestVersionId()),
        r.getPublishedVersionId() == null ? null : String.valueOf(r.getPublishedVersionId()),
        new Version(
            String.valueOf(r.getVersionId()),
            r.getVersionNo(),
            r.getTitle(),
            r.getDescription(),
            readList(r.getTagsJson()),
            r.getDifficulty(),
            readIntList(r.getAllowedFirstSeatsJson()),
            readTree(r.getInitialStateJson()),
            r.getStateHash()),
        actions(r, actor),
        r.getWinnerSide() == null
            ? AnalysisConstants.PROOF_UNKNOWN
            : AnalysisConstants.PROOF_PROVEN,
        r.getWinnerSide());
  }

  private List<String> actions(PuzzleRow row, UserRow actor) {
    if (row.getDeleted() != null && row.getDeleted() == 1)
      return isAdmin(actor) ? List.of("RESTORE") : List.of();
    if (PuzzleConstants.STATUS_PUBLISHED.equals(row.getStatus()))
      return isAdmin(actor)
          ? List.of("COPY", "ARCHIVE", "PLAY", "ANALYZE")
          : List.of("COPY", "PLAY", "ANALYZE");
    List<String> actions = new ArrayList<>(List.of("EDIT", "COPY", "PUBLISH", "PLAY", "ANALYZE"));
    if (isAdmin(actor)) actions.add("ARCHIVE");
    return List.copyOf(actions);
  }

  private DuplicatePuzzle duplicatePuzzle(PuzzleRow row) {
    return new DuplicatePuzzle(
        String.valueOf(row.getId()),
        String.valueOf(row.getVersionId()),
        row.getTitle(),
        row.getStatus(),
        row.getDeleted() != null && row.getDeleted() == 1,
        row.getStateHash());
  }

  private String duplicateMessage(PuzzleRow row) {
    return row.getDeleted() != null && row.getDeleted() == 1
        ? "已有相同的已删除残局：" + row.getTitle() + "（ID " + row.getId() + "，可复用原验证结果）"
        : "已有相同残局：" + row.getTitle() + "（ID " + row.getId() + "）";
  }

  private Long parseOptionalId(String value) {
    return value == null || value.isBlank() ? null : Long.parseLong(value);
  }

  private int[] counts(JsonNode n) {
    int[] a = new int[15];
    for (int i = 0; i < 15; i++) a[i] = n.get(i).asInt(-1);
    return a;
  }

  private String json(Object v) {
    try {
      return mapper.writeValueAsString(v);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private List<String> readList(String v) {
    try {
      return mapper.readValue(
          v, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (Exception e) {
      return List.of();
    }
  }

  private List<Integer> readIntList(String v) {
    try {
      return mapper.readValue(
          v, mapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
    } catch (Exception e) {
      return List.of();
    }
  }

  private JsonNode readTree(String v) {
    try {
      return mapper.readTree(v);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String hash(JsonNode n) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(n.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private boolean isAdmin(UserRow actor) {
    return actor != null && "ADMIN".equals(actor.getRole());
  }

  private boolean canManage(UserRow actor, PuzzleRow row) {
    return isAdmin(actor) || Objects.equals(actor.getId(), row.getOwnerId());
  }
}
