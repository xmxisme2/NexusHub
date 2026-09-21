package com.nexushub.rules.domain;

public record HandValidationResult(boolean valid, String message) {
  public static HandValidationResult ok() {
    return new HandValidationResult(true, "");
  }

  public static HandValidationResult invalid(String message) {
    return new HandValidationResult(false, message);
  }
}
