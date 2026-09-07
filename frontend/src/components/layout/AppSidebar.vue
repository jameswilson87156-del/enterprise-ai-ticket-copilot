<script setup lang="ts">
import BrandMark from './BrandMark.vue'
import NavIcon from './NavIcon.vue'

interface SidebarItem {
  label: string
  caption: string
  route: string
  icon: string
}

defineProps<{
  activeRoute: string
  items: SidebarItem[]
  pendingReviewCount: number
  runtimeLabel: string
}>()

const emit = defineEmits<{
  navigate: [route: string]
}>()
</script>

<template>
  <aside class="app-sidebar" aria-label="主导航">
    <div class="app-sidebar__brand">
      <BrandMark />
      <div>
        <strong>Enterprise Ticket</strong>
        <span>支持工作台</span>
      </div>
    </div>

    <nav class="app-sidebar__nav" aria-label="工作区页面">
      <button
        v-for="item in items"
        :key="item.route"
        type="button"
        class="app-sidebar__nav-item"
        :class="{ 'app-sidebar__nav-item--active': activeRoute === item.route }"
        :aria-current="activeRoute === item.route ? 'page' : undefined"
        @click="emit('navigate', item.route)"
      >
        <span class="app-sidebar__nav-icon" aria-hidden="true"><NavIcon :name="item.icon" /></span>
        <span class="app-sidebar__nav-copy">
          <strong>{{ item.label }}</strong>
          <small>{{ item.caption }}</small>
        </span>
        <span v-if="item.route === 'human-review' && pendingReviewCount" class="app-sidebar__count">{{ pendingReviewCount }}</span>
      </button>
    </nav>

    <div class="app-sidebar__footer">
      <section class="workspace-card" aria-label="当前工作区">
        <div class="workspace-card__topline">
          <span class="status-dot status-dot--green" aria-hidden="true"></span>
          <span>当前工作区</span>
          <span class="workspace-card__mode">LOCAL</span>
        </div>
        <strong>支持团队</strong>
        <small>{{ runtimeLabel }}</small>
      </section>
      <p class="app-sidebar__boundary">合成数据与运行证据仅用于本地验收，不代表生产统计。</p>
    </div>
  </aside>
</template>

<style scoped>
/* Structure is intentionally kept in styles.css so the shell and responsive rules stay centralized. */
</style>
