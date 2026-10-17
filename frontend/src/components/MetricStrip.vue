<script setup lang="ts">
interface Metric {
  label: string
  value: string
  delta: string
  tone: 'warm' | 'mint' | 'steel' | 'danger' | 'knowledge'
}

defineProps<{
  metrics: Metric[]
}>()
</script>

<template>
  <section class="metric-strip" aria-label="关键指标">
    <article v-for="metric in metrics" :key="metric.label" class="metric-cell" :class="`metric-cell--${metric.tone}`">
      <div class="metric-label">{{ metric.label }}</div>
      <div class="metric-row">
        <strong>{{ metric.value }}</strong>
        <span>{{ metric.delta }}</span>
      </div>
    </article>
  </section>
</template>
