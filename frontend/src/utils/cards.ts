import type { Move, MoveType } from "../api/modules/game";

/** 牌点顺序与后端 CLASSIC_V1 保持一致：3 至 2、小王、大王。 */
export const RANK_LABELS = [
  "3",
  "4",
  "5",
  "6",
  "7",
  "8",
  "9",
  "10",
  "J",
  "Q",
  "K",
  "A",
  "2",
  "小王",
  "大王",
] as const;

const total = (cards: number[]) => cards.reduce((sum, value) => sum + value, 0);
const singleRank = (cards: number[], expected: number) => {
  let found = -1;
  for (let index = 0; index < cards.length; index += 1) {
    if (cards[index] === expected) {
      if (found >= 0) return -1;
      found = index;
    } else if (cards[index] !== 0) return -1;
  }
  return found;
};
const rankWithCount = (cards: number[], expected: number) => {
  let found = -1;
  let matches = 0;
  cards.forEach((value, index) => {
    if (value === expected) {
      found = index;
      matches += 1;
    }
  });
  return matches === 1 ? found : -1;
};
const consecutive = (cards: number[], expected: number) => {
  let start = -1;
  let end = -1;
  for (let index = 0; index <= 11; index += 1) {
    if (cards[index] === expected) {
      if (start < 0) start = index;
      end = index;
    } else if (cards[index] !== 0) return 0;
  }
  for (let index = 12; index < 15; index += 1) if (cards[index] !== 0) return 0;
  return start < 0 ? 0 : end - start + 1;
};
const lastRank = (cards: number[]) => {
  for (let index = 14; index >= 0; index -= 1)
    if (cards[index] > 0) return index;
  return -1;
};
const lastRankWith = (cards: number[], expected: number) => {
  for (let index = 11; index >= 0; index -= 1)
    if (cards[index] === expected) return index;
  return -1;
};
const tripleRun = (cards: number[]) => {
  let groups = 0;
  let previous = -2;
  for (let index = 0; index <= 11; index += 1) {
    if (cards[index] === 3) {
      if (index !== previous + 1 && groups > 0) return 0;
      groups += 1;
      previous = index;
    } else if (cards[index] > 3) return 0;
  }
  return groups;
};
const removeTripleRun = (cards: number[], triples: number) => {
  const end = lastRankWith(cards, 3);
  for (let index = end - triples + 1; index <= end; index += 1)
    cards[index] -= 3;
};
const singleWings = (cards: number[], triples: number) => {
  const copy = [...cards];
  removeTripleRun(copy, triples);
  if (copy[13] === 1 && copy[14] === 1) return false;
  return total(copy) === triples;
};
const pairWings = (cards: number[], triples: number) => {
  const copy = [...cards];
  removeTripleRun(copy, triples);
  let pairs = 0;
  for (const value of copy) {
    if (value === 0) continue;
    if (value !== 2) return false;
    pairs += 1;
  }
  return pairs === triples;
};

/** 在提交请求前将选中的牌点计数规范化为后端 Move。 */
export function canonicalizeSelection(cards: number[]): {
  move: Move | null;
  message: string;
} {
  if (
    cards.length !== 15 ||
    cards.some((value, index) => value < 0 || value > (index >= 13 ? 1 : 4))
  )
    return { move: null, message: "选中的牌数量不合法。" };
  const count = total(cards);
  if (count === 0) return { move: null, message: "请先点击选择要出的牌。" };
  const move = (
    type: MoveType,
    mainRank: number | null,
    sequenceLength = 0,
  ): { move: Move; message: string } => ({
    move: { type, cards: [...cards], mainRank, sequenceLength },
    message: "",
  });
  if (count === 1) return move("SINGLE", lastRank(cards));
  if (count === 2 && cards[13] === 1 && cards[14] === 1)
    return move("ROCKET", 14);
  let rank = singleRank(cards, 2);
  if (count === 2 && rank >= 0) return move("PAIR", rank);
  rank = singleRank(cards, 3);
  if (count === 3 && rank >= 0) return move("TRIPLE", rank);
  rank = singleRank(cards, 4);
  if (count === 4 && rank >= 0) return move("BOMB", rank);
  const triple = rankWithCount(cards, 3);
  if (count === 4 && triple >= 0) return move("TRIPLE_SINGLE", triple);
  if (count === 5 && triple >= 0) {
    const rest = cards
      .filter((_, index) => index !== triple)
      .reduce((sum, value) => sum + value, 0);
    if (
      rest === 2 &&
      cards.every(
        (value, index) => index === triple || value === 0 || value === 2,
      )
    )
      return move("TRIPLE_PAIR", triple);
  }
  let sequence = consecutive(cards, 1);
  if (count >= 5 && sequence === count)
    return move("STRAIGHT", lastRank(cards), sequence);
  sequence = consecutive(cards, 2);
  if (count >= 6 && count % 2 === 0 && sequence === count / 2)
    return move("PAIR_STRAIGHT", lastRank(cards), sequence);
  const triples = tripleRun(cards);
  if (triples >= 2) {
    if (count === triples * 3)
      return move("AIRPLANE", lastRankWith(cards, 3), triples);
    if (count === triples * 4 && singleWings(cards, triples))
      return move("AIRPLANE_SINGLE", lastRankWith(cards, 3), triples);
    if (count === triples * 5 && pairWings(cards, triples))
      return move("AIRPLANE_PAIR", lastRankWith(cards, 3), triples);
  }
  return {
    move: null,
    message: "当前选择不是支持的斗地主牌型（四带二不可出）。",
  };
}

export function beatsSelection(
  candidate: Move,
  target: Move | null | undefined,
) {
  if (!target) return candidate.type !== "PASS";
  if (candidate.type === "ROCKET") return target.type !== "ROCKET";
  if (target.type === "ROCKET") return false;
  if (candidate.type === "BOMB")
    return (
      target.type !== "BOMB" ||
      (candidate.mainRank ?? -1) > (target.mainRank ?? -1)
    );
  if (
    target.type === "BOMB" ||
    candidate.type !== target.type ||
    candidate.sequenceLength !== target.sequenceLength
  )
    return false;
  return (candidate.mainRank ?? -1) > (target.mainRank ?? -1);
}

export function moveKey(move: Move) {
  return `${move.type}:${move.cards.join(",")}`;
}
