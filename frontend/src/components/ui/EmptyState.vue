<script setup lang="ts">
withDefaults(defineProps<{
  title: string
  description: string
  actionLabel?: string
}>(), {
  actionLabel: ''
})

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state-modern" role="status">
    <span class="empty-state-modern__icon" aria-hidden="true">∅</span>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <button v-if="actionLabel" type="button" class="button button--secondary button--small" @click="emit('action')">{{ actionLabel }}</button>
  </div>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so empty states do not look like errors. */
</style>
