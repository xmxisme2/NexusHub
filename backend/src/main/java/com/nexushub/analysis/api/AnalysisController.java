package com.nexushub.analysis.api;

import com.nexushub.analysis.api.AnalysisDtos.*;
import com.nexushub.analysis.service.AnalysisService;
import com.nexushub.common.web.ApiResponse;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.identity.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** 分析任务入口；搜索在 Worker 中执行，HTTP 只负责排队、查询和取消。 */
@RestController
@RequestMapping("/api/analysis/tasks")
public class AnalysisController {
  private final AnalysisService service;
  private final SessionService sessions;

  public AnalysisController(AnalysisService service, SessionService sessions) {
    this.service = service;
    this.sessions = sessions;
  }

  @PostMapping("/create")
  public ApiResponse<SolveTask> create(
      @Valid @RequestBody SolveCreateRequest body, HttpServletRequest request) {
    return ApiResponse.ok(service.create(body, user(request)));
  }

  @PostMapping("/search")
  public ApiResponse<TaskPage> search(
      @Valid @RequestBody TaskSearchRequest body, HttpServletRequest request) {
    return ApiResponse.ok(service.search(body, user(request)));
  }

  @PostMapping("/detail")
  public ApiResponse<SolveTask> detail(
      @RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
    return ApiResponse.ok(service.detail(body.get("id"), user(request)));
  }

  @PostMapping("/cancel")
  public ApiResponse<SolveTask> cancel(
      @RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
    return ApiResponse.ok(service.cancel(body.get("id"), user(request)));
  }

  private UserRow user(HttpServletRequest request) {
    sessions.requireCsrf(request);
    return sessions.requireUser(request);
  }
}
