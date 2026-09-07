# Frontend Real Backend Flow and Full Redesign

## Current phase

PHASE_8_REFERENCE_LED_PREMIUM_BRAND_POLISH

## Current visual direction

- Direction: calm, light, reference-led support-operations workspace with a premium in-house brand layer; warm neutrals, restrained indigo actions, and clear queue/context hierarchy.
- Layout: branded App Shell, single Topbar, queue rail, TicketDetail context column, evidence-first Copilot rail, and below-fold result/history panels.
- Shared language: `AppSidebar`, `AppTopbar`, `PageHeader`, `PanelHeader`, `StatusBadge`, `MetricCard`, `LoadingState`, `EmptyState`, and `ErrorState`.
- Responsive behavior: the desktop three-column workbench collapses to queue → context → Copilot/evidence order at `390x844`; mobile screenshots are saved under `docs/images/mobile/`.
- Boundary: no new backend endpoint, database table, Provider credential, external image, remote font, or frontend Provider call was introduced.

## Phase 8 reference-led premium polish

- A second case review focused on the details that make mature products feel intentional: a consistent icon stroke, an identifiable mark, a local-first display font stack, warm neutral surfaces, a restrained accent color, and product language that stays Chinese-first.
- The project now uses the self-drawn `NavIcon.vue` line icon set and a ticket / evidence-node `BrandMark.vue`. The visual language borrows principles from public product design systems—tokens, typography, spacing, iconography, elevation, borders and radii—without importing third-party code or brand assets.
- The palette moved from cool blue-gray to warm neutral surfaces (`#f7f7f4` canvas and `#f0f1ee` sidebar), with indigo actions and a small terracotta brand detail. This is a visual refinement only; no workflow or data meaning changed.
- Demo screenshots were regenerated after the icon, typography and palette update. The Demo browser smoke passed again; the Real API smoke was not rerun in this visual-only phase and remains historical evidence in `docs/TEST_REPORT.md`.

## Phase 7 visual correction (historical)

- The previous dark-tech presentation was rejected in favor of the public support-workspace patterns used by Intercom Inbox, Zendesk Agent Workspace, Jira Service Management, ServiceNow workspaces, and Linear search / issue views.
- `frontend/src/styles.css` now uses a light gray canvas, white panels, blue selected states, low-saturation status colors, compact borders, and system UI typography. Gradients, glows, neon accents, and decorative technology backgrounds are not part of the current direction.
- The redesign keeps the same API state, DTO fields, routes, `data-e2e` selectors, Demo / Real / Fallback semantics, and backend workflow. It changes the visual hierarchy and visible labels only: queue first, ticket context second, actionable suggestion / evidence / review third.
- Browser captures were regenerated at 1440×960, 1920×1200, and 390×844. The Demo smoke and backend regression were rerun; the Real API smoke was not rerun in this style-only correction and remains historical evidence in `docs/TEST_REPORT.md`.

## Phase 5 visual direction (historical)

- Direction: calm Chinese enterprise SaaS operations console.
- Reason: the product is an internal ticket, RAG evidence, Trace, and Human Review workflow, so the UI keeps dense cards, queues, status badges, and audit panels instead of a decorative AI dashboard.
- Boundary: this phase preserved the existing sidebar, brand mark, navigation, typography, compact enterprise density, and card style. It did not introduce a new UI framework.

## Phase 6 full redesign delivery (historical implementation record)

- Rebuilt the shared shell and tokenized theme in `frontend/src/App.vue`, `frontend/src/styles.css`, `frontend/src/components/layout/`, and `frontend/src/components/ui/`; the dark token direction recorded below is historical and was corrected by Phase 7.
- Reworked Dashboard, Ticket Workbench, Knowledge Base, Retrieval Evidence, Trace Timeline, and Human Review into a consistent page hierarchy. Evaluation / Metrics remains the existing fact-bound local evaluation page inside the new shell.
- Added explicit queue search/status/priority filters, stable Demo review fixture, evidence-layer separation, collapsed audit metadata, semantic loading/empty/error states, visible disabled/loading actions, keyboard skip/search interactions, focus-visible styles, reduced-motion handling, and mobile layout rules.
- Preserved existing `data-e2e` selectors and all existing API paths. Legacy components in `frontend/src/components/` were not deleted because they are user workspace assets; pages now use the new shared primitives and they remain legacy candidates for a separately scoped cleanup task.

## Current frontend audit summary

