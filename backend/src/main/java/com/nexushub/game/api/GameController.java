package com.nexushub.game.api;

import com.nexushub.common.web.ApiResponse;
import com.nexushub.game.api.GameDtos.*;
import com.nexushub.game.service.GameService;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.identity.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 最小对局 HTTP 入口；客户端只提交用户意图，状态和胜负由服务端规则层决定。 */
@RestController
@Validated
@RequestMapping("/api/games")
public class GameController {
  private final GameService service;
  private final SessionService sessions;

  public GameController(GameService service, SessionService sessions) {
    this.service = service;
    this.sessions = sessions;
  }

  @PostMapping("/create")
  public ApiResponse<Game> create(
      @Valid @RequestBody GameCreateRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String ignored,
      HttpServletRequest request) {
    return ApiResponse.ok(service.create(body, user(request)));
  }

  @PostMapping("/detail")
  public ApiResponse<Game> detail(@Valid @RequestBody IdRequest body, HttpServletRequest request) {
    return ApiResponse.ok(service.detail(body, user(request)));
  }

  @PostMapping("/replay/detail")
  public ApiResponse<Replay> replay(
      @Valid @RequestBody ReplayRequest body, HttpServletRequest request) {
    return ApiResponse.ok(service.replay(body, user(request)));
  }

  @PostMapping("/legal-moves/search")
  public ApiResponse<LegalMovePage> legalMoves(
      @Valid @RequestBody LegalMovesRequest body, HttpServletRequest request) {
    return ApiResponse.ok(service.legalMoves(body, user(request)));
  }

  @PostMapping("/hint")
  public ApiResponse<HintResponse> hint(
      @Valid @RequestBody GameVersionRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String key,
      HttpServletRequest request) {
    return ApiResponse.ok(service.hint(body, key, user(request)));
  }

  @PostMapping("/actions/play")
  public ApiResponse<Game> play(
      @Valid @RequestBody PlayRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String key,
      HttpServletRequest request) {
    return ApiResponse.ok(service.play(body, key, user(request)));
  }

  @PostMapping("/actions/pass")
  public ApiResponse<Game> pass(
      @Valid @RequestBody GameVersionRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String key,
      HttpServletRequest request) {
    return ApiResponse.ok(service.pass(body, key, user(request)));
  }

  @PostMapping("/restart")
  public ApiResponse<Game> restart(
      @Valid @RequestBody GameRestartRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String key,
      HttpServletRequest request) {
    return ApiResponse.ok(service.restart(body, key, user(request)));
  }

  @PostMapping("/abandon")
  public ApiResponse<Game> abandon(
      @Valid @RequestBody GameVersionRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String key,
      HttpServletRequest request) {
    return ApiResponse.ok(service.abandon(body, key, user(request)));
  }

  private UserRow user(HttpServletRequest request) {
    sessions.requireCsrf(request);
    return sessions.requireUserOrGuest(request);
  }
}
