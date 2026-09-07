# DeepSeek two-project synthetic smoke — 2026-09-05

## Scope and boundary

This is a bounded, local verification of the enterprise ticket project's actual
DeepSeek OpenAI-compatible adapter. The request used only synthetic ticket
content and a test-profile H2 database. The API key was injected into the
temporary backend process only; it is not recorded here, in the frontend, in
the repository, or in the command output.

The reusable runner is [verify-deepseek-synthetic.ps1](../../scripts/local/verify-deepseek-synthetic.ps1).

## Result

- Provider: `deepseek`
- Model: `deepseek-chat`
- Adapter protocol: Chat Completions
- Local fallback: disabled
- Backend path: health → Demo login → synthetic ticket → `run-copilot` → Trace → reviewer approval
- Provider run: `SUCCESS`
- Actual provider: `deepseek`
- Fallback used: `false`
- Error category: `NONE`
- Retrieval snapshots: at least one
- Validated citations: `1`
- Structured output: `VALID`
- Citation membership: `VALID`
- Human review: completed
- Final ticket state: `RESOLVED`

## Repeatability note

- A later repeat reached the real provider and completed the workflow, but the
  first model response of that repeat did not satisfy the local structured-output
  or citation gate. The run failed closed; it did not fall back to the local
  provider and it was not recorded as a pass.
- The immediate retry passed with `output=VALID`, `citations=VALID`, one
  validated citation, and final state `RESOLVED`.
- The reusable smoke script now prints only sanitized validation status fields
  when this gate fails; it never prints the provider key or the raw model answer.

## Safety checks

- No provider key was found in the temporary smoke logs.
- Temporary backend port was released after the run.
- No MySQL, cloud host, DNS record, public deployment, or real customer data was changed.

## What this proves

The configured DeepSeek endpoint, server-side key injection, request shape,
structured-output parsing, retrieval/citation guard, immutable Trace path, and
human-review gate work together for one synthetic request.

## What this does not prove

This does not prove model quality on real support data, production quota or
cost, public availability, OIDC acceptance, production MySQL readiness,
backups, monitoring, DNS/TLS, or Alibaba Cloud deployment.
