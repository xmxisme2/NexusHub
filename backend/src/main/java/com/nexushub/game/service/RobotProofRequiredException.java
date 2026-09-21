package com.nexushub.game.service;

/** 严格最优模式预算不足时暂停机器人，避免悄悄退回启发式策略。 */
public class RobotProofRequiredException extends RuntimeException {
  public RobotProofRequiredException(String message) {
    super(message);
  }
}
