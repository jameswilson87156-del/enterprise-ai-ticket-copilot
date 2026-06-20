<script setup lang="ts">
type FilterValue = 'ALL' | 'PENDING' | 'REVIEW' | 'KNOWLEDGE'

defineProps<{
  keyword: string
  filter: FilterValue
}>()

const emit = defineEmits<{
  'update:keyword': [value: string]
  'update:filter': [value: FilterValue]
}>()

const filters: Array<{ label: string; value: FilterValue }> = [
  { label: '全部', value: 'ALL' },
  { label: '待处理', value: 'PENDING' },
  { label: '待确认', value: 'REVIEW' },
  { label: '已沉淀', value: 'KNOWLEDGE' }
]
</script>

<template>
  <section class="search-filter-bar" aria-label="搜索与筛选">
    <label class="search-field">
      <span>搜索</span>
      <input
        :value="keyword"
        type="search"
        placeholder="搜索工单、问题描述、知识库关键词"
        @input="emit('update:keyword', ($event.target as HTMLInputElement).value)"
      />
    </label>

    <div class="filter-tabs" aria-label="工单状态筛选">
      <button
        v-for="item in filters"
        :key="item.value"
        class="filter-tab"
        :class="{ 'filter-tab--active': filter === item.value }"
        type="button"
        @click="emit('update:filter', item.value)"
      >
        {{ item.label }}
      </button>
    </div>
  </section>
</template>
