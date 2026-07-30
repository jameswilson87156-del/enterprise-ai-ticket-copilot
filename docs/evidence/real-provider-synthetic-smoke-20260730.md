# Real Provider Synthetic Smoke Evidence

> [!WARNING]
> **Historical / Superseded evidence.** This smoke was captured before Phase 4. At that time, Provider citation validation and immutable run snapshots were not implemented. Preserve this file as timeline evidence, but use `real-structured-output-smoke-20260730.md` and `real-structured-output-smoke-20260730.json` for the current structured-output, citation-validation, and `IMMUTABLE_RUN` capability record.

## Scope

- This evidence covers synthetic demonstration data only.
- It does not include production data.
- It does not represent production stability, availability, or SLA.
- It does not represent validated Provider citations.
- It does not represent a complete distributed trace implementation.

## Configuration Boundary

- Shared environment mapping enabled.
- Project-specific variables have priority.
- Chat Completions Adapter used by Ticket Copilot.
- Provider protocol compatibility externally verified for both Chat Completions and Responses.
- Secrets are not stored in repository files.

## Protocol Verification

- Chat Completions: supported.
- Responses API: supported.
- Ticket Copilot runtime adapter: Chat Completions.
- Total protocol compatibility requests: 2.
- Synthetic prompt only.
- No project data sent.

## End-to-End Smoke Result

- Date: 2026-07-30.
- Runtime profile: smoke.
- Database: isolated H2 in-memory.
- Server port: 18080.
- Remote provider used: yes.
- Local fallback used: no.
- Provider business calls: 1.
- Generation record persisted: yes.
- Output nonempty: yes.
- Latency recorded: yes.
- Synthetic data only: yes.

## Retrieval Evidence

- Synthetic knowledge ID: KB-SYN-REDIS-CACHE-001.
- Synthetic knowledge title: Synthetic Redis 缓存读取超时排查手册.
- Synthetic ticket ID: TCK-260730130531-931.
- Retrieval candidate count: 1.
- Retrieved article count: 1.
- Article entered Top-K: yes.
- Category match: yes.
- Matched keyword count: 4.
- Used in draft: yes.

The earlier synthetic end-to-end smoke verified Provider invocation and generation persistence, but did not provide complete RAG evidence because the isolated knowledge data did not produce a matching reference. The guaranteed-match smoke above is the RAG evidence source.

## Persisted Evidence

The following fields are persisted by the backend flow:

- ticket
- analysis
- generation record
- status history

## Derived Evidence

The following fields are currently derived at query time:

- runId
- traceId
- total latency
- current step
- relevance
- matched keyword
- used-in-draft
- human review aggregation

## Security Verification

- API Key in logs: no.
- Authorization in logs: no.
- Base URL committed: no.
- .env created: no.
- real enterprise data sent: no.
- real personal data sent: no.
- sensitive temporary file remaining: no.

## Known Limitations

- Provider citation validation is not implemented.
- Retrieval references do not prove that every generated claim is supported.
- Trace evidence is not yet an immutable run snapshot.
- Frontend showcase pages are not yet connected to this real backend flow.
- This is a controlled synthetic smoke test, not a production benchmark.
