<template>
  <span class="status-badge" :class="`status-${tone}`"><slot /></span>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{ tone?: "success" | "warning" | "muted" | "info" | "danger" }>(),
  { tone: "info" },
);
</script>
