package com.nexushub.rules.domain;

import java.util.Arrays;

/** 不可变完整局面；hands[0] 固定 USER，hands[1] 固定 BOT。 */
public record GameState(
    int schemaVersion,
    String rulesetVersion,
    int[][] hands,
    int firstSeat,
    int currentSeat,
    boolean isFirstMove,
    Move targetMove,
    Integer lastPlaySeat,
    int consecutivePasses) {
  public GameState {
    if (hands == null
        || hands.length != 2
        || hands[0] == null
        || hands[1] == null
        || hands[0].length != 15
        || hands[1].length != 15) throw new IllegalArgumentException("局面必须包含双方15位手牌");
    hands = new int[][] {Arrays.copyOf(hands[0], 15), Arrays.copyOf(hands[1], 15)};
  }

  @Override
  public int[][] hands() {
    return new int[][] {Arrays.copyOf(hands[0], 15), Arrays.copyOf(hands[1], 15)};
  }

  public int[] hand(int seat) {
    return Arrays.copyOf(hands[seat], 15);
  }
}
