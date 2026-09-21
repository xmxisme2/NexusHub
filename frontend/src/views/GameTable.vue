<template>
  <main class="app-shell">
    <AppHeader />
    <p v-if="loading" class="loading-line">正在同步对局状态…</p>
    <section v-else-if="error" class="panel error-panel">
      <h1>无法打开对局</h1>
      <p>{{ error }}</p>
      <RouterLink class="button button-secondary" to="/landlord/puzzles"
        >返回残局库</RouterLink
      >
    </section>
    <section v-else-if="game" class="game-content">
      <section class="page-heading game-heading">
        <div>
          <p class="eyebrow">GAME TABLE / {{ game.id }}</p>
          <h1>{{ game.status === "FINISHED" ? "对局已结束" : "明牌对局" }}</h1>
          <p>{{ stateLabel }}</p>
        </div>
        <div class="game-heading__actions">
          <RouterLink class="button button-secondary" to="/landlord/puzzles"
            >返回残局库</RouterLink
          ><RouterLink
            class="button button-secondary"
            :to="`/landlord/games/${game.id}/replay`"
            >复盘</RouterLink
          ><button
            v-if="game.allowedActions.includes('ABANDON')"
            class="button button-danger"
            type="button"
            :disabled="abandoning || restarting"
            @click="abandonGame"
          >
            {{ abandoning ? "正在弃局…" : "弃局" }}</button
          ><button
            class="button button-secondary"
            type="button"
            :disabled="restarting || abandoning"
            @click="restartGame"
          >
            {{ restarting ? "正在重开…" : "重新开始" }}</button
          ><StatusBadge
            :tone="game.status === 'FINISHED' ? 'success' : 'info'"
            >{{ statusLabel }}</StatusBadge
          >
        </div>
      </section>
      <section v-if="game.status === 'FINISHED'" class="winner-banner">
        <span>✦</span>
        <div>
          <strong>{{
            game.winnerSide === "USER" ? "USER 获胜" : "BOT 获胜"
          }}</strong>
          <p>一方已先出完手牌，此局面已锁定为只读。</p>
        </div>
      </section>
      <section class="game-layout">
        <article class="game-board panel">
          <section
            class="game-seat bot-seat"
            :class="{ active: game.state.currentSeat === 1 }"
          >
            <div class="seat-title">
              <span class="seat-avatar bot-avatar">B</span>
              <div>
                <span class="seat-label">BOT</span
                ><strong>{{ cardCount(1) }} 张</strong>
              </div>
              <StatusBadge
                v-if="game.state.currentSeat === 1 && game.status === 'ACTIVE'"
                tone="warning"
                >行动中</StatusBadge
              >
            </div>
            <div class="poker-row poker-row--face-up">
              <PokerCard
                v-for="card in cardItems(1)"
                :key="`bot-${card.key}`"
                :rank-index="card.rankIndex"
                :copy-index="card.copyIndex"
                compact
              />
            </div>
          </section>
          <section class="table-center">
            <p class="eyebrow">CURRENT TRICK</p>
            <div class="trick-card">{{ moveText(game.state.targetMove) }}</div>
            <p class="trick-note">
              {{
                game.state.isFirstMove
                  ? "首手必须出牌"
                  : game.state.lastPlaySeat === null
                    ? "新一轮由当前行动者领出"
                    : `上次出牌：${game.state.lastPlaySeat === 0 ? "USER" : "BOT"}`
              }}
            </p>
          </section>
          <section
            class="game-seat user-seat"
            :class="{ active: game.state.currentSeat === 0 }"
          >
            <div class="seat-title">
              <span class="seat-avatar">U</span>
              <div>
                <span class="seat-label">USER · 点击选牌</span
                ><strong>{{ cardCount(0) }} 张</strong>
              </div>
              <StatusBadge
                v-if="game.state.currentSeat === 0 && game.status === 'ACTIVE'"
                tone="success"
                >轮到你</StatusBadge
              >
            </div>
            <div class="poker-row poker-row--selectable">
              <PokerCard
                v-for="card in cardItems(0)"
                :key="`user-${card.key}`"
                :rank-index="card.rankIndex"
                :copy-index="card.copyIndex"
                :selected="isSelected(card)"
                :selectable="canPlay"
                @toggle="toggleCard(card.rankIndex, card.copyIndex)"
              />
            </div>
            <p v-if="canPlay" class="selection-tip">
              每次点击只选择或取消一张实体牌，同点数多张需要逐张点击；系统会自动识别牌型。
            </p>
          </section>
        </article>
        <aside class="panel game-action-panel">
          <p class="eyebrow">YOUR ACTION</p>
          <h2>{{ actionTitle }}</h2>
          <p>{{ actionDescription }}</p>
          <div
            class="selection-preview"
            :class="{ 'selection-preview--waiting': !canPlay }"
          >
            <div class="selection-preview__top">
              <span>{{ canPlay ? "当前选择" : "行动状态" }}</span
              ><strong>{{ canPlay ? selectedCount + " 张" : "等待中" }}</strong>
            </div>
            <div
              v-if="canPlay && selectionMove"
              class="selection-preview__move"
            >
              {{ moveText(selectionMove) }}
            </div>
            <p v-else-if="canPlay" class="selection-preview__empty">
              从左侧牌面开始选择
            </p>
            <p v-else class="selection-preview__empty">
              等待轮到 USER 后可点击左侧牌面出牌。
            </p>
            <p
              class="selection-message"
              :class="{
                invalid: !selectionValid && selectionMessage,
                'message-empty': !canPlay || !selectionMessage,
              }"
            >
              {{ canPlay ? selectionMessage : "" }}
            </p>
          </div>
          <div class="legal-summary">
            <span class="legal-dot"></span
            ><span>{{
              canPlay
                ? "服务端已返回 " +
                  legalMoves.length +
                  " 个合法行动，提交前会再次进行前端规则校验。"
                : "当前不在用户行动回合。"
            }}</span>
          </div>
          <p
            class="loading-line game-loading-line"
            :class="{ 'message-empty': !movesLoading }"
          >
            {{ movesLoading ? "正在同步合法行动…" : "" }}
          </p>
          <div class="action-buttons">
            <button
              class="button"
              type="button"
              :disabled="!canSubmitPlay || submitting"
              @click="play"
            >
              {{ submitting ? "提交中…" : "出牌" }}</button
            ><button
              class="button button-secondary"
              type="button"
              :disabled="!canPass || submitting"
              @click="pass"
            >
              PASS
            </button>
          </div>
          <p
            class="form-error action-error"
            :class="{ 'message-empty': !actionError }"
            role="alert"
          >
            {{ actionError }}
          </p>
          <dl>
            <div>
              <dt>模式</dt>
              <dd>
                {{ game.mode === GAME_MODES.OPTIMAL ? "严格最优" : "训练" }}
              </dd>
            </div>
            <div>
              <dt>机器人</dt>
              <dd>{{ robotLabel }}</dd>
            </div>
            <div>
              <dt>局面版本</dt>
              <dd>v{{ game.stateVersion }}</dd>
            </div>
          </dl>
          <section class="analysis-card" aria-label="策略分析状态">
            <div class="analysis-card__heading">
              <span class="eyebrow">STRATEGY ANALYSIS</span
              ><StatusBadge :tone="analysisTone">{{
                analysisStatus
              }}</StatusBadge>
            </div>
            <strong>{{ analysisTitle }}</strong>
            <p>{{ analysisDescription }}</p>
            <div
              class="recommended-move"
              :class="{ 'message-empty': !hintResult?.move }"
            >
              <span>建议行动</span
              ><strong>{{
                hintResult?.move ? moveText(hintResult.move) : ""
              }}</strong>
            </div>
            <button
              class="button button-secondary analysis-action"
              type="button"
              :disabled="!canHint || hintLoading"
              @click="requestHint"
            >
              {{
                hintLoading
                  ? "分析中…"
                  : hintResult
                    ? "重新获取提示"
                    : "请求一条策略提示"
              }}
            </button>
            <p
              class="form-error hint-error"
              :class="{ 'message-empty': !hintError }"
              role="alert"
            >
              {{ hintError }}
            </p>
          </section>
          <button class="refresh-link" type="button" @click="load">
            刷新当前状态
          </button>
        </aside>
      </section>
    </section>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import PokerCard from "../components/PokerCard.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import {
  abandonGame as abandonGameRequest,
  GAME_MODES,
  getGame,
  getLegalMoves,
  hintGame,
  passMove,
  playMove,
  restartGame as restartGameRequest,
  type Game,
  type HintResponse,
  type Move,
} from "../api/modules/game";
import {
  RANK_LABELS,
  beatsSelection,
  canonicalizeSelection,
  moveKey,
} from "../utils/cards";

