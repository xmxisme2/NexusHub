package com.nexushub.rules.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameRulesTest {
  @Test
  void firstMoveCannotPass() {
    GameState state = GameRules.initial(hands(new int[] {1, 0}, new int[] {0, 1}), 0);
    assertThrows(IllegalArgumentException.class, () -> GameRules.apply(state, Move.pass()));
  }

  @Test
  void userWinsWhenItsHandBecomesEmpty() {
    GameState state = GameRules.initial(hands(new int[] {1, 0}, new int[] {0, 1}), 0);
    int[] cards = new int[15];
    cards[0] = 1;
    GameTransition transition = GameRules.apply(state, new Move(MoveType.SINGLE, cards, 0, 0));
    assertEquals(Seat.USER, transition.winnerSide());
    assertEquals(0, transition.state().hand(0)[0]);
  }

  @Test
  void singlePassReturnsLeadToLastPlayerInTwoPlayerGame() {
    GameState initial = GameRules.initial(hands(new int[] {2, 0}, new int[] {1, 0}), 0);
    int[] cards = new int[15];
    cards[0] = 1;
    GameState afterPlay = GameRules.apply(initial, new Move(MoveType.SINGLE, cards, 0, 0)).state();
    GameState afterPass = GameRules.apply(afterPlay, Move.pass()).state();
    assertEquals(0, afterPass.currentSeat());
    assertNull(afterPass.targetMove());
    assertEquals(0, afterPass.consecutivePasses());
  }

  @Test
  void fourWithTwoIsNeverClassified() {
    int[] cards = new int[15];
    cards[0] = 4;
    cards[1] = 1;
    cards[2] = 1;
    assertThrows(IllegalArgumentException.class, () -> GameRules.canonicalize(cards));
  }

  private int[][] hands(int[] userPrefix, int[] botPrefix) {
    int[] user = new int[15], bot = new int[15];
    System.arraycopy(userPrefix, 0, user, 0, userPrefix.length);
    System.arraycopy(botPrefix, 0, bot, 0, botPrefix.length);
    return new int[][] {user, bot};
  }
}
