# DeepSeek isolated provider smoke — 2026-09-04

## Scope and data boundary

This is a one-request, local verification of the Enterprise AI Ticket Copilot's existing OpenAI-compatible provider adapter against DeepSeek. It used the developer's untracked local runtime configuration and sent **only synthetic test content**. No API key, bearer token, raw prompt, raw model response, customer data, production database data, or external deployment credential is recorded in this file.

The temporary Spring Boot process used the `test` profile, an in-memory H2 database, the test-only seeded knowledge article `KB-OPS-003`, Demo authentication, and a temporary localhost port. It was stopped after the test; no MySQL database, cloud host, DNS record, or public deployment was changed.

## Configuration shape verified

- Provider identity: `deepseek`
- Provider endpoint family: DeepSeek OpenAI-compatible Chat Completions (`/v1/chat/completions`)
- Model identity: `deepseek-chat`
- Local-rule fallback: disabled

## Results

1. A minimal synthetic adapter-shape request received HTTP `200` from DeepSeek.
2. The response contained a non-empty Chat Completions choice whose content parsed as strict JSON and supplied every field required by the project structured-output contract.
3. The actual backend API path completed successfully:
   - Demo login: `200`
   - Create synthetic ticket: `200`
   - `POST /api/tickets/{id}/run-copilot`: `200`
   - Trace retrieval: `200`
4. The immutable run recorded `requestedProvider=deepseek`, `actualProvider=deepseek`, `runStatus=SUCCESS`, `fallbackUsed=false`, `errorCategory=NONE`, and one retrieval-hit snapshot.
5. The backend accepted the result as `VALID` structured output with `VALID` citation membership and one validated citation.
6. A reviewer approval completed through the API: `200`, final ticket status `RESOLVED`, and review history contained `APPROVED_RESOLUTION`.

## What this does and does not prove

This proves the configured key, endpoint, model, adapter request shape, provider response parsing, evidence/citation guard, immutable trace recording, and human-review flow work for one synthetic, isolated request at the recorded time.

It does **not** prove public deployment readiness, model quality on real support data, throughput, quota, cost, provider uptime, OIDC integration, DNS/TLS, production MySQL migrations, backups, monitoring, multi-replica rate limiting, or Alibaba Cloud deployment. Those remain separate release checks.
