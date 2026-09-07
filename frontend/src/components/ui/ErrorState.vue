<script setup lang="ts">
defineProps<{
  title?: string
  message: string
}>()
</script>

<template>
  <div class="error-state-modern" role="alert">
    <span class="error-state-modern__icon" aria-hidden="true">!</span>
    <div>
      <strong>{{ title ?? '请求未完成' }}</strong>
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so error messaging stays compact and safe. */
</style>
