package com.nexushub.rules.domain;

import com.nexushub.common.constants.RulesConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** CLASSIC_V1 规则校验、行动规范化和状态转移的唯一入口，不依赖 HTTP 或数据库。 */
public final class GameRules {
  private GameRules() {}

  public static GameState initial(int[][] hands, int firstSeat) {
    if (firstSeat < 0 || firstSeat > 1) throw new IllegalArgumentException("先手座位非法");
    HandValidationResult validation =
        HandValidator.validate(hands[0], hands[1], firstSeat == 0 ? Seat.USER : Seat.BOT);
    if (!validation.valid()) throw new IllegalArgumentException(validation.message());
    return new GameState(
        RulesConstants.STATE_SCHEMA_VERSION,
        RulesConstants.CLASSIC_V1,
        hands,
        firstSeat,
        firstSeat,
        true,
        null,
        null,
        0);
  }

  public static GameTransition apply(GameState state, Move submitted) {
    validateState(state);
    if (submitted.type() == MoveType.PASS) return applyPass(state, submitted);
    Move move = canonicalize(submitted.cards());
    if (submitted.type() != move.type()) throw new IllegalArgumentException("牌型与实际牌点不一致");
    if (!contains(state.hand(state.currentSeat()), move.cards()))
      throw new IllegalArgumentException("出牌不属于当前手牌");
    if (state.targetMove() != null && !beats(move, state.targetMove()))
      throw new IllegalArgumentException("出牌不能压过当前牌墩");
    int[] left = subtract(state.hand(state.currentSeat()), move.cards());
    int[][] hands = state.hands();
    hands[state.currentSeat()] = left;
    if (total(left) == 0)
      return new GameTransition(
          new GameState(
              1,
              RulesConstants.CLASSIC_V1,
              hands,
              state.firstSeat(),
              state.currentSeat(),
              false,
              move,
              state.currentSeat(),
              0),
          state.currentSeat() == 0 ? Seat.USER : Seat.BOT);
    return new GameTransition(
        new GameState(
            1,
            RulesConstants.CLASSIC_V1,
            hands,
            state.firstSeat(),
            1 - state.currentSeat(),
            false,
            move,
            state.currentSeat(),
            0),
        null);
  }

  private static GameTransition applyPass(GameState state, Move submitted) {
    if (!allZero(submitted.cards())) throw new IllegalArgumentException("PASS 不能携带牌");
    if (state.isFirstMove() || state.targetMove() == null)
      throw new IllegalArgumentException("首手或自由领出时不能PASS");
    int next = 1 - state.currentSeat();
    if (next == state.lastPlaySeat()) {
      return new GameTransition(
          new GameState(
              RulesConstants.STATE_SCHEMA_VERSION,
              RulesConstants.CLASSIC_V1,
              state.hands(),
              state.firstSeat(),
              next,
              false,
              null,
              null,
              0),
          null);
    }
    return new GameTransition(
        new GameState(
            RulesConstants.STATE_SCHEMA_VERSION,
            RulesConstants.CLASSIC_V1,
            state.hands(),
            state.firstSeat(),
            next,
            false,
            state.targetMove(),
            state.lastPlaySeat(),
            1),
        null);
  }

  public static Move canonicalize(int[] cards) {
    if (cards == null || cards.length != 15) throw new IllegalArgumentException("行动必须包含15位牌点计数");
    for (int i = 0; i < 15; i++)
      if (cards[i] < 0 || cards[i] > (i >= 13 ? 1 : 4))
        throw new IllegalArgumentException("牌点数量非法");
    int count = total(cards);
    if (count == 0) throw new IllegalArgumentException("出牌不能为空");
    if (count == 1) return move(MoveType.SINGLE, cards, last(cards), 0);
    if (count == 2 && cards[13] == 1 && cards[14] == 1) return move(MoveType.ROCKET, cards, 14, 0);
    int rank = singleRank(cards, 2);
    if (count == 2 && rank >= 0) return move(MoveType.PAIR, cards, rank, 0);
    rank = singleRank(cards, 3);
    if (count == 3 && rank >= 0) return move(MoveType.TRIPLE, cards, rank, 0);
    rank = singleRank(cards, 4);
    if (count == 4 && rank >= 0) return move(MoveType.BOMB, cards, rank, 0);
    int triple = rankWithCount(cards, 3);
    if (count == 4 && triple >= 0) return move(MoveType.TRIPLE_SINGLE, cards, triple, 0);
    if (count == 5 && triple >= 0 && remainingIs(cards, triple, 2))
      return move(MoveType.TRIPLE_PAIR, cards, triple, 0);
    int sequence = consecutive(cards, 1);
    if (count >= 5 && sequence == count)
      return move(MoveType.STRAIGHT, cards, last(cards), sequence);
    sequence = consecutive(cards, 2);
    if (count >= 6 && count % 2 == 0 && sequence == count / 2)
      return move(MoveType.PAIR_STRAIGHT, cards, last(cards), sequence);
    int triples = tripleRun(cards);
    if (triples >= 2) {
      if (count == triples * 3)
        return move(MoveType.AIRPLANE, cards, lastRankWith(cards, 3), triples);
      if (count == triples * 4 && singleWings(cards, triples))
        return move(MoveType.AIRPLANE_SINGLE, cards, lastRankWith(cards, 3), triples);
      if (count == triples * 5 && pairWings(cards, triples))
        return move(MoveType.AIRPLANE_PAIR, cards, lastRankWith(cards, 3), triples);
    }
    throw new IllegalArgumentException("不支持的牌型：CLASSIC_V1 固定不含四带二");
  }

