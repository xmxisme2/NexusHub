package com.nexushub.identity.persistence;

/** 数据库账号行，密码散列仅在认证服务内部使用，绝不进入接口响应。 */
public class UserRow {
  private Long id;
  private String username, displayName, passwordHash, role, status;
  private Integer rowVersion, credentialVersion;

  public Long getId() {
    return id;
  }

  public void setId(Long value) {
    id = value;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String value) {
    username = value;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String value) {
    displayName = value;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String value) {
    passwordHash = value;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String value) {
    role = value;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String value) {
    status = value;
  }

  public Integer getRowVersion() {
    return rowVersion;
  }

  public void setRowVersion(Integer value) {
    rowVersion = value;
  }

  public Integer getCredentialVersion() {
    return credentialVersion;
  }

  public void setCredentialVersion(Integer value) {
    credentialVersion = value;
  }
}