const props = defineProps<{ id: string }>();
const game = ref<Game | null>(null);
const legalMoves = ref<
  {
    move: Move;
    proofStatus: "PROVEN" | "UNKNOWN";
    winnerSide: "USER" | "BOT" | null;
  }[]
>([]);
// 使用实体牌 key 记录选择状态，避免点击一张牌时把同点数的其他牌一起选中。
const selectedCards = ref<Set<string>>(new Set());
const loading = ref(true);
const movesLoading = ref(false);
const submitting = ref(false);
const restarting = ref(false);
const abandoning = ref(false);
const hintLoading = ref(false);
const error = ref("");
const actionError = ref("");
const hintError = ref("");
const hintResult = ref<HintResponse | null>(null);
let pollTimer: number | undefined;
const canPlay = computed(
  () =>
    game.value?.status === "ACTIVE" &&
    game.value.state.currentSeat === 0 &&
    game.value.allowedActions.includes("PLAY"),
);
const canPass = computed(
  () =>
    game.value?.status === "ACTIVE" &&
    game.value.state.currentSeat === 0 &&
    game.value.allowedActions.includes("PASS"),
);
const statusLabel = computed(() =>
  game.value?.status === "ACTIVE"
    ? "进行中"
    : game.value?.status === "FINISHED"
      ? "已结束"
      : "已放弃",
);
const robotLabel = computed(
  () =>
    ({
      IDLE: "空闲",
      QUEUED: "等待行动",
      THINKING: "正在思考",
      NEEDS_PROOF: "需要证明",
      FAILED: "暂不可用",
    })[game.value?.robotStatus ?? "IDLE"],
);
const stateLabel = computed(() =>
  game.value?.status === "ACTIVE"
    ? game.value.state.currentSeat === 0
      ? "轮到 USER 行动；点击牌面选择出牌。"
      : "BOT 正在等待或执行行动；状态以服务端快照为准。"
    : "终局不再接受出牌或 PASS。",
);
const actionTitle = computed(() =>
  canPlay.value
    ? "点击牌面出牌"
    : canPass.value
      ? "可以选择 PASS"
      : game.value?.status === "FINISHED"
        ? "对局已结束"
        : "等待对方行动",
);
const actionDescription = computed(() =>
  canPlay.value
    ? "前端先识别牌型并检查牌墩，后端收到请求后会再次校验手牌、牌型和状态版本。"
    : game.value?.robotStatus === "NEEDS_PROOF"
      ? "当前机器人需要完成证明后才能继续。"
      : "当前操作由服务端状态决定。",
);
const canHint = computed(
  () =>
    game.value?.status === "ACTIVE" &&
    game.value.state.currentSeat === 0 &&
    game.value.allowedActions.includes("HINT"),
);
const analysisStatus = computed<"PROVEN" | "UNKNOWN">(
  () => hintResult.value?.proofStatus ?? "UNKNOWN",
);
const analysisTone = computed<"success" | "warning">(() =>
  analysisStatus.value === "PROVEN" ? "success" : "warning",
);
const analysisTitle = computed(() => {
  if (hintResult.value?.proofStatus === "PROVEN")
    return `${hintResult.value.winnerSide === "USER" ? "USER" : "BOT"} 必胜已证明`;
  return game.value?.status === "FINISHED"
    ? "终局结果已由服务端确定"
    : hintResult.value
      ? "当前局面暂未完成必胜证明"
      : "尚未请求当前局面的策略提示";
});
const analysisDescription = computed(() => {
  if (hintResult.value?.proofStatus === "PROVEN")
    return "后端已完成当前局面的多分支验证，下面展示推荐行动。";
  if (game.value?.status === "FINISHED")
    return `胜者：${game.value.winnerSide === "USER" ? "USER" : "BOT"}。`;
  if (hintResult.value)
    return "UNKNOWN 表示当前预算内没有完成严格的多分支证明，不代表任一方必胜；提示行动仅供参考。";
  return "提示仅在 USER 回合可用，不改变对局版本。后端会再次验证当前局面。";
});
const robotTurn = computed(
  () => game.value?.status === "ACTIVE" && game.value.state.currentSeat === 1,
);
const selectedCounts = computed(() => {
  const counts = Array(15).fill(0);
  for (const key of selectedCards.value) {
    const rankIndex = Number(key.split(":")[0]);
    if (
      Number.isInteger(rankIndex) &&
      rankIndex >= 0 &&
      rankIndex < counts.length
    )
      counts[rankIndex] += 1;
  }
  return counts;
});
const selectedCount = computed(() => selectedCards.value.size);
const normalizedSelection = computed(() =>
  canonicalizeSelection(selectedCounts.value),
);
const selectionMove = computed(() => normalizedSelection.value.move);
const selectionValid = computed(() =>
  Boolean(
    selectionMove.value &&
      game.value &&
      beatsSelection(
        selectionMove.value,
        asMove(game.value.state.targetMove),
      ) &&
      legalMoves.value.some(
        (item) => moveKey(item.move) === moveKey(selectionMove.value!),
      ),
  ),
);
const selectionMessage = computed(() => {
  if (!selectedCount.value) return "";
  if (!normalizedSelection.value.move) return normalizedSelection.value.message;
  if (
    !game.value ||
    !beatsSelection(
      normalizedSelection.value.move,
      asMove(game.value.state.targetMove),
    )
  )
    return "当前选择无法压过桌面牌墩。";
  if (
    !legalMoves.value.some(
      (item) => moveKey(item.move) === moveKey(normalizedSelection.value.move!),
    )
  )
    return "当前选择未出现在服务端合法行动中，请刷新后重试。";
  return `已识别为${moveTypeLabel(normalizedSelection.value.move.type)}，可以提交。`;
});
const canSubmitPlay = computed(() => canPlay.value && selectionValid.value);

