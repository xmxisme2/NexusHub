<template>
  <button
    v-if="selectable"
    class="poker-card"
    :class="[
      { 'poker-card--selected': selected, 'poker-card--compact': compact },
      { 'poker-card--red': isRed },
    ]"
    type="button"
    :aria-pressed="selected"
    :aria-label="`${label}${suit}`"
    @click="$emit('toggle')"
  >
    <span class="poker-card__corner">{{ label }}</span
    ><span class="poker-card__suit">{{ suit }}</span>
  </button>
  <span
    v-else
    class="poker-card"
    :class="[{ 'poker-card--compact': compact }, { 'poker-card--red': isRed }]"
    :aria-label="`${label}${suit}`"
  >
    <span class="poker-card__corner">{{ label }}</span
    ><span class="poker-card__suit">{{ suit }}</span>
  </span>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { RANK_LABELS } from "../utils/cards";
const props = withDefaults(
  defineProps<{
    rankIndex: number;
    copyIndex?: number;
    selected?: boolean;
    selectable?: boolean;
    compact?: boolean;
  }>(),
  { copyIndex: 0, selected: false, selectable: false, compact: false },
);
defineEmits<{ toggle: [] }>();
const label = computed(() => RANK_LABELS[props.rankIndex] ?? "?");
const isJoker = computed(() => props.rankIndex >= 13);
const suit = computed(() =>
  isJoker.value
    ? props.rankIndex === 14
      ? "★"
      : "☆"
    : ["♠", "♥", "♣", "♦"][props.copyIndex % 4],
);
const isRed = computed(
  () =>
    props.rankIndex === 14 ||
    (!isJoker.value &&
      (props.copyIndex % 4 === 1 || props.copyIndex % 4 === 3)),
);
</script>
