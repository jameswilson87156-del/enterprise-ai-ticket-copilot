<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  description?: string
  count?: string | number
}>()
</script>

<template>
  <header class="panel-header">
    <div>
      <p v-if="eyebrow" class="panel-header__eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description" class="panel-header__description">{{ description }}</p>
    </div>
    <div class="panel-header__tools">
      <span v-if="count !== undefined" class="panel-count">{{ count }}</span>
      <slot />
    </div>
  </header>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so panel headers remain consistent. */
</style>
