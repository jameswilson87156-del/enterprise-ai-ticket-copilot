<script setup lang="ts">
import AuthControl from '../auth/AuthControl.vue'

defineProps<{
  eyebrow: string
  title: string
  runtimeLabel: string
  runtimeTone: 'demo' | 'real' | 'error' | 'checking'
  providerLabel: string
  providerTone: 'connected' | 'fallback' | 'unknown'
}>()

const emit = defineEmits<{
  openSearch: []
}>()
</script>

<template>
  <header class="app-topbar">
    <button type="button" class="app-topbar__search" aria-label="搜索工单、知识和运行记录" @click="emit('openSearch')">
      <span class="app-topbar__search-icon" aria-hidden="true">
        <svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" focusable="false">
          <circle cx="8.75" cy="8.75" r="4.85" />
          <path d="m12.35 12.35 4 4" />
        </svg>
      </span>
      <span>搜索工单、知识和运行记录…</span>
      <kbd>Ctrl K</kbd>
    </button>
    <div class="app-topbar__context">
      <div class="app-topbar__route">
        <span>{{ eyebrow }}</span>
        <strong>{{ title }}</strong>
      </div>
      <span class="topbar-badge" :class="`topbar-badge--${runtimeTone}`">
        <span class="status-dot" aria-hidden="true"></span>
        {{ runtimeLabel }}
      </span>
      <span class="topbar-provider" :class="`topbar-provider--${providerTone}`">
        <span class="status-dot" aria-hidden="true"></span>
        Provider · {{ providerLabel }}
      </span>
      <AuthControl />
      <span class="topbar-avatar" aria-label="当前用户 Admin">ET</span>
    </div>
  </header>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so the shell and responsive rules stay centralized. */
</style>
