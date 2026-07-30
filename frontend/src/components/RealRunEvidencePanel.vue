<script setup lang="ts">
import { computed, ref, shallowRef } from 'vue'
import { fetchTraceEvidence } from '../api/tickets'
import {
  DEMO_SAMPLE_LABEL,
  toRealRunEvidenceError,
  toRealRunEvidenceViewModel,
  type RealRunEvidenceViewModel
} from '../features/realRunEvidence'

const ticketId = ref('')
const model = shallowRef<RealRunEvidenceViewModel>({
  state: 'idle',
  label: 'Ready',
  evidence: null,
  detail: 'Enter a ticket number to replay its read-only backend evidence.'
})

const evidence = computed(() => model.value.evidence)
const isLoading = computed(() => model.value.state === 'loading')

async function loadEvidence() {
  const normalizedTicketId = ticketId.value.trim()
  if (!normalizedTicketId) {
    model.value = {
      state: 'request-error',
      label: 'Request error',
      evidence: null,
      detail: 'Ticket number is required.'
    }
    return
  }

  model.value = {
    state: 'loading',
    label: 'Loading',
    evidence: null,
    detail: 'Reading the existing trace snapshot. This GET does not run Copilot or call a Provider.'
  }
  try {
    model.value = toRealRunEvidenceViewModel(await fetchTraceEvidence(normalizedTicketId))
  } catch (error) {
    model.value = toRealRunEvidenceError(error)
  }
}
</script>

<template>
  <section class="real-run-evidence" aria-labelledby="real-run-evidence-title">
    <header class="real-run-evidence__header">
      <div>
        <span>Local authenticated read-only replay</span>
        <h2 id="real-run-evidence-title">Real Run Evidence</h2>
        <p>Loads sanitized evidence from <code>GET /api/tickets/{id}/trace-evidence</code>.</p>
      </div>
      <strong :data-state="model.state">{{ model.label }}</strong>
    </header>

    <form class="real-run-evidence__form" @submit.prevent="loadEvidence">
      <label>
        Ticket number
        <input v-model="ticketId" autocomplete="off" placeholder="TCK-..." />
      </label>
      <button type="submit" :disabled="isLoading">{{ isLoading ? 'Loading…' : 'Replay evidence' }}</button>
    </form>

    <p class="real-run-evidence__auth-note">
      真实回放仅用于本地已认证演示。公开作品集页面不接收、保存或发送手动 Bearer Token，
      也不绕过后端既有鉴权；未建立本地认证上下文时仅显示安全状态。
    </p>

    <section v-if="model.state !== 'immutable-run'" class="real-run-evidence__state" :data-state="model.state">
      <strong>{{ model.label }}</strong>
      <p>{{ model.detail }}</p>
    </section>

    <template v-else-if="evidence">
      <dl class="real-run-evidence__summary">
        <div><dt>Run ID</dt><dd>{{ evidence.runId }}</dd></div>
        <div><dt>Evidence source</dt><dd>{{ evidence.evidenceSource }}</dd></div>
        <div><dt>Run status</dt><dd>{{ evidence.copilotRun?.runStatus ?? 'not recorded' }}</dd></div>
        <div><dt>Actual provider</dt><dd>{{ evidence.copilotRun?.actualProvider ?? 'not recorded' }}</dd></div>
        <div><dt>Replay calls Provider</dt><dd>No — read-only GET only</dd></div>
        <div><dt>Fallback</dt><dd>{{ evidence.copilotRun?.fallbackUsed ? (evidence.copilotRun.fallbackReasonCode || 'used') : 'not used' }}</dd></div>
        <div><dt>Output validation</dt><dd>{{ evidence.structuredOutput?.outputValidationStatus ?? 'not recorded' }}</dd></div>
        <div><dt>Abstention</dt><dd>{{ evidence.structuredOutput?.abstained ? (evidence.structuredOutput.abstentionReasonCode || 'yes') : 'no' }}</dd></div>
        <div><dt>Human review</dt><dd>{{ evidence.humanReview?.decision || evidence.humanReview?.reviewStatus || 'not recorded' }}</dd></div>
      </dl>

      <div class="real-run-evidence__columns">
        <section>
          <h3>Retrieval hits <span>{{ evidence.ragReferences.length }}</span></h3>
          <p v-if="!evidence.ragReferences.length">No retrieval hit was persisted for this run.</p>
          <article v-for="hit in evidence.ragReferences" :key="`${hit.linkedRunId}-${hit.articleNo}`">
            <strong>{{ hit.articleNo }} · {{ hit.knowledgeTitle }}</strong>
            <small>{{ hit.sourcePath }} · score {{ hit.relevanceScore }} · {{ hit.usedInDraft ? 'used in draft' : 'not used' }}</small>
            <p>{{ hit.snippet }}</p>
          </article>
        </section>

        <section>
          <h3>Validated citations <span>{{ evidence.validatedCitations.length }}</span></h3>
          <p v-if="!evidence.validatedCitations.length">No validated citation was persisted for this run.</p>
          <article v-for="citation in evidence.validatedCitations" :key="citation.resultCitationId">
            <strong>{{ citation.knowledgeArticleId }} · {{ citation.knowledgeTitle }}</strong>
            <small>{{ citation.citationType }}</small>
            <p>{{ citation.evidenceExcerpt }}</p>
          </article>
        </section>
      </div>
    </template>

    <footer>{{ DEMO_SAMPLE_LABEL }}. The showcase trace below remains synthetic UI data and is not this backend replay.</footer>
  </section>
