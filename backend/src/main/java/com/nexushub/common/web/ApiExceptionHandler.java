package com.nexushub.common.web;

import com.nexushub.identity.service.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ApiError invalid(IllegalArgumentException e) {
    return new ApiError(42200, e.getMessage(), null, "local");
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiError conflict(IllegalStateException e) {
    return new ApiError(40900, e.getMessage(), null, "local");
  }

  @ExceptionHandler(java.util.NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError missing(java.util.NoSuchElementException e) {
    return new ApiError(40400, e.getMessage(), null, "local");
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiError> forbidden(SecurityException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiError(40300, e.getMessage(), null, "local"));
  }

  @ExceptionHandler(UnauthenticatedException.class)
  public ResponseEntity<ApiError> unauthenticated(UnauthenticatedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ApiError(40100, e.getMessage(), null, "local"));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> malformed(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(new ApiError(40000, "请求参数错误", null, "local"));
  }

  public record ApiError(int code, String message, Object data, String requestId) {}
}
