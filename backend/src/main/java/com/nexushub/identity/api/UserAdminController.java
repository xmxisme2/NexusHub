package com.nexushub.identity.api;

import com.nexushub.common.web.ApiResponse;
import com.nexushub.identity.api.IdentityDtos.*;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.identity.service.IdentityService;
import com.nexushub.identity.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** 管理员账号管理；服务端同时校验 Cookie 会话、CSRF 和固定管理员角色。 */
@RestController
@RequestMapping("/api/admin/users")
@Validated
public class UserAdminController {
  private final SessionService sessions;
  private final IdentityService identities;

  public UserAdminController(SessionService sessions, IdentityService identities) {
    this.sessions = sessions;
    this.identities = identities;
  }

  @PostMapping("/search")
  public ApiResponse<Page> search(
      @Valid @RequestBody UserSearchRequest body, HttpServletRequest request) {
    admin(request);
    return ApiResponse.ok(identities.search(body));
  }

  @PostMapping("/create")
  public ApiResponse<User> create(
      @Valid @RequestBody CreateUserRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String ignored,
      HttpServletRequest request) {
    admin(request);
    return ApiResponse.ok(identities.create(body));
  }

  @PostMapping("/status")
  public ApiResponse<User> status(
      @Valid @RequestBody UserStatusRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String ignored,
      HttpServletRequest request) {
    UserRow user = admin(request);
    return ApiResponse.ok(identities.changeStatus(user, body));
  }

  @PostMapping("/reset-password")
  public ApiResponse<User> resetPassword(
      @Valid @RequestBody ResetPasswordRequest body,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$") String ignored,
      HttpServletRequest request) {
    UserRow user = admin(request);
    return ApiResponse.ok(identities.resetPassword(user, body));
  }

  private UserRow admin(HttpServletRequest request) {
    sessions.requireCsrf(request);
    UserRow user = sessions.requireUser(request);
    identities.requireAdmin(user);
    return user;
  }
}
