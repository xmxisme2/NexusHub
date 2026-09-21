package com.nexushub.rules.domain;

import java.util.Arrays;

/** 由规则引擎规范化的行动；cards 始终是点数 3 至大王的 15 位计数。 */
public record Move(MoveType type, int[] cards, Integer mainRank, int sequenceLength) {
  public Move {
    if (type == null || cards == null || cards.length != 15)
      throw new IllegalArgumentException("行动必须包含15位牌点计数");
    cards = Arrays.copyOf(cards, cards.length);
  }

  @Override
  public int[] cards() {
    return Arrays.copyOf(cards, cards.length);
  }

  public static Move pass() {
    return new Move(MoveType.PASS, new int[15], null, 0);
  }
}
