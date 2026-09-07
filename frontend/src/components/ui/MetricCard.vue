<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  note: string
  tone?: 'blue' | 'cyan' | 'green' | 'amber' | 'red' | 'violet'
  link?: string
}>(), {
  tone: 'blue',
  link: ''
})
</script>

<template>
  <article class="metric-card" :class="`metric-card--${tone}`">
    <div class="metric-card__label">
      <span class="metric-card__signal" aria-hidden="true"></span>
      <span>{{ label }}</span>
    </div>
    <strong>{{ value }}</strong>
    <p>{{ note }}</p>
    <a v-if="link" :href="link">查看详情 <span aria-hidden="true">→</span></a>
  </article>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so metric cards use one token set. */
</style>
