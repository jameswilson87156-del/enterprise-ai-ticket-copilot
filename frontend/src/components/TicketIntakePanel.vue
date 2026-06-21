<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { CreateTicketRequest, TicketPriority } from '../types/ticket'

const props = defineProps<{
  submitting: boolean
  resetVersion: number
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
const validationMessage = ref('')

const resetForm = () => {
  form.title = ''
  form.description = ''
  form.errorLog = ''
  validationMessage.value = ''
}

watch(() => props.resetVersion, resetForm)

const submit = () => {
  if (!form.title.trim() || !form.description.trim()) {
    validationMessage.value = '请填写标题和问题描述后再提交。'
    return
  }
  validationMessage.value = ''
  emit('submit', { ...form })
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
      <label for="ticket-title">
        <span>标题</span>
        <input
          id="ticket-title"
          v-model="form.title"
          maxlength="180"
          placeholder="例：CRM 报表无法打开"
          required
          :aria-invalid="Boolean(validationMessage && !form.title.trim())"
          aria-describedby="ticket-intake-message"
        />
      </label>
      <label for="ticket-system">
        <span>系统</span>
        <input id="ticket-system" v-model="form.systemName" maxlength="96" placeholder="如：quote-center" />
      </label>
      <label for="ticket-description">
        <span>问题描述</span>
        <textarea
          id="ticket-description"
          v-model="form.description"
          rows="4"
          placeholder="描述影响范围、出现时间和已尝试操作"
          required
          :aria-invalid="Boolean(validationMessage && !form.description.trim())"
          aria-describedby="ticket-intake-message"
        ></textarea>
      </label>
      <label for="ticket-error-log">
        <span>错误日志</span>
        <textarea id="ticket-error-log" v-model="form.errorLog" rows="3" placeholder="可粘贴关键错误、traceId 或审计日志"></textarea>
      </label>
      <div class="intake-row">
        <label for="ticket-priority">
          <span>紧急程度</span>
          <select id="ticket-priority" v-model="form.urgency">
            <option v-for="priority in priorities" :key="priority" :value="priority">{{ priority }}</option>
          </select>
        </label>
        <button class="primary-action" type="submit" :disabled="submitting">
          {{ submitting ? '提交中' : '提交并生成建议' }}
        </button>
      </div>
      <p
        id="ticket-intake-message"
        class="form-message"
        :class="{ 'form-message--error': validationMessage }"
        role="status"
        aria-live="polite"
      >
        {{ validationMessage || '标题和问题描述为必填项；Demo 模式只写入当前浏览器内存。' }}
      </p>
    </form>
  </section>
</template>
