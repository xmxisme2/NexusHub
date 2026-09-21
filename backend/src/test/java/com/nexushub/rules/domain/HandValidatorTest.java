package com.nexushub.rules.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HandValidatorTest {
  @Test
  void acceptsMinimalUserFirstPosition() {
    int[] user = new int[15], bot = new int[15];
    user[0] = 1;
    bot[1] = 1;
    assertTrue(HandValidator.validate(user, bot, Seat.USER).valid());
  }

  @Test
  void rejectsEmptyHand() {
    int[] user = new int[15], bot = new int[15];
    user[0] = 1;
    assertFalse(HandValidator.validate(user, bot, Seat.USER).valid());
  }

  @Test
  void rejectsTooManyCardsForFirstSeat() {
    int[] user = new int[15], bot = new int[15];
    for (int i = 0; i < 6; i++) user[i] = 4;
    bot[0] = 1;
    assertFalse(HandValidator.validate(user, bot, Seat.USER).valid());
  }

  @Test
  void rejectsMoreThanDeckCopiesAcrossBothPlayers() {
    int[] user = new int[15], bot = new int[15];
    user[0] = 3;
    bot[0] = 2;
    assertFalse(HandValidator.validate(user, bot, Seat.USER).valid());
  }
}
