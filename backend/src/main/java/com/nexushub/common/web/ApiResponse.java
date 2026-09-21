package com.nexushub.common.web;

/** 统一接口响应外壳；业务模块不得各自定义响应格式。 */
public record ApiResponse<T>(int code, String message, T data, String requestId) {
  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(200, "success", data, "local");
  }
}
