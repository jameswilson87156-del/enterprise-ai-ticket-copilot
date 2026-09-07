<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <header class="page-header">
    <div class="page-header__copy">
      <p class="page-eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions || $slots.meta" class="page-header__actions">
      <slot name="meta" />
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so page primitives share one responsive system. */
</style>
