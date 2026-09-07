# Real authentication, provider and staging deployment

> 2026-09-05 audit correction: overall **BLOCKED**. The current independent rerun passed 289 backend tests with zero failures/errors/skips, frontend type-check/build, production npm audit, configuration guards, isolated schema-only MySQL checks and ordinary/TLS Compose static checks (**LOCAL_PASS**). Local OIDC/PKCE and DeepSeek historical evidence exists, but was not rerun for this audit. Demo SQL mounts, local MySQL wiring, missing versioned baseline, unsafe build-context inclusion and staging/production localhost fallback were remediated locally; Docker registry access, real cloud/IdP/browser/backup acceptance and public DNS/TLS remain **STAGING_PENDING** or **BLOCKED**. See [the independent staging audit](release/STAGING_AUDIT_PLAN_20260905.md) before any runbook deployment commands.

This document records the runnable boundary for the current enterprise ticket project. It is an execution guide, not evidence that a public deployment has succeeded. The explicitly linked isolated provider smoke below is evidence for one synthetic DeepSeek request only; it is not production-readiness evidence.

> Copy-ready deployment and upgrade handoff: [AI_DEPLOYMENT_AND_UPGRADE_PROMPT.md](release/AI_DEPLOYMENT_AND_UPGRADE_PROMPT.md). The handoff separates local evidence from external staging and public-deployment gates.

## What is implemented

- Backend `OIDC` mode is a stateless Spring Security resource server. The issuer's discovery document and JWKS are used to verify bearer JWTs. An optional audience validator is enabled by `TICKET_AUTH_AUDIENCE`.
- The existing Demo JWT login remains available only when `TICKET_AUTH_MODE=DEMO`. Staging and production profile files select `OIDC`; the password login endpoint is disabled in that mode.
- Roles are mapped from the configured claim plus common `groups`, Keycloak `realm_access.roles`, and `scope` shapes. The application still applies its own ticket role policy after token verification.
- The Vue client uses Authorization Code + PKCE. It never receives the AI API key. The access token is kept in page memory and a short-lived `sessionStorage` entry so a page reload can complete the same browser session; no refresh token is stored.
- `deploy/staging/` contains a MySQL-backed API, Nginx edge, and optional Caddy TLS scaffold. It is intentionally not a production secret manager or a live deployment command.
- A staging/production startup guard rejects incomplete OIDC, database or AI-provider configuration, unsupported provider names, and `TICKET_AI_FALLBACK_TO_LOCAL=true`. It prevents a public profile from silently becoming a local-rule or Demo deployment.
- SpringDoc and Swagger UI are disabled in staging/production. The expensive Copilot-run route is rate-limited per authenticated user (default: 6 requests per 60 seconds) and returns standard rate-limit headers plus `429`/`Retry-After` when exhausted.
- The edge template provides a baseline CSP, frame/object blocking, MIME sniffing protection, referrer and permissions policies. TLS mode adds HSTS. The in-process rate limiter is a single-instance safeguard; production replicas still need a shared gateway or Redis limiter.

## Provider choices

The Java adapter currently sends a bounded OpenAI-compatible Chat Completions request. The same code path can target OpenAI, DeepSeek, or a compatible gateway by changing server-side values:

| Provider | Base URL | Path | Example model |
| --- | --- | --- | --- |
| OpenAI | `https://api.openai.com/v1` | `/chat/completions` | provider account model |
| DeepSeek | `https://api.deepseek.com` | `/v1/chat/completions` | `deepseek-chat` |
| Compatible gateway | gateway base | gateway path | gateway model |

The adapter sends only ticket fields and current-run retrieval evidence. It validates the response shape, records provider outcome metadata, and keeps the human review gate. The server must set `TICKET_AI_FALLBACK_TO_LOCAL=false` if a provider failure must not become a local-rule answer.

