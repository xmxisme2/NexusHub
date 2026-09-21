<template>
  <StatusBadge :tone="proven ? 'success' : 'warning'">
    {{
      proven
        ? `已证明 · ${winnerSide === "USER" ? "用户" : "机器人"}必胜`
        : "尚未证明"
    }}
  </StatusBadge>
</template>

<script setup lang="ts">
import { computed } from "vue";
import StatusBadge from "./StatusBadge.vue";
import type { Puzzle } from "../api/modules/puzzle";

/** 仅展示服务端严格证明且胜方完整的结论，生命周期状态另行展示。 */
const props = defineProps<Pick<Puzzle, "proofStatus" | "winnerSide">>();
const proven = computed(
  () =>
    props.proofStatus === "PROVEN" &&
    (props.winnerSide === "USER" || props.winnerSide === "BOT"),
);
</script>
