package com.nexushub.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nexushub.identity.api.IdentityDtos.CreateUserRequest;
import com.nexushub.identity.persistence.UserMapper;
import com.nexushub.identity.persistence.UserRow;
import org.junit.jupiter.api.Test;

class IdentityServiceTest {
  @Test
  void newAccountUsesBcryptAndNormalizedUsername() {
    UserMapper mapper = mock(UserMapper.class);
    when(mapper.findByUsername("player.01")).thenReturn(null);
    when(mapper.nextId()).thenReturn(9L);
    UserRow stored = new UserRow();
    stored.setId(9L);
    stored.setUsername("player.01");
    stored.setDisplayName("玩家");
    stored.setRole("USER");
    stored.setStatus("ENABLED");
    stored.setRowVersion(0);
    when(mapper.findById(9L)).thenReturn(stored);
    IdentityService service = new IdentityService(mapper);
    assertEquals(
        "player.01",
        service
            .create(new CreateUserRequest(" Player.01 ", "玩家", "strong-password", "USER"))
            .username());
    verify(mapper)
        .insert(
            argThat(
                row ->
                    row.getPasswordHash().startsWith("$2a$")
                        || row.getPasswordHash().startsWith("$2b$")));
  }
}
