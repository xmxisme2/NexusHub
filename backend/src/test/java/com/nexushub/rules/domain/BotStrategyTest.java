package com.nexushub.rules.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BotStrategyTest {
  @Test
  void choosesTheFirstLegalMoveOnItsTurn() {
    GameState state = GameRules.initial(hands(new int[] {1, 0}, new int[] {1, 1}), 1);
    Move selected = BotStrategy.choose(state);
    assertEquals(MoveType.SINGLE, selected.type());
    assertEquals(0, selected.mainRank());
  }

  @Test
  void passesWhenNoMoveCanBeatTheCurrentTrick() {
    GameState initial =
        GameRules.initial(hands(new int[] {0, 0, 0, 1, 1}, new int[] {1, 0, 0, 0}), 0);
    int[] userCards = new int[15];
    userCards[3] = 1;
    GameState botTurn =
        GameRules.apply(initial, new Move(MoveType.SINGLE, userCards, 3, 0)).state();
    assertEquals(MoveType.PASS, BotStrategy.choose(botTurn).type());
  }

  private int[][] hands(int[] userPrefix, int[] botPrefix) {
    int[] user = new int[15], bot = new int[15];
    System.arraycopy(userPrefix, 0, user, 0, userPrefix.length);
    System.arraycopy(botPrefix, 0, bot, 0, botPrefix.length);
    return new int[][] {user, bot};
  }
}
