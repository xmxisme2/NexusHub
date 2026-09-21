package com.nexushub.identity.service;

import com.nexushub.identity.api.IdentityDtos.*;
import com.nexushub.identity.persistence.UserMapper;
import com.nexushub.identity.persistence.UserRow;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 账号认证、管理员账号管理及固定角色权限映射。 */
@Service
public class IdentityService {
  private static final List<String> USER_PERMISSIONS =
      List.of("hub:read", "puzzle:read", "puzzle:write", "game:play", "analysis:run");
  private static final List<String> ADMIN_EXTRA =
      List.of("hub:manage", "puzzle:publish", "puzzle:archive", "user:manage");
  private static final List<String> GUEST_PERMISSIONS =
      List.of("hub:read", "puzzle:read", "game:play");
  private final UserMapper users;

  /** BCrypt 成本参数 12；登录只做 matches，不会把散列值还原成明文。 */
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

  public IdentityService(UserMapper users) {
    this.users = users;
  }

  public User login(LoginRequest request) {
    UserRow row = users.findByUsername(normalizeUsername(request.username()));
    if (row == null
        || !"ENABLED".equals(row.getStatus())
        || !passwordEncoder.matches(request.password(), row.getPasswordHash()))
      throw new UnauthenticatedException("用户名或密码错误");
    return toUser(row);
  }

  public User current(UserRow row) {
    return toUser(row);
  }

  public Page search(UserSearchRequest request) {
    int page = request.pageNum() == null ? 1 : request.pageNum(),
        size = request.pageSize() == null ? 20 : request.pageSize();
    List<User> all =
        users.search(trim(request.keyword()), request.status()).stream().map(this::toUser).toList();
    int from = Math.min((page - 1) * size, all.size());
    return new Page(all.subList(from, Math.min(from + size, all.size())), all.size(), page, size);
  }

  @Transactional
  public User create(CreateUserRequest request) {
    String username = normalizeUsername(request.username());
    if (users.findByUsername(username) != null) throw new IllegalStateException("用户名已存在");
    checkPassword(request.password());
    UserRow row = new UserRow();
    row.setId(users.nextId());
    row.setUsername(username);
    row.setDisplayName(request.displayName().trim());
    row.setPasswordHash(passwordEncoder.encode(request.password()));
    row.setRole(request.role());
    users.insert(row);
    return toUser(users.findById(row.getId()));
  }

  @Transactional
  public User changeStatus(UserRow operator, UserStatusRequest request) {
    long id = Long.parseLong(request.id());
    UserRow target = mustFind(id);
    if (operator.getId().equals(id) && "DISABLED".equals(request.status()))
      throw new IllegalArgumentException("不能停用当前登录账号");
    if ("ADMIN".equals(target.getRole())
        && "ENABLED".equals(target.getStatus())
        && "DISABLED".equals(request.status())
        && users.countEnabledAdmins() <= 1) throw new IllegalStateException("至少保留一个启用的管理员");
    if (users.updateStatus(id, request.status(), request.expectedRowVersion()) != 1)
      throw new IllegalStateException("账号版本冲突");
    return toUser(mustFind(id));
  }

  @Transactional
  public User resetPassword(UserRow operator, ResetPasswordRequest request) {
    long id = Long.parseLong(request.id());
    mustFind(id);
    checkPassword(request.newPassword());
    if (users.resetPassword(
            id, passwordEncoder.encode(request.newPassword()), request.expectedRowVersion())
        != 1) throw new IllegalStateException("账号版本冲突");
    return toUser(mustFind(id));
  }

  public void requireAdmin(UserRow user) {
    if (!"ADMIN".equals(user.getRole())) throw new SecurityException("需要管理员权限");
  }

  private UserRow mustFind(long id) {
    UserRow row = users.findById(id);
    if (row == null) throw new java.util.NoSuchElementException("账号不存在");
    return row;
  }

  private User toUser(UserRow row) {
    List<String> permissions =
        "GUEST".equals(row.getRole())
            ? GUEST_PERMISSIONS
            : "ADMIN".equals(row.getRole())
                ? concat(USER_PERMISSIONS, ADMIN_EXTRA)
                : USER_PERMISSIONS;
    return new User(
        String.valueOf(row.getId()),
        row.getUsername(),
        row.getDisplayName(),
        row.getRole(),
        row.getStatus(),
        row.getRowVersion(),
        permissions);
  }

  private List<String> concat(List<String> one, List<String> two) {
    java.util.ArrayList<String> list = new java.util.ArrayList<>(one);
    list.addAll(two);
    return List.copyOf(list);
  }

  private String normalizeUsername(String username) {
    return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private void checkPassword(String password) {
    if (password == null
        || password.length() < 12
        || password.length() > 64
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException("密码长度须为12至64字符且UTF-8编码不超过72字节");
  }
}
