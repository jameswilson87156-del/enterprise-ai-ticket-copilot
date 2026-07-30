import {
  TraceEvidenceRequestError,
  type TraceEvidenceResponse
} from '../api/tickets'

export const DEMO_SAMPLE_LABEL = 'Demo sample — backend not called'

export type RealRunEvidenceState =
  | 'idle'
  | 'loading'
  | 'backend-unavailable'
  | 'no-immutable-run'
  | 'immutable-run'
  | 'legacy-derived'
  | 'request-error'

export interface RealRunEvidenceViewModel {
  state: RealRunEvidenceState
  label: string
  evidence: TraceEvidenceResponse | null
  detail?: string
}

export function toRealRunEvidenceViewModel(
  evidence: TraceEvidenceResponse | null
): RealRunEvidenceViewModel {
  if (evidence === null) {
    return {
      state: 'no-immutable-run',
      label: 'No immutable run',
      evidence: null,
      detail: 'The read-only endpoint returned no persisted run evidence.'
    }
  }
  if (evidence.evidenceSource === 'IMMUTABLE_RUN' && evidence.copilotRun) {
    return {
      state: 'immutable-run',
      label: 'IMMUTABLE_RUN',
      evidence
    }
  }
  if (evidence.evidenceSource === 'LEGACY_DERIVED') {
    return {
      state: 'legacy-derived',
      label: 'LEGACY_DERIVED',
      evidence,
      detail: 'Legacy evidence is derived from mutable ticket records and is not an immutable run snapshot.'
    }
  }
  return {
    state: 'no-immutable-run',
    label: 'No immutable run',
    evidence,
    detail: 'The response did not include a complete immutable copilot run.'
  }
}

export function toRealRunEvidenceError(error: unknown): RealRunEvidenceViewModel {
  if (error instanceof TraceEvidenceRequestError) {
    const backendUnavailable = error.category === 'backend-unavailable'
    return {
      state: backendUnavailable ? 'backend-unavailable' : 'request-error',
      label: backendUnavailable ? 'Backend unavailable' : error.userLabel,
      evidence: null,
      detail: error.message
    }
  }
  return {
    state: 'request-error',
    label: 'Request error',
    evidence: null,
    detail: 'Unable to load trace evidence.'
  }
}
