<template>
  <main class="app-shell">
    <AppHeader />
    <nav class="detail-navigation" aria-label="残局导航">
      <RouterLink class="button button-secondary" to="/landlord/puzzles">
        <span aria-hidden="true">←</span> 返回残局库
      </RouterLink>
    </nav>
    <p v-if="loading" class="loading-line">正在读取残局局面…</p>
    <section v-else-if="error" class="panel error-panel">
      <h1>无法打开残局</h1>
      <p>{{ error }}</p>
      <button class="button button-secondary" type="button" @click="load">
        重新加载
      </button>
    </section>
    <section v-else-if="puzzle" class="detail-content">
      <section class="page-heading">
        <div>
          <p class="eyebrow">PUZZLE DETAIL / {{ puzzle.id }}</p>
          <h1>{{ puzzle.version.title }}</h1>
          <p>{{ puzzle.version.description || "双人完全明牌残局" }}</p>
        </div>
        <div class="game-heading__actions">
          <button
            v-if="canPublish"
            class="button"
            type="button"
            :disabled="mutating"
            @click="publish"
          >
            {{ publishing ? "发布中…" : "发布残局" }}
          </button>
          <RouterLink
            v-if="puzzle.allowedActions.includes('EDIT') && !mutating"
            class="button button-secondary"
            :to="`/landlord/puzzles/${puzzle.id}/edit`"
            >编辑草稿</RouterLink
          ><button
            v-if="puzzle.allowedActions.includes('ARCHIVE')"
            class="button button-danger"
            type="button"
            :disabled="mutating"
            @click="archive"
          >
            {{ deleting ? "删除中…" : "删除残局" }}</button
          ><button
            v-if="puzzle.allowedActions.includes('RESTORE')"
            class="button button-secondary"
            type="button"
            :disabled="mutating"
            @click="restore"
          >
            {{ restoring ? "恢复中…" : "恢复残局" }}</button
          ><StatusBadge
            :tone="puzzle.status === 'PUBLISHED' ? 'success' : 'warning'"
            >{{
              puzzle.status === "PUBLISHED"
                ? "已发布"
                : puzzle.status === "ARCHIVED"
                  ? "已删除"
                  : "草稿"
            }}</StatusBadge
          >
        </div>
      </section>
      <section
        v-if="puzzle.status === 'DRAFT' || publishMessage"
        class="panel publish-panel"
      >
        <p v-if="puzzle.status === 'DRAFT'">
          {{
            canPublish
              ? "当前残局为草稿。点击“发布残局”后，其他用户可在已发布残局库中查看和对局，无需等待必胜证明。"
              : "当前残局为草稿，仅本人和管理员可见。请联系管理员打开此残局并点击“发布残局”，发布后将进入已发布残局库。"
          }}
        </p>
        <p
          v-if="publishMessage"
          class="validation-result"
          :class="{ invalid: publishFailed }"
          role="status"
        >
          {{ publishMessage }}
        </p>
        <button
          v-if="publishFailed"
          class="button button-secondary"
          type="button"
          @click="load"
        >
          刷新残局状态
        </button>
      </section>
      <section class="detail-layout">
        <article class="panel state-panel">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">INITIAL STATE</p>
              <h2>初始局面</h2>
            </div>
            <span class="rules-note">四带二不可出</span>
          </div>
          <div class="turn-indicator">
            <span class="turn-dot"></span
            >{{
              puzzle.version.initialState.firstSeat === 0
                ? "USER 先手，首手必须出牌"
                : "BOT 先手，首手必须出牌"
            }}
          </div>
          <div class="seats">
            <section class="seat">
              <div>
                <span class="seat-label">USER</span
                ><strong>{{ totalCards(0) }} 张</strong>
              </div>
              <div class="poker-row">
                <span
                  v-for="(card, index) in cards(0)"
                  :key="`${card}-${index}`"
                  class="poker-card"
                  >{{ card }}</span
                >
              </div>
            </section>
            <section class="seat">
              <div>
                <span class="seat-label">BOT</span
                ><strong>{{ totalCards(1) }} 张</strong>
              </div>
              <div class="poker-row">
                <span
                  v-for="(card, index) in cards(1)"
                  :key="`${card}-${index}`"
                  class="poker-card poker-card--bot"
                  >{{ card }}</span
                >
              </div>
            </section>
          </div>
        </article>
        <aside class="panel action-panel">
          <p class="eyebrow">VERSION {{ puzzle.version.versionNo }}</p>
          <h2>准备就绪</h2>
          <PuzzleProofBadge
            :proof-status="puzzle.proofStatus"
            :winner-side="puzzle.winnerSide"
          />
          <p>
            训练模式使用启发式落子；严格最优模式会在每个机器人回合实时执行多分支求解，无法证明时暂停机器人。
          </p>
          <p
            v-if="
              puzzle.proofStatus === 'PROVEN' && puzzle.winnerSide === 'USER'
            "
            class="swap-proof-note"
          >
            当前证明结论为 USER 必胜；勾选“交换先后手”后，可让机器人使用原 USER
            的必胜手牌。
          </p>
          <dl>
            <div>
              <dt>难度</dt>
              <dd>{{ difficultyLabel(puzzle.version.difficulty) }}</dd>
            </div>
            <div>
              <dt>先手</dt>
              <dd>
                {{
                  puzzle.version.initialState.firstSeat === 0 ? "USER" : "BOT"
                }}
              </dd>
            </div>
            <div>
              <dt>状态摘要</dt>
              <dd>{{ puzzle.version.stateHash.slice(0, 12) }}</dd>
            </div>
          </dl>
          <div class="action-stack">
            <label class="swap-seats-option">
              <input v-model="swapSeats" type="checkbox" />
              <span>
                交换先后手
                <small>让机器人使用原 USER 手牌并按交换后的先手出牌</small>
              </span>
            </label>
            <button
              class="button"
              type="button"
              :disabled="starting"
              @click="startGame(GAME_MODES.TRAINING)"
            >
              {{
                startingMode === "TRAINING" ? "正在创建…" : "开始训练对局"
              }}</button
            ><button
              class="button button-secondary"
              type="button"
              :disabled="starting"
              @click="startGame(GAME_MODES.OPTIMAL)"
            >
              {{
                startingMode === "OPTIMAL" ? "正在创建…" : "开始严格最优对局"
              }}
            </button>
          </div>
          <button
            class="button button-secondary strategy-toggle"
            type="button"
            @click="router.push(`/landlord/puzzles/${puzzle.id}/strategy`)"
          >
            查看已证明策略
          </button>
          <button
            class="button button-secondary"
            type="button"
            @click="validate"
          >
            校验局面
          </button>
          <p
            v-if="validationText"
            class="validation-result"
            :class="{ invalid: !validationOk }"
          >
            {{ validationText }}
          </p>
        </aside>
      </section>
    </section>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import PuzzleProofBadge from "../components/PuzzleProofBadge.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import {
  archivePuzzle,
  getPuzzle,
  publishPuzzle,
  restorePuzzle,
  validatePuzzle,
  type Puzzle,
  type PuzzleDifficulty,
} from "../api/modules/puzzle";
import { createGame, GAME_MODES, type GameMode } from "../api/modules/game";
import { useSessionStore } from "../stores/session";
const props = defineProps<{ id: string }>();
const router = useRouter();
const session = useSessionStore();
const puzzle = ref<Puzzle | null>(null);
const loading = ref(true);
const error = ref("");
const validationText = ref("");
const validationOk = ref(false);
const starting = ref(false);
const startingMode = ref<GameMode | null>(null);
const swapSeats = ref(false);
const deleting = ref(false);
const restoring = ref(false);
const publishing = ref(false);
const publishMessage = ref("");
const publishFailed = ref(false);
const mutating = computed(
  () => publishing.value || deleting.value || restoring.value,
);
/** 发布入口同时遵循会话权限、服务端动作和最新草稿版本约束。 */
const canPublish = computed(() =>
  Boolean(
    session.user?.permissions.includes("puzzle:publish") &&
    puzzle.value?.allowedActions.includes("PUBLISH") &&
    puzzle.value.status === "DRAFT" &&
    puzzle.value.version.id === puzzle.value.latestVersionId,
  ),
);
const ranks = [
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
];
function cards(seat: 0 | 1) {
  const hand = puzzle.value?.version.initialState.hands[seat] ?? [];
  return hand.flatMap((count, index) =>
    Array.from({ length: count }, () => ranks[index]),
  );
}
function totalCards(seat: 0 | 1) {
  return cards(seat).length;
}
function difficultyLabel(value: PuzzleDifficulty) {
  return { EASY: "简单", MEDIUM: "中等", HARD: "困难" }[value];
}
async function load() {
  loading.value = true;
  error.value = "";
  publishMessage.value = "";
  try {
    puzzle.value = await getPuzzle(props.id);
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : "网络连接未建立。";
  } finally {
    loading.value = false;
  }
}
async function publish() {
  if (!puzzle.value || !canPublish.value || mutating.value) return;
  publishing.value = true;
  publishMessage.value = "";
  publishFailed.value = false;
  try {
    puzzle.value = await publishPuzzle(
      puzzle.value.id,
      puzzle.value.rowVersion,
      puzzle.value.version.id,
    );
    publishMessage.value = "发布成功，残局已进入已发布残局库。";
  } catch (cause) {
    publishFailed.value = true;
    publishMessage.value =
      cause instanceof ApiError ? cause.message : "发布失败，请稍后重试。";
  } finally {
    publishing.value = false;
  }
}
async function validate() {
  if (!puzzle.value) return;
  try {
    const result = await validatePuzzle(puzzle.value.version.initialState);
    validationOk.value = result.valid;
    validationText.value = result.valid
      ? "服务端校验通过。"
      : result.errors[0]?.message ||
        result.errors[0]?.reason ||
        "局面不符合规则。";
  } catch (cause) {
    validationOk.value = false;
    validationText.value =
      cause instanceof ApiError ? cause.message : "暂时无法校验局面。";
  }
}
async function startGame(mode: GameMode) {
  if (!puzzle.value) return;
  starting.value = true;
  startingMode.value = mode;
  validationText.value = "";
  try {
    const game = await createGame({
      puzzleVersionId: puzzle.value.version.id,
      firstSeat: puzzle.value.version.initialState.firstSeat,
      mode,
      swapSeats: swapSeats.value,
    });
    await router.push(`/landlord/games/${game.id}`);
  } catch (cause) {
    validationOk.value = false;
    validationText.value =
      cause instanceof ApiError ? cause.message : "暂时无法创建对局。";
  } finally {
    starting.value = false;
    startingMode.value = null;
  }
}
async function archive() {
  if (!puzzle.value || !puzzle.value.allowedActions.includes("ARCHIVE")) return;
  if (!window.confirm("删除后会保留残局版本和证明记录，确认继续吗？")) return;
  deleting.value = true;
  validationText.value = "";
  try {
    await archivePuzzle({
      id: puzzle.value.id,
      expectedRowVersion: puzzle.value.rowVersion,
    });
    await router.push("/landlord/puzzles");
  } catch (cause) {
    validationOk.value = false;
    validationText.value =
      cause instanceof ApiError ? cause.message : "删除失败，请刷新后重试。";
  } finally {
    deleting.value = false;
  }
}
async function restore() {
  if (!puzzle.value || !puzzle.value.allowedActions.includes("RESTORE")) return;
  restoring.value = true;
  validationText.value = "";
  try {
    puzzle.value = await restorePuzzle({
      id: puzzle.value.id,
      expectedRowVersion: puzzle.value.rowVersion,
    });
  } catch (cause) {
    validationOk.value = false;
    validationText.value =
      cause instanceof ApiError ? cause.message : "恢复失败，请刷新后重试。";
  } finally {
    restoring.value = false;
  }
}
onMounted(load);
</script>
<style scoped>
.detail-navigation {
  margin: 24px 0;
}
.detail-navigation .button {
  gap: 8px;
}
.publish-panel {
  padding: 20px 24px;
}
.strategy-toggle {
  width: 100%;
  margin-top: 12px;
}
</style>