</template>

<style scoped>
.real-run-evidence {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
  border: 1px solid rgba(33, 199, 217, 0.28);
  border-radius: 8px;
  padding: 14px;
  color: #eef5ff;
  background: linear-gradient(145deg, rgba(10, 28, 43, 0.96), rgba(6, 14, 27, 0.98));
}

.real-run-evidence__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.real-run-evidence__header span,
.real-run-evidence__header p,
.real-run-evidence__auth-note,
.real-run-evidence small,
.real-run-evidence footer {
  color: #8fa3bd;
}

.real-run-evidence h2,
.real-run-evidence h3,
.real-run-evidence p,
.real-run-evidence dl {
  margin: 0;
}

.real-run-evidence h2 {
  margin-top: 3px;
  font-size: 20px;
}

.real-run-evidence__header > strong {
  align-self: start;
  border: 1px solid rgba(61, 124, 255, 0.34);
  border-radius: 6px;
  padding: 6px 8px;
  color: #bcd5ff;
  font: 800 11px/1.2 "Cascadia Code", Consolas, monospace;
}

.real-run-evidence__header > strong[data-state='immutable-run'] {
  border-color: rgba(43, 216, 143, 0.34);
  color: #7ce9b9;
}

.real-run-evidence__form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) auto;
  gap: 8px;
  align-items: end;
}

.real-run-evidence__form label {
  display: grid;
  gap: 5px;
  color: #b8c8dc;
  font-size: 11px;
  font-weight: 800;
}

.real-run-evidence__form input,
.real-run-evidence__form button {
  min-height: 36px;
  border: 1px solid rgba(151, 180, 214, 0.2);
  border-radius: 6px;
  padding: 0 10px;
  color: #eef5ff;
  background: rgba(4, 10, 20, 0.76);
  font: inherit;
}

.real-run-evidence__form button {
  border-color: rgba(33, 199, 217, 0.36);
  cursor: pointer;
}

.real-run-evidence__form button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.real-run-evidence__auth-note,
.real-run-evidence footer {
  font-size: 11px;
  line-height: 1.45;
}

.real-run-evidence code,
.real-run-evidence dd {
  font-family: "Cascadia Code", Consolas, monospace;
}

.real-run-evidence__state {
  border: 1px dashed rgba(151, 180, 214, 0.24);
  border-radius: 7px;
  padding: 12px;
}

.real-run-evidence__state p {
  margin-top: 5px;
  color: #8fa3bd;
  font-size: 12px;
}

.real-run-evidence__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 7px;
  background: rgba(151, 180, 214, 0.12);
}

.real-run-evidence__summary div {
  min-width: 0;
  padding: 9px;
  background: rgba(5, 13, 25, 0.9);
}

.real-run-evidence dt {
  color: #8298b4;
  font-size: 10px;
}

.real-run-evidence dd {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
  font-size: 11px;
}

.real-run-evidence__columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.real-run-evidence__columns > section {
  display: grid;
  align-content: start;
  gap: 7px;
  border: 1px solid rgba(151, 180, 214, 0.14);
  border-radius: 7px;
  padding: 10px;
}

.real-run-evidence h3 {
  font-size: 13px;
}

.real-run-evidence h3 span {
  color: #21c7d9;
}

.real-run-evidence article {
  display: grid;
  gap: 4px;
  border-top: 1px solid rgba(151, 180, 214, 0.1);
  padding-top: 7px;
}

.real-run-evidence article strong {
  font-size: 11px;
}

.real-run-evidence article p {
  color: #aebed2;
  font-size: 11px;
  line-height: 1.45;
}

.real-run-evidence footer {
  border-top: 1px solid rgba(255, 180, 92, 0.18);
  padding-top: 10px;
  color: #ffd5a3;
}

@media (max-width: 900px) {
  .real-run-evidence__form,
  .real-run-evidence__summary,
  .real-run-evidence__columns {
    grid-template-columns: 1fr;
  }
}
</style>
