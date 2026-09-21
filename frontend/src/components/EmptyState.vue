<template>
  <section class="empty-state" aria-live="polite">
    <div class="empty-mark">◇</div>
    <h2>{{ title }}</h2>
    <p>{{ description }}</p>
    <button
      v-if="actionText"
      class="button button-secondary"
      type="button"
      @click="$emit('action')"
    >
      {{ actionText }}
    </button>
  </section>
</template>
<script setup lang="ts">
defineProps<{ title: string; description: string; actionText?: string }>();
defineEmits<{ action: [] }>();
</script>