function asMove(value: unknown): Move | null {
  return value &&
    typeof value === "object" &&
    "type" in value &&
    "cards" in value
    ? (value as Move)
    : null;
}
function cardItems(seat: 0 | 1) {
  const hand = game.value?.state.hands[seat] ?? [];
  return hand.flatMap((count, rankIndex) =>
    Array.from({ length: count }, (_, copyIndex) => ({
      rankIndex,
      copyIndex,
      key: `${rankIndex}-${copyIndex}`,
    })),
  );
}
function cardCount(seat: 0 | 1) {
  return cardItems(seat).length;
}
function cardKey(rankIndex: number, copyIndex: number) {
  return `${rankIndex}:${copyIndex}`;
}
function isSelected(card: { rankIndex: number; copyIndex: number }) {
  return selectedCards.value.has(cardKey(card.rankIndex, card.copyIndex));
}
function toggleCard(rankIndex: number, copyIndex: number) {
  if (!canPlay.value) return;
  const next = new Set(selectedCards.value);
  const key = cardKey(rankIndex, copyIndex);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  selectedCards.value = next;
  actionError.value = "";
}
function moveText(move: Move | unknown | null | undefined) {
  const typed = asMove(move);
  if (!typed) return "尚未出牌";
  if (typed.type === "PASS") return "PASS";
  const shown = typed.cards
    .flatMap((count, index) =>
      Array.from({ length: count }, () => RANK_LABELS[index]),
    )
    .join(" ");
  return `${moveTypeLabel(typed.type)} · ${shown}`;
}
function moveTypeLabel(type: Move["type"]) {
  return {
    SINGLE: "单张",
    PAIR: "对子",
    TRIPLE: "三张",
    TRIPLE_SINGLE: "三带一",
    TRIPLE_PAIR: "三带二",
    STRAIGHT: "顺子",
    PAIR_STRAIGHT: "连对",
    AIRPLANE: "飞机",
    AIRPLANE_SINGLE: "飞机带单",
    AIRPLANE_PAIR: "飞机带对",
    BOMB: "炸弹",
    ROCKET: "王炸",
    PASS: "不出",
  }[type];
}
function clearSelection() {
  selectedCards.value = new Set();
}
async function loadMoves(current: Game) {
  legalMoves.value = [];
  clearSelection();
  hintResult.value = null;
  hintError.value = "";
  if (
    !(
      current.status === "ACTIVE" &&
      current.state.currentSeat === 0 &&
      current.allowedActions.includes("PLAY")
    )
  )
    return;
  movesLoading.value = true;
  try {
    legalMoves.value = (
      await getLegalMoves(current.id, current.stateVersion)
    ).items;
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError ? cause.message : "无法取得合法行动。";
  } finally {
    movesLoading.value = false;
  }
}
function schedulePoll() {
  if (pollTimer !== undefined) window.clearTimeout(pollTimer);
  if (!robotTurn.value) return;
  pollTimer = window.setTimeout(syncRobotTurn, 1500);
}
async function syncRobotTurn() {
  if (!robotTurn.value || submitting.value) {
    schedulePoll();
    return;
  }
  try {
    const current = await getGame(props.id);
    game.value = current;
    if (!robotTurn.value) await loadMoves(current);
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError
        ? cause.message
        : "机器人状态刷新失败，将自动重试。";
  } finally {
    schedulePoll();
  }
}
async function load() {
  loading.value = true;
  error.value = "";
  actionError.value = "";
  hintError.value = "";
  hintResult.value = null;
  try {
    const current = await getGame(props.id);
    game.value = current;
    await loadMoves(current);
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : "网络连接未建立。";
  } finally {
    loading.value = false;
    schedulePoll();
  }
}
async function play() {
  if (!game.value || !selectionMove.value) return;
  if (!selectionValid.value) {
    actionError.value = selectionMessage.value || "请选择合法牌型。";
    return;
  }
  submitting.value = true;
  actionError.value = "";
  try {
    game.value = await playMove(
      game.value.id,
      game.value.stateVersion,
      selectionMove.value,
    );
    await loadMoves(game.value);
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError ? cause.message : "出牌未完成，请刷新后重试。";
  } finally {
    submitting.value = false;
    schedulePoll();
  }
}
async function pass() {
  if (!game.value) return;
  submitting.value = true;
  actionError.value = "";
  clearSelection();
  try {
    game.value = await passMove(game.value.id, game.value.stateVersion);
    await loadMoves(game.value);
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError ? cause.message : "PASS 未完成，请刷新后重试。";
  } finally {
    submitting.value = false;
    schedulePoll();
  }
}
async function restartGame() {
  if (!game.value) return;
  restarting.value = true;
  actionError.value = "";
  try {
    const restarted = await restartGameRequest(
      game.value.id,
      game.value.stateVersion,
      game.value.mode,
    );
    game.value = restarted;
    await loadMoves(restarted);
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError
        ? cause.message
        : "暂时无法重新开始，请稍后重试。";
  } finally {
    restarting.value = false;
    schedulePoll();
  }
}
async function abandonGame() {
  if (!game.value || !game.value.allowedActions.includes("ABANDON")) return;
  if (!window.confirm("确认放弃当前对局吗？已有行动会保留，之后可以重新开始。"))
    return;
  abandoning.value = true;
  actionError.value = "";
  try {
    const abandoned = await abandonGameRequest(
      game.value.id,
      game.value.stateVersion,
    );
    game.value = abandoned;
    await loadMoves(abandoned);
  } catch (cause) {
    actionError.value =
      cause instanceof ApiError
        ? cause.message
        : "暂时无法弃局，请刷新后重试。";
  } finally {
    abandoning.value = false;
    schedulePoll();
  }
}
async function requestHint() {
  if (!game.value || !canHint.value) return;
  hintLoading.value = true;
  hintError.value = "";
  try {
    hintResult.value = await hintGame(game.value.id, game.value.stateVersion);
  } catch (cause) {
    hintError.value =
      cause instanceof ApiError
        ? cause.message
        : "策略提示未完成，请稍后重试。";
  } finally {
    hintLoading.value = false;
  }
}
onMounted(load);
onUnmounted(() => {
  if (pollTimer !== undefined) window.clearTimeout(pollTimer);
});
</script>