| Area | Finding |
| --- | --- |
| Vue / TypeScript | Vue 3.5.x, TypeScript 5.7.x, Vite 6.x. |
| Router | No Vue Router. `App.vue` uses hash routes mapped to showcase views. |
| API client | Existing `frontend/src/api/tickets.ts` used `fetch`; previous real mode lacked backend auth and demo mode used static data. |
| API base | Vite proxy maps relative `/api` to local backend. Optional non-secret `VITE_TICKET_API_BASE_URL` is supported by the client. |
| Dashboard | Previously mostly showcase/demo metrics. Now minimally uses backend ticket list and metrics API. |
| Ticket Workbench | Previously static/demo queue and analysis. Now uses backend ticket list/detail/create/run/review/trace APIs. |
| Trace pages | Previously static showcase content. Now reads `/api/tickets/{id}/trace-evidence`. |
| Human Review | Previously static review presentation. Now calls backend review decision endpoints and rereads review history. |
| Knowledge Base | No independent knowledge list API exists. This page is read-only and shows analysis knowledge hits plus trace retrieval snapshots. |
| Tests | No full test framework is configured; existing build and screenshot script remain the primary frontend verification tools. |
| Screenshot script | Existing `frontend/scripts/capture-screenshots.mjs` supports `SCREENSHOT_URL` for real local backend/frontend capture. |

## Backend API mapping

| Frontend action | Method | Backend path | Request DTO | Response DTO | Display location |
| --- | --- | --- | --- | --- | --- |
| Backend health | GET | `/api/health` | none | health response | connection state chips |
| Login for frontend session | POST | `/api/auth/login` | `LoginRequest` | `AuthResponse` | real mode keeps the token in memory only; Demo mode skips login |
| Ticket List | GET | `/api/tickets` | none | `TicketSummary[]` | dashboard, queue, recent tickets |
| Ticket Detail | GET | `/api/tickets/{id}` | none | `TicketDetail` | detail panel |
| Create Ticket | POST | `/api/tickets` | `CreateTicketRequest` | `TicketDetail` | synthetic ticket form |
| Metrics | GET | `/api/tickets/metrics` | none | `WorkbenchMetrics` | dashboard cards |
| Structured Result | GET | `/api/tickets/{id}/ai-analysis` | none | `AiAnalysis` | structured result, risk, abstention, troubleshooting |
| Run Copilot | POST | `/api/tickets/{id}/run-copilot` | none | `TicketDetail` | run button; detail refresh |
| Trace Evidence | GET | `/api/tickets/{id}/trace-evidence` | none | `TraceEvidence` | Trace page, evidence layers, review history |
| Approve Review | POST | `/api/tickets/{id}/review/approve` | `ReviewDecisionRequest` | `TicketDetail` | review action |
| Request Changes | POST | `/api/tickets/{id}/review/request-changes` | `ReviewDecisionRequest` | `TicketDetail` | review action |
| Reject Review | POST | `/api/tickets/{id}/review/reject` | `ReviewDecisionRequest` | `TicketDetail` | review action |

## Implementation scope

Changed frontend scope:

- Central TypeScript DTOs in `frontend/src/types/ticket.ts` now match current backend response shapes, including structured output, validated citations, Trace evidence, and review records.
- Unified API client in `frontend/src/api/tickets.ts` now uses relative backend API paths, in-memory demo session login, sanitized errors, and no silent mock fallback in real mode.
- Shared state in `frontend/src/composables/useTicketRealFlow.ts` centralizes loading, backend unavailable, selected ticket, analysis, trace, run, review history, and action states.
- Views connected to backend APIs:
  - Dashboard
  - Ticket Workbench / Ticket Detail
  - Knowledge/RAG evidence
  - Retrieval evidence
  - Trace timeline
  - Human Review
- Shared shell and page styles are centralized in `frontend/src/styles.css`; new layout rules replace the previous showcase structure while preserving the existing API-facing page contracts.

Explicit non-scope:

- Backend Java business logic was not changed.
- Database migrations and schema were not changed.
- RAG algorithm/data, Provider adapter, Citation validator, and abstention policy were not changed.
- No `.env` file was created.
- No Provider secret, real Base URL, actual model value, request credential, prompt, or Provider raw response is committed or displayed.
- The frontend never calls Provider endpoints directly; it only calls the ticket backend.

## Page state design

- Backend Connected / Backend Unavailable is shown without exposing secret configuration.
- Real mode does not silently replace failed network calls with mock data.
- Demo mode remains isolated to Vite `MODE=demo` and provides a complete local showcase fallback, including trace/run/review actions, without pretending to be backend persistence.
- Run Copilot disables the button while the selected ticket is running to prevent repeated clicks.
- Review actions are disabled unless the selected backend status is reviewable.
- Review submit waits for backend confirmation and then rereads ticket, list, metrics, and Trace.
- Trace page states the replay boundary: viewing Trace does not call Provider, rerun retrieval, or rewrite Citation results.
- Citation UI separates Retrieval Reference from Validated Model Citation and states the ID-membership validation boundary.

## E2E path

Synthetic local E2E is designed to run with:

