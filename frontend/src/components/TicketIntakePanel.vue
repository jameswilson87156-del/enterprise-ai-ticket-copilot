<script setup lang="ts">
import { reactive } from 'vue'
import type { CreateTicketRequest, TicketPriority } from '../types/ticket'

defineProps<{
  submitting: boolean
}>()

const emit = defineEmits<{
  submit: [payload: CreateTicketRequest]
}>()

const form = reactive<CreateTicketRequest>({
  title: '',
  description: '',
  systemName: 'order-center',
  errorLog: '',
  urgency: 'P2',
  requester: '员工自助提交',
  requesterDepartment: '内部支持'
})

const priorities: TicketPriority[] = ['P1', 'P2', 'P3']

const submit = () => {
  if (!form.title.trim() || !form.description.trim()) {
    return
  }
  emit('submit', { ...form })
  form.title = ''
  form.description = ''
  form.errorLog = ''
}
</script>

<template>
  <section class="intake-panel">
    <div class="panel-heading panel-heading--compact">
      <div>
        <p class="eyebrow">工单入口</p>
        <h2>新工单入口</h2>
      </div>
      <span class="human-lock">提交后分类</span>
    </div>
    <form class="intake-form" @submit.prevent="submit">
      <label>
        <span>标题</span>
        <input v-model="form.title" maxlength="180" placeholder="例：CRM 报表无法打开" />
      </label>
      <label>
        <span>系统</span>
        <input v-model="form.systemName" maxlength="96" placeholder="如：quote-center" />
      </label>
      <label>
        <span>问题描述</span>
        <textarea v-model="form.description" rows="4" placeholder="描述影响范围、出现时间和已尝试操作"></textarea>
      </label>
      <label>
        <span>错误日志</span>
        <textarea v-model="form.errorLog" rows="3" placeholder="可粘贴关键错误、traceId 或审计日志"></textarea>
      </label>
      <div class="intake-row">
        <label>
          <span>紧急程度</span>
          <select v-model="form.urgency">
            <option v-for="priority in priorities" :key="priority" :value="priority">{{ priority }}</option>
          </select>
        </label>
        <button class="primary-action" type="submit" :disabled="submitting">
          {{ submitting ? '提交中' : '提交并生成建议' }}
        </button>
      </div>
    </form>
  </section>
</template>
