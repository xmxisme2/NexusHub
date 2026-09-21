package com.nexushub.rules.domain;

import java.util.List;

/**
 * 机器人 V1 启发式策略：在服务端规则引擎枚举的合法行动中选择排序最靠前的一手， 没有可以压过当前牌墩的行动时选择 PASS。它不修改局面，实际落子仍必须经过 {@link
 * GameRules#apply(GameState, Move)}。
 */
public final class BotStrategy {
  private BotStrategy() {}

  public static Move choose(GameState state) {
    if (state == null || state.currentSeat() != 1) {
      throw new IllegalArgumentException("不是机器人回合");
    }
    List<Move> legalMoves = GameRules.legalMoves(state);
    return legalMoves.isEmpty() ? Move.pass() : legalMoves.get(0);
  }

  /** 面向用户提示的确定性建议。提示与机器人落子共用规则枚举，但不表示证明结果。 当当前牌墩存在且没有可压行动时，PASS 是唯一可行的提示；首手不会返回 PASS。 */
  public static Move chooseHint(GameState state) {
    if (state == null || state.currentSeat() < 0 || state.currentSeat() > 1) {
      throw new IllegalArgumentException("局面或当前座位非法");
    }
    List<Move> legalMoves = GameRules.legalMoves(state);
    if (!legalMoves.isEmpty()) return legalMoves.get(0);
    if (!state.isFirstMove() && state.targetMove() != null) return Move.pass();
    throw new IllegalStateException("当前局面没有可用行动");
  }
}
