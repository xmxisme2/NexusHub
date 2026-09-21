package com.nexushub.puzzle.api;

import com.nexushub.common.web.ApiResponse;
import com.nexushub.analysis.api.AnalysisDtos.SolveTask;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.identity.service.SessionService;
import com.nexushub.puzzle.api.PuzzleDtos.*;
import com.nexushub.puzzle.service.PuzzleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/puzzles")
public class PuzzleController {
  private final PuzzleService service;
  private final SessionService sessions;

  public PuzzleController(PuzzleService service, SessionService sessions) {
    this.service = service;
    this.sessions = sessions;
  }

  @PostMapping("/search")
  public ApiResponse<Page> search(
      @Valid @RequestBody SearchRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.search(r, readUser(request)));
  }

  @PostMapping("/detail")
  public ApiResponse<Puzzle> detail(
      @Valid @RequestBody DetailRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.detail(r, readUser(request)));
  }

  @PostMapping("/proof-strategy")
  public ApiResponse<SolveTask> proofStrategy(
      @Valid @RequestBody ProofStrategyRequest r,
      jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.proofStrategy(r.versionId(), readUser(request)));
  }

  @PostMapping("/validate")
  public ApiResponse<ValidationResult> validate(
      @Valid @RequestBody ValidateRequest r, jakarta.servlet.http.HttpServletRequest request) {
    user(request);
    return ApiResponse.ok(service.validate(r.state()));
  }

  @PostMapping("/duplicate-check")
  public ApiResponse<DuplicateCheck> duplicateCheck(
      @Valid @RequestBody DuplicateCheckRequest r,
      jakarta.servlet.http.HttpServletRequest request) {
    user(request);
    return ApiResponse.ok(service.duplicateCheck(r));
  }

  @PostMapping("/save")
  public ApiResponse<Puzzle> save(
      @Valid @RequestBody SaveRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.save(r, user(request)));
  }

  @PostMapping("/publish")
  public ApiResponse<Puzzle> publish(
      @Valid @RequestBody PublishRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.publish(r, user(request)));
  }

  @PostMapping({"/archive", "/delete"})
  public ApiResponse<Puzzle> archive(
      @Valid @RequestBody ArchiveRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.archive(r, user(request)));
  }

  @PostMapping("/restore")
  public ApiResponse<Puzzle> restore(
      @Valid @RequestBody ArchiveRequest r, jakarta.servlet.http.HttpServletRequest request) {
    return ApiResponse.ok(service.restore(r, user(request)));
  }

  private UserRow user(jakarta.servlet.http.HttpServletRequest request) {
    sessions.requireCsrf(request);
    return sessions.requireUser(request);
  }

  private UserRow readUser(jakarta.servlet.http.HttpServletRequest request) {
    sessions.requireCsrf(request);
    return sessions.requireUserOrGuest(request);
  }
}
