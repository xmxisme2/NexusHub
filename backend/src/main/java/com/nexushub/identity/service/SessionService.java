package com.nexushub.identity.service;

import com.nexushub.identity.persistence.UserMapper;
import com.nexushub.identity.persistence.UserRow;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/** 单实例 Cookie 会话。会话中保存凭据版本，账号停用或重置密码时自动失效。 */
@Service
public class SessionService {
  public static final long GUEST_USER_ID = 2L;
  public static final String SESSION_COOKIE = "NEXUSHUB_SESSION";
  public static final String CSRF_COOKIE = "NEXUSHUB_CSRF";
  private static final long SESSION_SECONDS = 30 * 60;
  private final SecureRandom random = new SecureRandom();
  private final Map<String, Session> sessions = new ConcurrentHashMap<>();
  private final UserMapper users;

  @Value("${nexushub.security.cookie-secure:false}")
  private boolean secureCookies;

  public SessionService(UserMapper users) {
    this.users = users;
  }

  public String csrf(HttpServletRequest request) {
    String existing = cookie(request, CSRF_COOKIE);
    return existing == null ? token() : existing;
  }

  public ResponseCookie csrfCookie(String value) {
    return ResponseCookie.from(CSRF_COOKIE, value)
        .path("/")
        .httpOnly(false)
        .secure(secureCookies)
        .sameSite("Lax")
        .maxAge(SESSION_SECONDS)
        .build();
  }

  public void requireCsrf(HttpServletRequest request) {
    String cookie = cookie(request, CSRF_COOKIE);
    String header = request.getHeader("X-CSRF-TOKEN");
    if (cookie == null || header == null || !constantEquals(cookie, header))
      throw new SecurityException("CSRF 校验失败");
  }

  public ResponseCookie create(UserRow user) {
    String id = token();
    sessions.put(
        id,
        new Session(
            user.getId(), user.getCredentialVersion(), Instant.now().plusSeconds(SESSION_SECONDS)));
    return ResponseCookie.from(SESSION_COOKIE, id)
        .path("/")
        .httpOnly(true)
        .secure(secureCookies)
        .sameSite("Lax")
        .maxAge(SESSION_SECONDS)
        .build();
  }

  /** 创建游客会话；游客绑定数据库账号，便于保存对局归属并隔离权限。 */
  public ResponseCookie createGuest() {
    UserRow guest = users.findById(GUEST_USER_ID);
    if (guest == null || !"GUEST".equals(guest.getRole()))
      throw new IllegalStateException("游客账号未初始化");
    String id = token();
    sessions.put(
        id,
        new Session(
            guest.getId(), guest.getCredentialVersion(), Instant.now().plusSeconds(SESSION_SECONDS)));
    return ResponseCookie.from(SESSION_COOKIE, id)
        .path("/")
        .httpOnly(true)
        .secure(secureCookies)
        .sameSite("Lax")
        .maxAge(SESSION_SECONDS)
        .build();
  }

  public UserRow requireUserOrGuest(HttpServletRequest request) {
    try {
      return requireUser(request);
    } catch (UnauthenticatedException ignored) {
      UserRow guest = users.findById(GUEST_USER_ID);
      if (guest == null || !"GUEST".equals(guest.getRole())) throw ignored;
      return guest;
    }
  }

  public UserRow requireUser(HttpServletRequest request) {
    String id = cookie(request, SESSION_COOKIE);
    Session session = id == null ? null : sessions.get(id);
    if (session == null || session.expiresAt().isBefore(Instant.now())) {
      if (id != null) sessions.remove(id);
      throw new UnauthenticatedException();
    }
    UserRow user = users.findById(session.userId());
    if (user == null
        || !"ENABLED".equals(user.getStatus())
        || !session.credentialVersion().equals(user.getCredentialVersion())) {
      sessions.remove(id);
      throw new UnauthenticatedException();
    }
    sessions.put(
        id,
        new Session(
            session.userId(),
            session.credentialVersion(),
            Instant.now().plusSeconds(SESSION_SECONDS)));
    return user;
  }

  public void logout(HttpServletRequest request) {
    String id = cookie(request, SESSION_COOKIE);
    if (id != null) sessions.remove(id);
  }

  public ResponseCookie clearSessionCookie() {
    return ResponseCookie.from(SESSION_COOKIE, "")
        .path("/")
        .httpOnly(true)
        .secure(secureCookies)
        .sameSite("Lax")
        .maxAge(0)
        .build();
  }

  private String cookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return null;
    for (Cookie cookie : cookies) if (name.equals(cookie.getName())) return cookie.getValue();
    return null;
  }

  private String token() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private boolean constantEquals(String left, String right) {
    if (left.length() != right.length()) return false;
    int value = 0;
    for (int i = 0; i < left.length(); i++) value |= left.charAt(i) ^ right.charAt(i);
    return value == 0;
  }

  private record Session(Long userId, Integer credentialVersion, Instant expiresAt) {}
}
