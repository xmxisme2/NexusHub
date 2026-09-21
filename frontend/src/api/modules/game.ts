import { request } from "../http";
import type { GameState } from "./puzzle";

/** 对局模式契约值；页面不得直接散落字符串。 */
export const GAME_MODES = {
  TRAINING: "TRAINING",
  OPTIMAL: "OPTIMAL",
} as const;

export type GameMode = (typeof GAME_MODES)[keyof typeof GAME_MODES];

export type MoveType =
  | "SINGLE"
  | "PAIR"
  | "TRIPLE"
  | "TRIPLE_SINGLE"
  | "TRIPLE_PAIR"
  | "STRAIGHT"
  | "PAIR_STRAIGHT"
  | "AIRPLANE"
  | "AIRPLANE_SINGLE"
  | "AIRPLANE_PAIR"
  | "BOMB"
  | "ROCKET"
  | "PASS";
export type Move = {
  type: MoveType;
  cards: number[];
  mainRank: number | null;
  sequenceLength: number;
};
export type LegalMove = {
  move: Move;
  proofStatus: "PROVEN" | "UNKNOWN";
  winnerSide: "USER" | "BOT" | null;
};
export type Game = {
  id: string;
  puzzleVersionId: string;
  playerSeat: 0;
  firstSeat: 0 | 1;
  mode: GameMode;
  status: "ACTIVE" | "FINISHED" | "ABANDONED";
  stateVersion: number;
  state: GameState;
  winnerSide: "USER" | "BOT" | null;
  robotStatus: "IDLE" | "QUEUED" | "THINKING" | "NEEDS_PROOF" | "FAILED";
  hintCount: number;
  allowedActions: string[];
  createTime: string;
};
export type LegalMovePage = {
  items: LegalMove[];
  total: number;
  pageNum: number;
  pageSize: number;
};
export type HintResponse = {
  move: Move | null;
  proofStatus: "PROVEN" | "UNKNOWN";
  winnerSide: "USER" | "BOT" | null;
  hintCount: number;
  stateVersion: number;
};
export type GameAction = {
  sequenceNo: number;
  actorSeat: 0 | 1;
  actorType: "HUMAN" | "BOT";
  move: Move;
  beforeVersion: number;
  afterVersion: number;
  afterState: GameState;
  decisionQuality: "OPTIMAL" | "PROVEN" | "HEURISTIC" | "HUMAN";
  createTime: string;
};
export type Replay = {
  game: Game;
  initialState: GameState;
  actions: GameAction[];
  total: number;
  pageNum: number;
  pageSize: number;
};
const idempotencyKey = () =>
  globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`;

export function getGame(id: string) {
  return request<Game>({ method: "POST", url: "/games/detail", data: { id } });
}
export function getReplay(gameId: string, pageNum = 1, pageSize = 100) {
  return request<Replay>({
    method: "POST",
    url: "/games/replay/detail",
    data: { gameId, pageNum, pageSize },
  });
}
export function createGame(payload: {
  puzzleVersionId: string;
  firstSeat: 0 | 1;
  mode: GameMode;
  proofResultId?: string;
  swapSeats?: boolean;
}) {
  return request<Game>({
    method: "POST",
    url: "/games/create",
    data: { ...payload, playerSeat: 0 },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
export function getLegalMoves(gameId: string, expectedStateVersion: number) {
  return request<LegalMovePage>({
    method: "POST",
    url: "/games/legal-moves/search",
    data: { gameId, expectedStateVersion, pageNum: 1, pageSize: 100 },
  });
}
export function playMove(
  gameId: string,
  expectedStateVersion: number,
  move: Move,
) {
  return request<Game>({
    method: "POST",
    url: "/games/actions/play",
    data: { gameId, expectedStateVersion, move },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
export function passMove(gameId: string, expectedStateVersion: number) {
  return request<Game>({
    method: "POST",
    url: "/games/actions/pass",
    data: { gameId, expectedStateVersion },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
export function restartGame(
  gameId: string,
  expectedStateVersion: number,
  mode: GameMode,
) {
  return request<Game>({
    method: "POST",
    url: "/games/restart",
    data: { gameId, expectedStateVersion, mode },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
export function abandonGame(gameId: string, expectedStateVersion: number) {
  return request<Game>({
    method: "POST",
    url: "/games/abandon",
    data: { gameId, expectedStateVersion },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
export function hintGame(gameId: string, expectedStateVersion: number) {
  return request<HintResponse>({
    method: "POST",
    url: "/games/hint",
    data: { gameId, expectedStateVersion },
    headers: { "Idempotency-Key": idempotencyKey() },
  });
}