  public static boolean beats(Move candidate, Move target) {
    if (target == null) return candidate.type() != MoveType.PASS;
    if (candidate.type() == MoveType.ROCKET) return target.type() != MoveType.ROCKET;
    if (target.type() == MoveType.ROCKET) return false;
    if (candidate.type() == MoveType.BOMB)
      return target.type() != MoveType.BOMB || candidate.mainRank() > target.mainRank();
    if (target.type() == MoveType.BOMB
        || candidate.type() != target.type()
        || candidate.sequenceLength() != target.sequenceLength()) return false;
    return candidate.mainRank() > target.mainRank();
  }

  /** 从当前玩家手牌枚举全部合法出牌；牌型识别与实际落子共用 canonicalize。 */
  public static List<Move> legalMoves(GameState state) {
    validateState(state);
    List<Move> moves = new ArrayList<>();
    enumerate(state, state.hand(state.currentSeat()), new int[15], 0, moves);
    moves.sort(
        Comparator.comparingInt((Move m) -> total(m.cards()))
            .thenComparingInt(m -> m.type().ordinal())
            .thenComparingInt(Move::mainRank));
    return List.copyOf(moves);
  }

  private static void enumerate(
      GameState state, int[] hand, int[] selected, int rank, List<Move> moves) {
    if (rank == 15) {
      if (total(selected) == 0) return;
      try {
        Move move = canonicalize(selected);
        if (state.targetMove() == null || beats(move, state.targetMove())) moves.add(move);
      } catch (IllegalArgumentException ignored) {
        /* 非法牌型不是可选行动。 */
      }
      return;
    }
    for (int count = 0; count <= hand[rank]; count++) {
      selected[rank] = count;
      enumerate(state, hand, selected, rank + 1, moves);
    }
    selected[rank] = 0;
  }

  private static void validateState(GameState state) {
    if (state == null
        || state.schemaVersion() != RulesConstants.STATE_SCHEMA_VERSION
        || !RulesConstants.CLASSIC_V1.equals(state.rulesetVersion())
        || state.currentSeat() < 0
        || state.currentSeat() > 1) {
      throw new IllegalArgumentException("局面非法");
    }
    if (state.isFirstMove()
        && (state.targetMove() != null
            || state.lastPlaySeat() != null
            || state.currentSeat() != state.firstSeat()))
      throw new IllegalArgumentException("首手局面非法");
  }

  private static Move move(MoveType type, int[] cards, int rank, int sequence) {
    return new Move(type, cards, rank, sequence);
  }

  private static int total(int[] values) {
    int total = 0;
    for (int value : values) total += value;
    return total;
  }

  private static boolean allZero(int[] values) {
    return total(values) == 0;
  }

  private static boolean contains(int[] hand, int[] cards) {
    for (int i = 0; i < 15; i++) if (cards[i] > hand[i]) return false;
    return true;
  }

  private static int[] subtract(int[] hand, int[] cards) {
    int[] out = Arrays.copyOf(hand, 15);
    for (int i = 0; i < 15; i++) out[i] -= cards[i];
    return out;
  }

  private static int singleRank(int[] cards, int target) {
    int found = -1;
    for (int i = 0; i < 15; i++) {
      if (cards[i] == target) {
        if (found >= 0) return -1;
        found = i;
      } else if (cards[i] != 0) return -1;
    }
    return found;
  }

  private static boolean remainingIs(int[] cards, int exclude, int target) {
    for (int i = 0; i < 15; i++)
      if (i != exclude && cards[i] != 0 && cards[i] != target) return false;
    return true;
  }

  private static int last(int[] cards) {
    for (int i = 14; i >= 0; i--) if (cards[i] > 0) return i;
    return -1;
  }

  private static int consecutive(int[] cards, int expected) {
    int start = -1, end = -1;
    for (int i = 0; i <= 11; i++) {
      if (cards[i] == expected) {
        if (start < 0) start = i;
        end = i;
      } else if (cards[i] != 0) return 0;
    }
    for (int i = 12; i < 15; i++) if (cards[i] != 0) return 0;
    return start < 0 ? 0 : end - start + 1;
  }

  private static int lastRankWith(int[] cards, int expected) {
    for (int i = 11; i >= 0; i--) if (cards[i] == expected) return i;
    return -1;
  }

  private static int rankWithCount(int[] cards, int expected) {
    int rank = -1;
    for (int i = 0; i < 15; i++)
      if (cards[i] == expected) {
        if (rank >= 0) return -1;
        rank = i;
      }
    return rank;
  }

  private static int tripleRun(int[] cards) {
    int groups = 0, last = -2;
    for (int i = 0; i <= 11; i++) {
      if (cards[i] == 3) {
        if (i != last + 1 && groups > 0) return 0;
        groups++;
        last = i;
      } else if (cards[i] > 3) return 0;
    }
    return groups;
  }

  private static boolean singleWings(int[] cards, int triples) {
    int[] copy = Arrays.copyOf(cards, 15);
    removeTripleRun(copy, triples);
    if (copy[13] == 1 && copy[14] == 1) return false;
    return total(copy) == triples;
  }

  private static boolean pairWings(int[] cards, int triples) {
    int[] copy = Arrays.copyOf(cards, 15);
    removeTripleRun(copy, triples);
    int pairs = 0;
    for (int value : copy) {
      if (value == 0) continue;
      if (value != 2) return false;
      pairs++;
    }
    return pairs == triples;
  }

  private static void removeTripleRun(int[] cards, int triples) {
    int end = lastRankWith(cards, 3);
    for (int i = end - triples + 1; i <= end; i++) cards[i] -= 3;
  }
}
