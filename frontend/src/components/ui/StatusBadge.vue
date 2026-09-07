<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  tone?: 'success' | 'info' | 'warning' | 'danger' | 'neutral' | 'violet'
  compact?: boolean
}>(), {
  tone: 'neutral',
  compact: false
})
</script>

<template>
  <span class="status-badge" :class="[`status-badge--${tone}`, { 'status-badge--compact': compact }]">
    <span class="status-badge__dot" aria-hidden="true"></span>
    {{ label }}
  </span>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so status semantics are shared across pages. */
</style>
