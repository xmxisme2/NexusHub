<template>
  <main class="app-shell strategy-page">
    <AppHeader />
    <nav class="detail-navigation" aria-label="策略导航">
      <RouterLink class="button button-secondary" :to="`/landlord/puzzles/${id}`">
        <span aria-hidden="true">←</span> 返回残局
      </RouterLink>
      <RouterLink class="button button-secondary" to="/landlord/puzzles">
        返回残局库
      </RouterLink>
    </nav>

    <p v-if="loading" class="loading-line">正在读取严格证明策略…</p>
    <section v-else-if="error" class="panel error-panel">
      <h1>无法读取证明策略</h1>
      <p>{{ error }}</p>
      <button class="button button-secondary" type="button" @click="load">
        重新加载
      </button>
    </section>
    <section v-else-if="puzzle && strategy" class="strategy-content">
      <header class="page-heading strategy-heading">
        <div>
          <p class="eyebrow">PROVEN STRATEGY / {{ puzzle.id }}</p>
          <h1>{{ puzzle.version.title }} · 必胜推演</h1>
          <p>
            这是基于完整局面和当前求解器版本生成的严格证明结果。向下滚动查看不同应对分支。
          </p>
        </div>
        <PuzzleProofBadge
          :proof-status="puzzle.proofStatus"
          :winner-side="puzzle.winnerSide"
        />
      </header>

      <section class="panel strategy-summary">
        <div>
          <span class="eyebrow">证明结论</span>
          <h2>{{ winnerLabel }}</h2>
          <p>
            严格证明覆盖对手的所有合法应对；页面展示最多 5 条已验证代表路线，帮助理解在不同局面下应如何继续出牌。
          </p>
        </div>
        <dl>
          <div><dt>策略路线</dt><dd>{{ lines.length }} 条</dd></div>
          <div><dt>求解器</dt><dd>{{ strategy.result?.solverVersion || "—" }}</dd></div>
          <div><dt>规则版本</dt><dd>{{ strategy.result?.rulesetVersion || "—" }}</dd></div>
        </dl>
      </section>

      <section v-if="lines.length" class="strategy-routes" aria-label="多分支策略路线">
        <article v-for="line in lines" :key="line.lineNo" class="panel strategy-route">
          <header class="strategy-route__heading">
            <div>
              <span class="strategy-route__number">{{ line.lineNo }}</span>
              <div>
                <p class="eyebrow">VERIFIED BRANCH {{ line.lineNo }}</p>
                <h2>分支 {{ line.lineNo }}：对手采取不同应对时的推演</h2>
              </div>
            </div>
            <StatusBadge tone="success">已验证</StatusBadge>
          </header>
          <ol class="strategy-steps">
            <li v-for="(move, index) in line.moves" :key="`${line.lineNo}-${index}`">
              <span class="strategy-step__index">{{ index + 1 }}</span>
              <div class="strategy-step__content">
                <strong>{{ seatLabel(index) }}{{ index % 2 === 0 ? "行动" : "应对" }}</strong>
                <p>{{ moveDescription(move) }}</p>
              </div>
            </li>
          </ol>
        </article>
      </section>
      <section v-else class="panel empty-state">
        <h2>当前证明没有可展示的代表路线</h2>
        <p>根结论已经保存，但策略材料还未生成。稍后重新打开本页面即可继续查看。</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import AppHeader from "../components/AppHeader.vue";
import PuzzleProofBadge from "../components/PuzzleProofBadge.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import {
  getProofStrategy,
  getPuzzle,
  type Puzzle,
  type ProofStrategy,
} from "../api/modules/puzzle";
import type { Move } from "../api/modules/game";

