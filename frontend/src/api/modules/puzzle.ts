import { request, type ApiFieldError } from "../http";
import type { Move } from "./game";

/** 规则版本契约值；与后端 RulesConstants.CLASSIC_V1 对齐。 */
export const RULESET_VERSION = "CLASSIC_V1" as const;

export type PuzzleScope = "PUBLISHED" | "MINE" | "MANAGE";
export type PuzzleDifficulty = "EASY" | "MEDIUM" | "HARD";
export type PuzzleStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";
export type GameState = {
  schemaVersion: 1;
  rulesetVersion: typeof RULESET_VERSION;
  hands: number[][];
  firstSeat: 0 | 1;
  currentSeat?: 0 | 1;
  isFirstMove?: boolean;
  targetMove?: unknown | null;
  lastPlaySeat?: 0 | 1 | null;
  consecutivePasses?: number;
};
export type PuzzleVersion = {
  id: string;
  versionNo: number;
  title: string;
  description: string;
  tags: string[];
  difficulty: PuzzleDifficulty;
  allowedFirstSeats: number[];
  initialState: GameState;
  stateHash: string;
};
export type Puzzle = {
  id: string;
  ownerId: string;
  status: PuzzleStatus;
  rowVersion: number;
  latestVersionId: string | null;
  publishedVersionId: string | null;
  version: PuzzleVersion;
  allowedActions: string[];
  proofStatus: "PROVEN" | "UNKNOWN";
  winnerSide: "USER" | "BOT" | null;
};
export type PuzzlePage = {
  items: Puzzle[];
  total: number;
  pageNum: number;
  pageSize: number;
};
export type WinningLine = {
  lineNo: number;
  moves: Move[];
  verified: boolean;
};
export type ProofStrategy = {
  id: string;
  taskStatus: string;
  result: {
    proofStatus: "PROVEN" | "UNKNOWN";
    winnerSide: "USER" | "BOT" | null;
    strategyStatus: string;
    winningLines: WinningLine[];
    solverVersion: string;
    rulesetVersion: string;
  } | null;
};
export type PuzzleSearch = {
  pageNum: number;
  pageSize: number;
  scope: PuzzleScope;
  keyword?: string;
  tag?: string;
  difficulty?: PuzzleDifficulty;
  status?: PuzzleStatus;
};
export type PuzzleSave = {
  id?: string;
  expectedRowVersion?: number;
  title: string;
  description: string;
  tags: string[];
  difficulty: PuzzleDifficulty;
  allowedFirstSeats: number[];
  state: GameState;
};
export type PuzzleValidation = {
  valid: boolean;
  errors: ApiFieldError[];
  canonicalState: GameState | null;
  stateHash: string | null;
};
export type PuzzleDuplicate = {
  id: string;
  versionId: string;
  title: string;
  status: PuzzleStatus;
  deleted: boolean;
  stateHash: string;
};
export type PuzzleDuplicateCheck = {
  duplicate: boolean;
  existing: PuzzleDuplicate | null;
};
export type PuzzleArchiveRequest = { id: string; expectedRowVersion: number };

export function searchPuzzles(payload: PuzzleSearch) {
  return request<PuzzlePage>({
    method: "POST",
    url: "/puzzles/search",
    data: payload,
  });
}
export function getPuzzle(id: string, versionId?: string) {
  return request<Puzzle>({
    method: "POST",
    url: "/puzzles/detail",
    data: { id, versionId },
  });
}
export function getProofStrategy(versionId: string) {
  return request<ProofStrategy>({
    method: "POST",
    url: "/puzzles/proof-strategy",
    data: { versionId },
  });
}
export function validatePuzzle(state: GameState) {
  return request<PuzzleValidation>({
    method: "POST",
    url: "/puzzles/validate",
    data: { state },
  });
}
export function checkPuzzleDuplicate(
  state: GameState,
  excludePuzzleId?: string,
) {
  return request<PuzzleDuplicateCheck>({
    method: "POST",
    url: "/puzzles/duplicate-check",
    data: { state, excludePuzzleId },
  });
}
export function savePuzzle(payload: PuzzleSave) {
  return request<Puzzle>({
    method: "POST",
    url: "/puzzles/save",
    data: payload,
  });
}
export function publishPuzzle(
  id: string,
  expectedRowVersion: number,
  versionId: string,
) {
  return request<Puzzle>({
    method: "POST",
    url: "/puzzles/publish",
    headers: { "Idempotency-Key": crypto.randomUUID() },
    data: { id, expectedRowVersion, versionId },
  });
}
export function archivePuzzle(payload: PuzzleArchiveRequest) {
  return request<Puzzle>({
    method: "POST",
    url: "/puzzles/archive",
    data: payload,
  });
}
export function restorePuzzle(payload: PuzzleArchiveRequest) {
  return request<Puzzle>({
    method: "POST",
    url: "/puzzles/restore",
    data: payload,
  });
}
