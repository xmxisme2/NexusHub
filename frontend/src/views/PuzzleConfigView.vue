<template>
  <main class="app-shell">
    <AppHeader />
    <section class="page-heading">
      <div>
        <p class="eyebrow">PUZZLE CONFIG / CLASSIC_V1</p>
        <h1>{{ isEdit ? "编辑残局草稿" : "新建残局" }}</h1>
        <p>
          配置双方完全明牌手牌；保存后形成不可变版本，服务端会再次校验规则。
        </p>
      </div>
      <RouterLink class="button button-secondary" to="/landlord/puzzles"
        >返回残局库</RouterLink
      >
    </section>
    <form
      class="puzzle-config"
      @submit.prevent="save"
      @invalid.capture="handleInvalid"
    >
      <section class="panel puzzle-config__meta">
        <div class="config-grid">
          <label
            >标题<input
              v-model.trim="title"
              required
              maxlength="120"
              placeholder="例如：L005 · 双王抢先" /></label
          ><label
            >难度<select v-model="difficulty">
              <option value="EASY">简单</option>
              <option value="MEDIUM">中等</option>
              <option value="HARD">困难</option>
            </select></label
          ><label class="config-grid__wide"
            >描述<textarea
              v-model.trim="description"
              maxlength="2000"
              rows="3"
              placeholder="说明这个残局的训练目标"
            /></label
          ><label class="config-grid__wide"
            >标签<input
              v-model="tagsText"
              maxlength="160"
              placeholder="用逗号分隔，例如：单张,先手,基础"
          /></label>
        </div>
      </section>
      <section class="panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">VISIBLE HANDS</p>
            <h2>双方手牌</h2>
          </div>
          <span class="rules-note">每张点数合计不超过牌库上限</span>
        </div>
        <div class="hand-config-grid">
          <section
            v-for="(seat, seatIndex) in ['USER', 'BOT']"
            :key="seat"
            class="hand-config"
          >
            <div class="hand-config__head">
              <div>
                <span class="seat-label">{{ seat }}</span
                ><strong>{{ totals[seatIndex] }} 张</strong>
              </div>
              <StatusBadge :tone="totals[seatIndex] ? 'success' : 'warning'">{{
                seatIndex === firstSeat ? "先手" : "后手"
              }}</StatusBadge>
            </div>
            <div class="rank-grid">
              <label v-for="(rank, rankIndex) in ranks" :key="rank"
                ><span>{{ rank }}</span
                ><input
                  type="number"
                  min="0"
                  :max="rankIndex >= 13 ? 1 : 4"
                  :value="hands[seatIndex][rankIndex]"
                  @input="changeCount(seatIndex as 0 | 1, rankIndex, $event)"
              /></label>
            </div>
          </section>
        </div>
      </section>
      <section class="panel puzzle-config__footer">
        <div>
          <label class="first-seat-label"
            >先手<select v-model.number="firstSeat">
              <option :value="0">USER</option>
              <option :value="1">BOT</option>
            </select></label
          >
          <p class="config-hint">
            USER 最多 20 张，BOT 最多 17 张；双方都不能为空。
          </p>
        </div>
        <div class="config-actions">
          <button
            class="button button-secondary"
            type="button"
            @click="validate"
          >
            校验局面</button
          ><button class="button" type="submit" :disabled="saving">
            {{ saving ? "保存中…" : "保存残局" }}
          </button>
        </div>
      </section>
      <p v-if="message" class="validation-result" :class="{ invalid: !valid }">
        {{ message
        }}<RouterLink
          v-if="duplicate"
          class="text-link validation-result__link"
          :to="`/landlord/puzzles/${duplicate.id}`"
          >查看已有残局</RouterLink
        >
      </p>
    </form>
  </main>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import AppHeader from "../components/AppHeader.vue";
import StatusBadge from "../components/StatusBadge.vue";
import { ApiError } from "../api/http";
import {
  checkPuzzleDuplicate,
  getPuzzle,
  RULESET_VERSION,
  savePuzzle,
  validatePuzzle,
  type Puzzle,
  type PuzzleDifficulty,
  type GameState,
  type PuzzleDuplicate,
} from "../api/modules/puzzle";

