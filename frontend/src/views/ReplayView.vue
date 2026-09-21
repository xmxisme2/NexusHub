<template>
  <main class="app-shell">
    <AppHeader />
    <p v-if="loading" class="loading-line">正在读取行动历史…</p>
    <section v-else-if="error" class="panel error-panel">
      <h1>无法打开复盘</h1>
      <p>{{ error }}</p>
      <RouterLink class="button button-secondary" :to="`/landlord/games/${id}`"
        >返回对局</RouterLink
      >
    </section>
    <section v-else-if="replay" class="detail-content">
      <section class="page-heading">
        <div>
          <p class="eyebrow">REPLAY / GAME {{ replay.game.id }}</p>
          <h1>对局复盘</h1>
          <p>按服务端保存的行动事件查看每一步局面快照。</p>
        </div>
        <div class="game-heading__actions">
          <RouterLink
            class="button button-secondary"
            :to="`/landlord/games/${id}`"
            >返回对局</RouterLink
          ><RouterLink class="button" to="/landlord/puzzles">残局库</RouterLink>
        </div>
      </section>
      <section class="replay-layout">
        <article class="panel replay-summary">
          <div class="panel-heading">
            <h2>终局摘要</h2>
            <StatusBadge
              :tone="replay.game.status === 'FINISHED' ? 'success' : 'info'"
              >{{ statusText }}</StatusBadge
            >
          </div>
          <dl>
            <div>
              <dt>题目版本</dt>
              <dd>{{ replay.game.puzzleVersionId }}</dd>
            </div>
            <div>
              <dt>行动数</dt>
              <dd>{{ replay.total }}</dd>
            </div>
            <div>
              <dt>先手</dt>
              <dd>{{ replay.game.firstSeat === 0 ? "USER" : "BOT" }}</dd>
            </div>
            <div>
              <dt>结果</dt>
              <dd>
                {{
                  replay.game.winnerSide
                    ? `${replay.game.winnerSide} 获胜`
                    : "未结束"
                }}
              </dd>
            </div>
          </dl>
        </article>
        <article class="panel replay-actions">
          <div class="panel-heading">
            <h2>行动时间线</h2>
            <span class="eyebrow"
              >{{ replay.actions.length }} / {{ replay.total }}</span
            >
          </div>
          <p v-if="!replay.actions.length" class="empty-state">
            当前对局还没有可复盘的行动。
          </p>
          <ol v-else class="replay-timeline">
            <li
              v-for="action in replay.actions"
              :key="`${action.sequenceNo}-${action.afterVersion}`"
            >
              <div class="replay-timeline__head">
                <span class="replay-sequence">{{
                  String(action.sequenceNo).padStart(2, "0")
                }}</span
                ><strong>{{ action.actorSeat === 0 ? "USER" : "BOT" }}</strong
                ><StatusBadge
                  :tone="action.actorType === 'BOT' ? 'warning' : 'info'"
                  >{{ action.decisionQuality }}</StatusBadge
                ><time>{{ formatTime(action.createTime) }}</time>
              </div>
              <p>{{ moveText(action.move) }}</p>
              <small
                >v{{ action.beforeVersion }} → v{{ action.afterVersion }} ·
                {{ cardCount(action.afterState, 0) }} /
                {{ cardCount(action.afterState, 1) }} 张</small
              >
            </li>
          </ol>
        </article>
      </section>
    </section>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import { getReplay, type Move, type Replay } from "../api/modules/game";
import type { GameState } from "../api/modules/puzzle";
import { RANK_LABELS } from "../utils/cards";

const props = defineProps<{ id: string }>();
const id = props.id;
const replay = ref<Replay | null>(null);
const loading = ref(true);
const error = ref("");
const statusText = computed(() =>
  replay.value?.game.status === "FINISHED"
    ? "已结束"
    : replay.value?.game.status === "ABANDONED"
      ? "已放弃"
      : "进行中",
);
function moveText(move: Move) {
  if (move.type === "PASS") return "PASS · 不出";
  return `${moveTypeLabel(move.type)} · ${move.cards.flatMap((count, index) => Array.from({ length: count }, () => RANK_LABELS[index])).join(" ")}`;
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
function cardCount(state: GameState, seat: 0 | 1) {
  return state.hands[seat].reduce((sum, count) => sum + count, 0);
}
function formatTime(value: string) {
  return new Date(value).toLocaleString("zh-CN", { hour12: false });
}
onMounted(async () => {
  try {
    replay.value = await getReplay(id);
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : "网络连接未建立。";
  } finally {
    loading.value = false;
  }
});
</script>
