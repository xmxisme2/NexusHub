package com.nexushub.identity.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** identity OpenAPI 契约对应的传输对象。 */
public final class IdentityDtos {
  private IdentityDtos() {}

  /** 本地初始化账号由测试种子提供；登录仅校验非空，管理员重置和新建账号仍执行强密码规则。 */
  public record LoginRequest(
      @NotBlank @Size(min = 3, max = 64) String username,
      @NotBlank @Size(max = 64) String password) {}

  public record UserSearchRequest(
      @Min(1) @Max(100000) Integer pageNum,
      @Min(1) @Max(100) Integer pageSize,
      @Size(max = 64) String keyword,
      @Pattern(regexp = "ENABLED|DISABLED", message = "账号状态非法") String status) {}

  public record CreateUserRequest(
      @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9_.-]{2,63}$") String username,
      @NotBlank @Size(max = 64) String displayName,
      @NotBlank @Size(min = 12, max = 64) String password,
      @NotBlank @Pattern(regexp = "USER|ADMIN") String role) {}

  public record UserStatusRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String id,
      @NotNull @Min(0) Integer expectedRowVersion,
      @NotBlank @Pattern(regexp = "ENABLED|DISABLED") String status) {}

  public record ResetPasswordRequest(
      @NotBlank @Pattern(regexp = "^[1-9][0-9]{0,18}$") String id,
      @NotNull @Min(0) Integer expectedRowVersion,
      @NotBlank @Size(min = 12, max = 64) String newPassword) {}

  public record Csrf(String token, String headerName) {}

  public record User(
      String id,
      String username,
      String displayName,
      String role,
      String status,
      int rowVersion,
      List<String> permissions) {}

  public record Page(List<User> items, long total, int pageNum, int pageSize) {}
}
