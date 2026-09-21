<template>
  <main class="app-shell">
    <AppHeader />

    <section class="page-heading">
      <div>
        <p class="eyebrow">PUZZLE LIBRARY / 02</p>
        <h1>残局库</h1>
        <p>浏览已发布残局，或查看自己刚保存的草稿和历史版本。</p>
      </div>
      <button class="button button-secondary" type="button" @click="load">
        刷新列表
      </button>
    </section>

    <section class="library-toolbar">
      <div class="library-scope" role="tablist" aria-label="残局范围">
        <button
          v-for="option in scopeOptions"
          :key="option.value"
          class="scope-button"
          :class="{ active: scope === option.value }"
          type="button"
          role="tab"
          :aria-selected="scope === option.value"
          @click="changeScope(option.value)"
        >
          {{ option.label }}
        </button>
      </div>

      <label>
        检索
        <input
          v-model.trim="keyword"
          placeholder="按题名或标签搜索"
          @keyup.enter="load"
        />
      </label>
      <label>
        难度
        <select v-model="difficulty" @change="load">
          <option value="">全部难度</option>
          <option value="EASY">简单</option>
          <option value="MEDIUM">中等</option>
          <option value="HARD">困难</option>
        </select>
      </label>
      <span class="rules-note">CLASSIC_V1</span>
      <RouterLink
        v-if="session.user?.role !== 'GUEST'"
        class="button"
        to="/landlord/puzzles/new"
        >新建残局</RouterLink
      >
    </section>

    <p v-if="loading" class="loading-line" aria-live="polite">正在读取残局…</p>
    <section v-else-if="error" class="panel error-panel">
      <h2>题库暂时无法加载</h2>
      <p>{{ error }}</p>
      <button class="button button-secondary" type="button" @click="load">
        重试
      </button>
    </section>
    <section v-else-if="puzzles.length" class="puzzle-grid">
      <RouterLink
        v-for="puzzle in puzzles"
        :key="puzzle.id"
        class="puzzle-card"
        :to="`/landlord/puzzles/${puzzle.id}`"
        :aria-label="`打开残局：${puzzle.version.title}`"
      >
        <div class="puzzle-card__meta">
          <StatusBadge
            v-if="puzzle.status !== 'PUBLISHED'"
            :tone="puzzle.status === 'ARCHIVED' ? 'danger' : 'warning'"
          >
            {{ puzzle.status === "ARCHIVED" ? "已删除" : "草稿" }}
          </StatusBadge>
          <PuzzleProofBadge
            :proof-status="puzzle.proofStatus"
            :winner-side="puzzle.winnerSide"
          />
          <span>{{ difficultyLabel(puzzle.version.difficulty) }}</span>
        </div>
        <h2>{{ displayTitle(puzzle) }}</h2>
        <div class="puzzle-thumbnail" aria-label="双方初始手牌缩略图">
          <div class="puzzle-thumbnail__seat">
            <span class="seat-label"
              >USER · {{ totalCards(puzzle, 0) }} 张</span
            >
            <div class="poker-row poker-row--thumbnail">
              <PokerCard
                v-for="card in cardItems(puzzle, 0)"
                :key="`user-${puzzle.id}-${card.key}`"
                :rank-index="card.rankIndex"
                :copy-index="card.copyIndex"
                compact
              />
            </div>
          </div>
          <div class="puzzle-thumbnail__seat">
            <span class="seat-label">BOT · {{ totalCards(puzzle, 1) }} 张</span>
            <div class="poker-row poker-row--thumbnail">
              <PokerCard
                v-for="card in cardItems(puzzle, 1)"
                :key="`bot-${puzzle.id}-${card.key}`"
                :rank-index="card.rankIndex"
                :copy-index="card.copyIndex"
                compact
              />
            </div>
          </div>
        </div>
      </RouterLink>
    </section>
    <EmptyState
      v-else
      title="暂无符合条件的残局"
      :description="
        scope === 'MINE'
          ? '保存后的草稿会显示在“我的残局”中。'
          : '可以调整筛选条件，或等待管理员发布新的残局。'
      "
      action-text="清除筛选"
      @action="clearFilters"
    />
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import EmptyState from "../components/EmptyState.vue";
import PokerCard from "../components/PokerCard.vue";
import PuzzleProofBadge from "../components/PuzzleProofBadge.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import {
  searchPuzzles,
  type Puzzle,
  type PuzzleDifficulty,
  type PuzzleScope,
} from "../api/modules/puzzle";
import { useSessionStore } from "../stores/session";

const session = useSessionStore();
const puzzles = ref<Puzzle[]>([]);
const loading = ref(false);
const error = ref("");
const keyword = ref("");
const difficulty = ref<"" | PuzzleDifficulty>("");
const scope = ref<PuzzleScope>(
  session.user?.role === "GUEST" ? "PUBLISHED" : "MINE",
);

const scopeOptions = computed(() => {
  const options: { value: PuzzleScope; label: string }[] = [
    ...(session.user?.role === "GUEST"
      ? []
      : [{ value: "MINE" as PuzzleScope, label: "我的残局" }]),
    { value: "PUBLISHED", label: "已发布" },
  ];
  if (session.user?.role === "ADMIN") {
    options.push({ value: "MANAGE", label: "管理视图" });
  }
  return options;
});

function cardItems(puzzle: Puzzle, seat: 0 | 1) {
  const hand = puzzle.version.initialState.hands[seat] ?? [];
  return hand.flatMap((count, rankIndex) =>
    Array.from({ length: count }, (_, copyIndex) => ({
      rankIndex,
      copyIndex,
      key: `${rankIndex}-${copyIndex}`,
    })),
  );
}

function totalCards(puzzle: Puzzle, seat: 0 | 1) {
  return cardItems(puzzle, seat).length;
}

function displayTitle(puzzle: Puzzle) {
  const prefix = puzzle.version.title.split("·")[0]?.trim();
  return prefix ? `残局 ${prefix}` : `残局 ${puzzle.id}`;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    const page = await searchPuzzles({
      scope: scope.value,
      pageNum: 1,
      pageSize: 20,
      keyword: keyword.value || undefined,
      difficulty: difficulty.value || undefined,
    });
    puzzles.value = page.items;
  } catch (cause) {
    error.value =
      cause instanceof ApiError ? cause.message : "网络连接未建立。";
  } finally {
    loading.value = false;
  }
}

function changeScope(nextScope: PuzzleScope) {
  scope.value = nextScope;
  load();
}

function clearFilters() {
  keyword.value = "";
  difficulty.value = "";
  load();
}

function difficultyLabel(value: PuzzleDifficulty) {
  return { EASY: "简单", MEDIUM: "中等", HARD: "困难" }[value];
}

onMounted(load);
</script>
<style scoped>
.puzzle-card {
  display: block;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
}
.puzzle-card:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 4px;
}
</style>
