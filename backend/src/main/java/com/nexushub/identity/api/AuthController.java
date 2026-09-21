package com.nexushub.identity.api;

import com.nexushub.common.web.ApiResponse;
import com.nexushub.identity.api.IdentityDtos.Csrf;
import com.nexushub.identity.api.IdentityDtos.LoginRequest;
import com.nexushub.identity.api.IdentityDtos.User;
import com.nexushub.identity.persistence.UserMapper;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.identity.service.IdentityService;
import com.nexushub.identity.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Cookie 会话入口；登录前后均需先获取 CSRF token。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final SessionService sessions;
  private final IdentityService identities;
  private final UserMapper users;

  public AuthController(SessionService sessions, IdentityService identities, UserMapper users) {
    this.sessions = sessions;
    this.identities = identities;
    this.users = users;
  }

  @GetMapping("/csrf")
  public ResponseEntity<ApiResponse<Csrf>> csrf(HttpServletRequest request) {
    String token = sessions.csrf(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessions.csrfCookie(token).toString())
        .body(ApiResponse.ok(new Csrf(token, "X-CSRF-TOKEN")));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<User>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    sessions.requireCsrf(servletRequest);
    User user = identities.login(request);
    UserRow row = users.findById(Long.parseLong(user.id()));
    String csrf = sessions.csrf(servletRequest);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessions.create(row).toString())
        .header(HttpHeaders.SET_COOKIE, sessions.csrfCookie(csrf).toString())
        .body(ApiResponse.ok(user));
  }

  @PostMapping("/me")
  public ApiResponse<User> me(
      @RequestBody(required = false) Map<String, Object> ignored, HttpServletRequest request) {
    sessions.requireCsrf(request);
    return ApiResponse.ok(identities.current(sessions.requireUser(request)));
  }

  @PostMapping("/guest")
  public ResponseEntity<ApiResponse<User>> guest(HttpServletRequest request) {
    sessions.requireCsrf(request);
    UserRow row = users.findById(SessionService.GUEST_USER_ID);
    String csrf = sessions.csrf(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessions.createGuest().toString())
        .header(HttpHeaders.SET_COOKIE, sessions.csrfCookie(csrf).toString())
        .body(ApiResponse.ok(identities.current(row)));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Map<String, Object>>> logout(
      @RequestBody(required = false) Map<String, Object> ignored, HttpServletRequest request) {
    sessions.requireCsrf(request);
    sessions.requireUser(request);
    sessions.logout(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessions.clearSessionCookie().toString())
        .body(ApiResponse.ok(Map.of()));
  }
}
