package com.nexushub.game.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexushub.game.api.GameDtos.GameRestartRequest;
import com.nexushub.game.api.GameDtos.GameCreateRequest;
import com.nexushub.game.api.GameDtos.GameVersionRequest;
import com.nexushub.game.persistence.GameMapper;
import com.nexushub.game.persistence.GameRow;
import com.nexushub.identity.persistence.UserRow;
import com.nexushub.puzzle.persistence.PuzzleMapper;
import com.nexushub.puzzle.persistence.PuzzleRow;
import com.nexushub.rules.domain.GameRules;
import com.nexushub.rules.domain.GameState;
import com.nexushub.rules.domain.Move;
import com.nexushub.rules.domain.MoveType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GameServiceTest {
  private final GameMapper games = mock(GameMapper.class);
  private final PuzzleMapper puzzles = mock(PuzzleMapper.class);
  private final ObjectMapper json = new ObjectMapper();
  private final GameService service = new GameService(games, puzzles, json);

  @Test
  void createWithSwapExchangesHandsAndFirstSeat() throws Exception {
    GameState puzzleState = GameRules.initial(hands(new int[] {3}, new int[] {1, 2}), 0);
    PuzzleRow puzzle = new PuzzleRow();
    puzzle.setId(2004L);
    puzzle.setVersionId(2104L);
    puzzle.setStatus("PUBLISHED");
    puzzle.setInitialStateJson(json.writeValueAsString(puzzleState));
    puzzle.setAllowedFirstSeatsJson("[0]");
    when(puzzles.findByVersionId(2104L)).thenReturn(puzzle);
    when(games.nextGameId()).thenReturn(3020L);
    GameRow saved = row(0, 1, "ACTIVE", GameRules.initial(hands(new int[] {1, 2}, new int[] {3}), 1), GameRules.initial(hands(new int[] {1, 2}, new int[] {3}), 1));
    when(games.findGame(3020L)).thenReturn(saved);

    UserRow actor = new UserRow();
    actor.setId(7L);
    service.create(new GameCreateRequest("2104", 0, 0, "TRAINING", null, true), actor);

    var captor = org.mockito.ArgumentCaptor.forClass(GameRow.class);
    verify(games).insertGame(captor.capture());
    assertEquals(1, captor.getValue().getFirstSeat());
    GameState swapped = json.readValue(captor.getValue().getInitialStateJson(), GameState.class);
    assertArrayEquals(puzzleState.hand(1), swapped.hand(0));
    assertArrayEquals(puzzleState.hand(0), swapped.hand(1));
    assertEquals(1, swapped.firstSeat());
  }

  @Test
  void restartRestoresInitialSnapshotAndInvalidatesOldActions() throws Exception {
    GameState initial = GameRules.initial(hands(new int[] {2}, new int[] {1}), 0);
    GameState changed =
        GameRules.apply(
                initial,
                new com.nexushub.rules.domain.Move(
                    com.nexushub.rules.domain.MoveType.SINGLE, cards(0, 1), 0, 0))
            .state();
    GameRow current = row(2, 0, "FINISHED", initial, changed);
    GameRow restarted = row(3, 0, "ACTIVE", initial, initial);
    when(games.findGame(3001L)).thenReturn(current, restarted);
    when(games.restartGame(3001L, 2, json.writeValueAsString(initial), "IDLE", "TRAINING", 7L))
        .thenReturn(1);

    UserRow actor = new UserRow();
    actor.setId(7L);
    var result =
        service.restart(
            new GameRestartRequest("3001", 2, "TRAINING", null), "restart-key-0001", actor);

    assertEquals("ACTIVE", result.status());
    assertEquals(3, result.stateVersion());
    assertArrayEquals(initial.hand(0), result.state().hand(0));
    assertArrayEquals(initial.hand(1), result.state().hand(1));
    verify(games).clearActions(3001L);
  }

  @Test
  void hintReturnsUnknownSuggestionAndOnlyIncrementsHintCount() throws Exception {
    GameState initial = GameRules.initial(hands(new int[] {2}, new int[] {1}), 0);
    GameRow current = row(4, 0, "ACTIVE", initial, initial);
    GameRow updated = row(4, 0, "ACTIVE", initial, initial);
    updated.setHintCount(1);
    when(games.findGame(3001L)).thenReturn(current, updated);
    when(games.incrementHintCount(3001L, 4)).thenReturn(1);

    UserRow actor = new UserRow();
    actor.setId(7L);
    var result = service.hint(new GameVersionRequest("3001", 4), "hint-key-00000001", actor);

    assertEquals(MoveType.SINGLE, result.move().type());
    assertEquals("UNKNOWN", result.proofStatus());
    assertEquals(null, result.winnerSide());
    assertEquals(1, result.hintCount());
    assertEquals(4, result.stateVersion());
    verify(games).incrementHintCount(3001L, 4);
    verify(games, never()).insertAction(any());
  }

  @Test
  void hintSuggestsPassWhenNoCardCanBeatCurrentTrick() throws Exception {
    GameState initial = GameRules.initial(hands(new int[] {2}, new int[] {0, 2, 1}), 1);
    Move botMove = new Move(MoveType.PAIR, cards(1, 2), 1, 0);
    GameState currentState = GameRules.apply(initial, botMove).state();
    GameRow current = row(2, 1, "ACTIVE", initial, currentState);
    GameRow updated = row(2, 1, "ACTIVE", initial, currentState);
    updated.setHintCount(3);
    when(games.findGame(3001L)).thenReturn(current, updated);
    when(games.incrementHintCount(3001L, 2)).thenReturn(1);

    UserRow actor = new UserRow();
    actor.setId(7L);
    var result = service.hint(new GameVersionRequest("3001", 2), "hint-key-00000002", actor);

    assertEquals(MoveType.PASS, result.move().type());
    assertEquals(3, result.hintCount());
    verify(games).incrementHintCount(3001L, 2);
  }

  @Test
  void abandonEndsActiveGameKeepsStateAndActionHistory() throws Exception {
    GameState initial = GameRules.initial(hands(new int[] {2}, new int[] {1}), 0);
    GameRow current = row(2, 0, "ACTIVE", initial, initial);
    GameRow abandoned = row(3, 0, "ABANDONED", initial, initial);
    abandoned.setRobotStatus("IDLE");
    when(games.findGame(3001L)).thenReturn(current, abandoned);
    when(games.abandonGame(3001L, 2, 7L)).thenReturn(1);

    UserRow actor = new UserRow();
    actor.setId(7L);
    var result = service.abandon(new GameVersionRequest("3001", 2), "abandon-key-0001", actor);

    assertEquals("ABANDONED", result.status());
    assertEquals(3, result.stateVersion());
    assertEquals(null, result.winnerSide());
    assertEquals("IDLE", result.robotStatus());
    verify(games).abandonGame(3001L, 2, 7L);
    verify(games, never()).clearActions(anyLong());
    verify(games, never()).insertAction(any());
  }

  private GameRow row(
      int version, int firstSeat, String status, GameState initial, GameState current)
      throws Exception {
    GameRow row = new GameRow();
    row.setId(3001L);
    row.setOwnerId(7L);
    row.setPuzzleVersionId(2001L);
    row.setFirstSeat(firstSeat);
    row.setMode("TRAINING");
    row.setStatus(status);
    row.setInitialStateJson(json.writeValueAsString(initial));
    row.setCurrentStateJson(json.writeValueAsString(current));
    row.setStateVersion(version);
    row.setHintCount(0);
    row.setRobotStatus("IDLE");
    row.setCreateTime(Instant.now());
    return row;
  }

  private int[][] hands(int[] user, int[] bot) {
    int[] u = new int[15], b = new int[15];
    System.arraycopy(user, 0, u, 0, user.length);
    System.arraycopy(bot, 0, b, 0, bot.length);
    return new int[][] {u, b};
  }

  private int[] cards(int rank, int count) {
    int[] cards = new int[15];
    cards[rank] = count;
    return cards;
  }
}
