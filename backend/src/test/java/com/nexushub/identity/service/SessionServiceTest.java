package com.nexushub.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nexushub.identity.persistence.UserMapper;
import com.nexushub.identity.persistence.UserRow;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
  @Test
  void sessionIsInvalidAfterCredentialVersionChanges() {
    UserMapper users = mock(UserMapper.class);
    SessionService service = new SessionService(users);
    UserRow row = user(1L, 0);
    when(users.findById(1L)).thenReturn(row);
    String token = service.create(row).getValue();
    HttpServletRequest request = request(SessionService.SESSION_COOKIE, token);
    assertEquals(1L, service.requireUser(request).getId());
    row.setCredentialVersion(1);
    assertThrows(UnauthenticatedException.class, () -> service.requireUser(request));
  }

  @Test
  void csrfRequiresMatchingCookieAndHeader() {
    UserMapper users = mock(UserMapper.class);
    SessionService service = new SessionService(users);
    HttpServletRequest request = request(SessionService.CSRF_COOKIE, "token");
    when(request.getHeader("X-CSRF-TOKEN")).thenReturn("token");
    assertDoesNotThrow(() -> service.requireCsrf(request));
  }

  private UserRow user(Long id, int version) {
    UserRow row = new UserRow();
    row.setId(id);
    row.setStatus("ENABLED");
    row.setCredentialVersion(version);
    return row;
  }

  private HttpServletRequest request(String name, String value) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getCookies()).thenReturn(new Cookie[] {new Cookie(name, value)});
    return request;
  }
}
