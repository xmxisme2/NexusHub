package com.nexushub.identity.service;

/** 会话缺失、过期或已因凭据变更撤销。 */
public final class UnauthenticatedException extends RuntimeException {
  public UnauthenticatedException() {
    super("登录已失效，请重新登录");
  }

  public UnauthenticatedException(String message) {
    super(message);
  }
}