1. Isolated Spring Boot backend.
2. H2 in-memory database.
3. Synthetic data only.
4. local-rule Provider mode.
5. Vite frontend with backend proxy.
6. Browser creates a synthetic ticket through the UI.
7. Browser clicks Run Copilot through the UI.
8. Browser verifies structured result, retrieval evidence, validated citation when seeded knowledge exists, model/final review separation, Trace evidence, and one manual review decision.
9. Browser refreshes a Trace route and verifies persisted evidence remains available.
10. Remote Provider request count remains 0 because Provider mode is local-rule.

## Known API boundaries

- `TicketDetail` does not expose a full `createdAt` timestamp; the UI labels this gap and uses the earliest timeline marker only when available.
- There is no standalone knowledge article list/search API in the current backend. The Knowledge page is read-only and API-bound to `AiAnalysis.knowledgeHits` and `TraceEvidence.ragReferences`.
- Trace `generationRecords` may contain prompt or response summaries in DTOs, but the frontend deliberately does not render those fields.
- Trace DTO may contain model-related field names for compatibility, but the UI does not display actual model values.

## Demo fixture behavior

`npm run dev:demo` intentionally keeps the existing showcase usable without a backend. Its in-memory adapter now covers the same user-visible workflow surface as the real client: ticket list/detail, metrics, Trace evidence, local-rule Copilot run, and Approve / Request changes / Reject review actions. Demo evidence is labeled `DEMO_LOCAL`, the Trace mode is `demo-local-derived` before a local run and `demo-local-run` after a local run, and generated run IDs use the `DEMO-*` prefix. These values are presentation fixtures, not persisted `IMMUTABLE_RUN` records.

Real mode has no equivalent fixture fallback. It authenticates against `/api/auth/login`, calls only the ticket backend, and preserves backend-unavailable, empty, loading, and action-error states. A real backend run with no retrieval evidence is expected to return a safe abstention (`NO_RETRIEVAL_EVIDENCE`) and require Human Review; the frontend does not invent a citation to make that case look successful.

## 2026-09-03 validation record

- `frontend/ npm run build` passed: `vue-tsc -b --pretty false` passed and Vite 6.4.3 produced a production bundle with 60 transformed modules.
- `frontend/ npm run screenshots` passed against the Demo Vite server on port 5182: 8 Showcase routes produced standard (`1440x960`), large (`1920x1200`), and mobile (`390x844`) PNGs; the script also passed `1366x900` and `390x844` horizontal-overflow checks. Mobile output is under `docs/images/mobile/`.
- Demo browser smoke passed: local Copilot run → `DEMO_LOCAL` evidence → Human Review Approve → `已解决` and local `APPROVED_RESOLUTION` history. The same smoke checked queue filtering, command search navigation, confirm dialog handling, and collected no console/page errors.
- Real API browser smoke passed against an isolated Spring Boot `test`-profile H2 backend on port 28081 and a Vite proxy on port 5184: `/api/health` returned 200; the UI created a synthetic ticket; `/run-copilot` returned 200 with one persisted `KB-OPS-003` retrieval snapshot, `IMMUTABLE_RUN`, valid structured output and valid Citation membership; Approve returned 200 and the ticket became `RESOLVED` with append-only review history.
- The real smoke process inherited the machine's existing optional Provider environment, so the backend attempted the configured OpenAI-compatible path, received HTTP 403, and safely recorded `PROVIDER_ERROR` / `PERMISSION_DENIED` before falling back to `local-rule`. This is a verified fallback path, not a successful real-model integration; no credential was added to the repository or frontend.
- `backend/ mvn test` passed: `Tests run: 84, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`.
- No frontend test runner is configured; the frontend acceptance therefore uses the production build, real browser smoke, screenshot capture, console-error collection, and overflow checks. No public deployment was performed.

## 2026-09-02 validation record

- `frontend/ npm run build` passed: `vue-tsc -b --pretty false` and Vite production build completed.
- `frontend/ npm run screenshots` passed against a separately started Demo Vite server on port 5180: all Showcase targets were captured at standard and `1920x1200` sizes, and the script's 1366 desktop / 390 mobile horizontal-overflow checks completed.
- Demo browser smoke passed: `DEMO-0005` → local Demo Copilot → `DEMO_LOCAL` evidence → Approve → detail status `已解决` and `APPROVED_RESOLUTION` review history.
- Real browser smoke passed against an isolated Spring Boot test-profile H2 backend on port 28080 and a Vite proxy on port 5181: synthetic ticket creation → backend Copilot run → `NO_RETRIEVAL_EVIDENCE` abstention → Approve → detail status `已解决` and review history.
- The isolated H2 smoke database was schema-only, so it intentionally did not exercise a retrieval-hit/citation-positive case. Existing backend JUnit/integration tests cover the persisted retrieval, structured output, citation validation, and Trace paths.
- No frontend test runner is configured; browser smoke and screenshot checks are the repeatable manual frontend acceptance for this phase.