const props = defineProps<{ id?: string }>();
const router = useRouter();
const isEdit = computed(() => Boolean(props.id));
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
const title = ref("");
const description = ref("");
const tagsText = ref("");
const difficulty = ref<PuzzleDifficulty>("EASY");
const firstSeat = ref<0 | 1>(0);
const hands = ref<number[][]>([Array(15).fill(0), Array(15).fill(0)]);
const rowVersion = ref(0);
const saving = ref(false);
const message = ref("");
const valid = ref(false);
const duplicate = ref<PuzzleDuplicate | null>(null);
const totals = computed(() =>
  hands.value.map((hand) => hand.reduce((sum, value) => sum + value, 0)),
);
function changeCount(seat: 0 | 1, rank: number, event: Event) {
  const value = Math.max(
    0,
    Math.min(
      rank >= 13 ? 1 : 4,
      Number((event.target as HTMLInputElement).value) || 0,
    ),
  );
  hands.value[seat][rank] = value;
}
function state(): GameState {
  return {
    schemaVersion: 1,
    rulesetVersion: RULESET_VERSION,
    hands: hands.value.map((hand) => [...hand]),
    firstSeat: firstSeat.value,
    currentSeat: firstSeat.value,
    isFirstMove: true,
    targetMove: null,
    lastPlaySeat: null,
    consecutivePasses: 0,
  };
}
function tags() {
  return tagsText.value
    .split(",")
    .map((tag) => tag.trim())
    .filter(Boolean);
}
function handleInvalid(event: Event) {
  const field = event.target as
    | HTMLInputElement
    | HTMLSelectElement
    | HTMLTextAreaElement;
  valid.value = false;
  message.value = field.validity.valueMissing
    ? "请输入残局标题后再保存。"
    : "请检查表单输入是否符合要求。";
}
async function validate() {
  message.value = "";
  duplicate.value = null;
  try {
    const result = await validatePuzzle(state());
    valid.value = result.valid;
    message.value = result.valid
      ? "服务端校验通过，可以保存。"
      : result.errors[0]?.message || "局面不符合规则。";
  } catch (cause) {
    valid.value = false;
    message.value =
      cause instanceof ApiError ? cause.message : "暂时无法校验局面。";
  }
}
async function save() {
  duplicate.value = null;
  if (!title.value.trim()) {
    valid.value = false;
    message.value = "请输入残局标题后再保存。";
    return;
  }
  await validate();
  if (!valid.value) return;
  saving.value = true;
  try {
    const puzzleState = state();
    const duplicateResult = await checkPuzzleDuplicate(puzzleState, props.id);
    if (duplicateResult.duplicate && duplicateResult.existing) {
      duplicate.value = duplicateResult.existing;
      valid.value = false;
      message.value = duplicateResult.existing.deleted
        ? `已有相同的已删除残局「${duplicateResult.existing.title}」（ID ${duplicateResult.existing.id}），可复用原验证结果。`
        : `已有相同残局「${duplicateResult.existing.title}」（ID ${duplicateResult.existing.id}），无需重复创建。`;
      return;
    }
    const result = await savePuzzle({
      id: props.id,
      expectedRowVersion: isEdit.value ? rowVersion.value : undefined,
      title: title.value,
      description: description.value,
      tags: tags(),
      difficulty: difficulty.value,
      allowedFirstSeats: [firstSeat.value],
      state: puzzleState,
    });
    await router.push(`/landlord/puzzles/${result.id}`);
  } catch (cause) {
    valid.value = false;
    message.value =
      cause instanceof ApiError ? cause.message : "保存失败，请稍后重试。";
  } finally {
    saving.value = false;
  }
}
onMounted(async () => {
  if (!props.id) return;
  try {
    const puzzle: Puzzle = await getPuzzle(props.id);
    title.value = puzzle.version.title;
    description.value = puzzle.version.description;
    tagsText.value = puzzle.version.tags.join(",");
    difficulty.value = puzzle.version.difficulty;
    firstSeat.value = (puzzle.version.allowedFirstSeats[0] ??
      puzzle.version.initialState.firstSeat) as 0 | 1;
    hands.value = puzzle.version.initialState.hands.map((hand) => [...hand]);
    rowVersion.value = puzzle.rowVersion;
  } catch (cause) {
    message.value =
      cause instanceof ApiError ? cause.message : "无法读取残局草稿。";
  }
});
</script>