An earlier shared compatible-endpoint check returned HTTP `403`; that historical result remains an upstream credential/permission or endpoint failure, not a success. A later isolated DeepSeek verification did succeed with a developer-supplied, untracked local runtime configuration. It used only synthetic data and confirmed the actual backend path records `requestedProvider=deepseek`, `actualProvider=deepseek`, `fallbackUsed=false`, `VALID` structured output, and `VALID` citation membership before human review. See [sanitized DeepSeek smoke evidence](evidence/deepseek-isolated-smoke-20260904.md). It does not establish public deployment readiness or real-data model quality.

## OIDC provider registration

Register one public browser client for this application:

1. Use Authorization Code + PKCE with `S256`; do not issue a client secret to the Vue application.
2. Register the exact redirect URI `https://<ticket-host>/` (and the exact local URI only for local verification, such as `http://127.0.0.1:5182/`).
3. Configure the token's `sub`, username/email, display name and role claim. Grant the role needed by the ticket policy (`ADMIN`, `AGENT`, `REVIEWER` or `VIEWER` after normalization).
4. Set `TICKET_AUTH_ISSUER_URI` to the issuer base used in the token's `iss` claim. Set `TICKET_AUTH_AUDIENCE` when the IdP issues an API audience.

## Staging runbook

From the repository root:

```powershell
Copy-Item deploy/staging/.env.example deploy/staging/.env
# Edit deploy/staging/.env; keep the file untracked.
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml config -q
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml up -d --build
docker compose --env-file deploy/staging/.env -f deploy/staging/docker-compose.yml --profile tls up -d caddy
```

The MySQL init scripts run only when the named volume is first created. Do not attach the demo data script to a production database; use a separately reviewed migration/seed process there. The API health check is `GET /api/health`, and the edge health check is `GET /health`.

Before enabling the TLS profile, verify that the host A/AAAA records point to the deployment machine, ports 80/443 are reachable, and the Caddy ACME email is real. The project does not contain SSH/cloud credentials and this workspace therefore cannot perform that external deployment step.

## Security release gate

Before an Alibaba Cloud or other public deployment, keep the following controls enabled and verify them against the actual hostnames and IdP:

1. Keep `TICKET_AUTH_MODE=OIDC`, use a real `TICKET_AUTH_ISSUER_URI`, and verify both an anonymous `401` path and a role-based `403` path with a real token.
2. Set a real MySQL account and a supported provider configuration. Keep `TICKET_AI_FALLBACK_TO_LOCAL=false`; the deployment guard deliberately stops startup if these values are missing or unsafe.
3. Keep `TICKET_AI_RATE_LIMIT_ENABLED=true`. The default local limiter is useful for a single API process; add an Alibaba Cloud gateway/WAF or a shared Redis limiter before running multiple API replicas.
4. Keep Swagger disabled in public profiles. Do not expose MySQL directly, and terminate HTTPS only after the domain, Caddy/Alibaba certificate, firewall rules and backup procedure have been reviewed.
5. Use an untracked runtime `.env` or a cloud secret manager. Never add provider keys, database passwords, OIDC client secrets or certificate private keys to the repository.

## Acceptance checks

1. Without a bearer token, `GET /api/tickets` must return 401 in OIDC mode.
2. A token with a valid issuer/signature but insufficient application role must reach the application and return 403 for a restricted action.
3. A token for a mapped user must load the ticket workbench; the Demo `admin/admin123` login must not work in staging.
4. Create a synthetic ticket, run Copilot, verify that the trace records the configured provider/model, then verify the result stays pending human review.
5. Intentionally use an invalid provider key. With fallback disabled, the trace must record provider failure and the response must not be presented as a local-rule success.
6. Run `git diff --check` and the repository test/build commands before release.

## Current verification boundary

- Enterprise backend: 289 tests passed with zero failures/errors in the current workspace, including deployment-guard, AI rate-limit, and DeepSeek-alias adapter tests.
- Enterprise frontend: type-check and production build passed; the production dependency audit reported zero vulnerabilities.
- The deployment compose file was statically validated with `docker compose ... config -q`.
- One isolated, synthetic DeepSeek provider/API/H2 smoke succeeded; its complete bounded result is recorded in the linked evidence file above. No real IdP token was available in this workspace, no cloud/SSH deployment credential was available, and no commit/push was performed.
