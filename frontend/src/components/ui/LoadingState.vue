<script setup lang="ts">
withDefaults(defineProps<{
  label?: string
  lines?: number
}>(), {
  label: '加载中',
  lines: 3
})
</script>

<template>
  <div class="loading-state" role="status" aria-live="polite">
    <span class="loading-state__label">{{ label }}</span>
    <span v-for="line in lines" :key="line" class="loading-state__line" :class="{ 'loading-state__line--short': line === lines }"></span>
  </div>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so all async regions share one skeleton treatment. */
</style>
