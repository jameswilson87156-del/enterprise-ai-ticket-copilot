import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import {
  fetchTraceEvidence,
  TraceEvidenceRequestError,
  type TraceEvidenceResponse
} from '../src/api/tickets.ts'
import {
  DEMO_SAMPLE_LABEL,
  toRealRunEvidenceError,
  toRealRunEvidenceViewModel
} from '../src/features/realRunEvidence.ts'

function immutableEvidence(): TraceEvidenceResponse {
  return {
    ticketId: 'TCK-SYNTHETIC-001',
    runId: 'RUN-SYNTHETIC-001',
    traceId: 'TRACE-SYNTHETIC-001',
    traceMode: 'immutable-copilot-run-trace',
    evidenceSource: 'IMMUTABLE_RUN',
    ragReferences: [],
    validatedCitations: [],
    copilotRun: {
      runId: 'RUN-SYNTHETIC-001',
      runStatus: 'SUCCESS',
      actualProvider: 'local-rule',
      fallbackUsed: false,
      fallbackReasonCode: null,
      errorCategory: 'NONE',
      outputProduced: true,
      humanReviewRequired: true,
      retrievalHitCount: 0
    },
    structuredOutput: {
      outputValidationStatus: 'VALID',
      citationValidationStatus: 'VALID',
      abstained: false,
      abstentionReasonCode: 'NONE',
      finalHumanReviewRequired: true,
      validCitationCount: 0,
      rejectedCitationCount: 0
    },
    humanReview: null,
    reviewRecords: []
  }
}

test('API success maps IMMUTABLE_RUN without adding an Authorization header', async () => {
  const originalFetch = globalThis.fetch
  let requestInit: RequestInit | undefined
  globalThis.fetch = async (_input, init) => {
    requestInit = init
    return new Response(JSON.stringify(immutableEvidence()), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    })
  }
  try {
    const result = await fetchTraceEvidence('TCK-SYNTHETIC-001')
    const model = toRealRunEvidenceViewModel(result)
    assert.equal(model.state, 'immutable-run')
    assert.equal(model.label, 'IMMUTABLE_RUN')
    assert.equal(model.evidence?.runId, 'RUN-SYNTHETIC-001')
    assert.equal(new Headers(requestInit?.headers).has('Authorization'), false)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('network failure is labeled with a generic backend-unavailable message', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => {
    throw new TypeError('private network detail')
  }
  try {
    const error = await fetchTraceEvidence('TCK-SYNTHETIC-001').catch((caught: unknown) => caught)
    assert.ok(error instanceof TraceEvidenceRequestError)
    assert.equal(error.message, 'The trace evidence backend is unavailable.')
    assert.equal(error.message.includes('private network detail'), false)

    const model = toRealRunEvidenceError(error)
    assert.equal(model.state, 'backend-unavailable')
    assert.equal(model.label, 'Backend unavailable')
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('HTTP error body is never exposed to the evidence UI model', async () => {
  const originalFetch = globalThis.fetch
  const privateResponseBody = 'internal exception: database host and stack trace'
  globalThis.fetch = async () => new Response(privateResponseBody, { status: 401 })
  try {
    const error = await fetchTraceEvidence('TCK-SYNTHETIC-001').catch((caught: unknown) => caught)
    assert.ok(error instanceof TraceEvidenceRequestError)
    assert.equal(error.status, 401)
    assert.equal(error.category, 'authentication-required')
    assert.equal(error.message.includes(privateResponseBody), false)

    const model = toRealRunEvidenceError(error)
    assert.equal(model.label, 'Authentication required')
    assert.equal(model.detail?.includes(privateResponseBody), false)
  } finally {
    globalThis.fetch = originalFetch
  }
})

test('unknown client errors are replaced with a generic UI message', () => {
  const model = toRealRunEvidenceError(new Error('private browser detail'))
  assert.equal(model.state, 'request-error')
  assert.equal(model.detail, 'Unable to load trace evidence.')
})

test('public panel contains no manual bearer-token control', () => {
  const source = readFileSync(
    new URL('../src/components/RealRunEvidencePanel.vue', import.meta.url),
    'utf8'
  )
  assert.equal(source.includes('const bearerToken'), false)
  assert.equal(source.includes('v-model="bearerToken"'), false)
  assert.equal(source.includes('type="password"'), false)
  assert.equal(source.includes('真实回放仅用于本地已认证演示'), true)
})

test('demo fallback label cannot be presented as a real backend run', () => {
  assert.equal(DEMO_SAMPLE_LABEL, 'Demo sample — backend not called')
  assert.equal(DEMO_SAMPLE_LABEL.includes('backend not called'), true)
  assert.equal(DEMO_SAMPLE_LABEL.includes('IMMUTABLE_RUN'), false)
})