const props = defineProps<{ id: string }>();
const id = props.id;
const puzzle = ref<Puzzle | null>(null);
const strategy = ref<ProofStrategy | null>(null);
const loading = ref(true);
const error = ref("");
const ranks = ["3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "小王", "大王"];
const typeLabels: Record<string, string> = {
  SINGLE: "单张",
  PAIR: "对子",
  TRIPLE: "三张",
  TRIPLE_SINGLE: "三带一",
  TRIPLE_PAIR: "三带二",
  STRAIGHT: "顺子",
  PAIR_STRAIGHT: "连对",
  AIRPLANE: "飞机",
  AIRPLANE_SINGLE: "飞机带单牌",
  AIRPLANE_PAIR: "飞机带对子",
  BOMB: "炸弹",
  ROCKET: "王炸",
  PASS: "不出",
};
const lines = computed(() => strategy.value?.result?.winningLines ?? []);
const winnerLabel = computed(() =>
  strategy.value?.result?.winnerSide === "USER" ? "用户必胜" : "机器人必胜",
);

function seatLabel(index: number): string {
  const firstSeat = puzzle.value?.version.initialState.firstSeat ?? 0;
  return (firstSeat + index) % 2 === 0 ? "USER" : "BOT";
}

function moveCards(move: Move): string[] {
  return move.cards.flatMap((count, index) =>
    Array.from({ length: count }, () => ranks[index] ?? String(index)),
  );
}

function moveDescription(move: Move): string {
  const type = typeLabels[move.type] || move.type;
  if (move.type === "PASS") return "选择不出，等待对手完成当前牌墩。";
  return `${type}：${moveCards(move).join("、")}。这是服务端规则引擎确认的合法行动。`;
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    puzzle.value = await getPuzzle(id);
    strategy.value = await getProofStrategy(puzzle.value.version.id);
  } catch (cause) {
    error.value = cause instanceof ApiError ? cause.message : "网络连接未建立。";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.detail-navigation {
  display: flex;
  gap: 10px;
  margin: 24px 0;
}
.strategy-content {
  padding-bottom: 60px;
}
.strategy-heading {
  align-items: flex-start;
}
.strategy-heading :deep(.proof-badge) {
  flex: 0 0 auto;
}
.strategy-summary {
  display: flex;
  justify-content: space-between;
  gap: 32px;
  margin-top: 24px;
  padding: 24px;
}
.strategy-summary h2 {
  margin: 8px 0;
  color: var(--accent);
}
.strategy-summary p {
  max-width: 720px;
  color: var(--muted);
  line-height: 1.7;
}
.strategy-summary dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(100px, 1fr));
  gap: 18px;
  min-width: 360px;
}
.strategy-summary dt {
  color: var(--muted);
  font-size: 12px;
}
.strategy-summary dd {
  margin-top: 6px;
  color: var(--text);
  font-weight: 700;
}
.strategy-routes {
  display: grid;
  gap: 20px;
  margin-top: 20px;
}
.strategy-route {
  padding: 24px;
}
.strategy-route__heading,
.strategy-route__heading > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}
.strategy-route__heading > div {
  justify-content: flex-start;
}
.strategy-route__heading h2 {
  margin: 4px 0 0;
  font-size: 20px;
}
.strategy-route__number {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  color: #06201f;
  background: var(--accent);
  font-weight: 800;
}
.strategy-steps {
  position: relative;
  display: grid;
  gap: 0;
  margin: 24px 0 0 20px;
  padding: 0;
  list-style: none;
}
.strategy-steps::before {
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 15px;
  width: 1px;
  background: rgba(104, 225, 201, 0.3);
  content: "";
}
.strategy-steps li {
  position: relative;
  display: flex;
  gap: 16px;
  padding: 12px 0;
}
.strategy-step__index {
  z-index: 1;
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border: 1px solid rgba(104, 225, 201, 0.5);
  border-radius: 50%;
  color: var(--accent);
  background: #071422;
  font-size: 12px;
  font-weight: 700;
}
.strategy-step__content strong {
  color: var(--accent);
  font-size: 13px;
}
.strategy-step__content p {
  margin: 5px 0 0;
  color: var(--text);
  line-height: 1.65;
}
.empty-state {
  margin-top: 24px;
  padding: 28px;
}
.empty-state p {
  color: var(--muted);
}
@media (max-width: 760px) {
  .strategy-summary {
    display: block;
  }
  .strategy-summary dl {
    min-width: 0;
    margin-top: 20px;
  }
  .strategy-route__heading,
  .strategy-route__heading > div {
    align-items: flex-start;
  }
}
</style>
