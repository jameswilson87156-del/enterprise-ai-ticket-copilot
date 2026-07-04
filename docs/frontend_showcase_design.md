# Enterprise Ticket RAG Copilot Frontend Showcase Design Plan

## 1. Visual Positioning

Enterprise Ticket RAG Copilot should be designed as:

- enterprise-grade AI RAG Copilot
- developer tool
- agent workflow console
- RAG / Trace / Evaluation workbench
- audit and review-oriented operations surface

It should not look like:

- a generic backend admin
- a CRUD management system
- a course assignment dashboard
- a chat-only AI landing page
- a fake production AI platform with overstated capabilities

Chosen visual direction:

`Enterprise audit and governance platform` + `dense operations cockpit`

Why it fits this project:

- The strongest story here is explainability, review, and operational trust.
- The product already has multiple showcase pages and local evaluation evidence.
- A calm, high-density enterprise console makes the portfolio feel more serious and distinctive than a trendy AI landing page.

How it differs from generic AI portfolios:

- It emphasizes trace, citation, and review gates over big hero sections.
- It makes evaluation visible as product evidence.
- It keeps the product grounded in enterprise support operations rather than in speculative agent autonomy.

## 2. Design Keywords

- dark AI SaaS console
- deep navy-black background
- cyan / blue / amber restrained highlights
- glass-light panels, not glossy gimmicks
- fine borders
- weak shadows
- high density without crowding
- trace / timeline / evidence semantics
- metric cards with audit meaning
- left navigation + single top status bar
- center work area + right detail panel
- enterprise calm over startup spectacle

## 3. Color System

### Core palette

| Role | Token suggestion | Value | Usage |
| --- | --- | --- | --- |
| App canvas | `--bg-canvas` | `#07111c` | page background |
| Deep canvas | `--bg-canvas-deep` | `#040914` | outer shell, topbar shadow zones |
| Sidebar | `--bg-sidebar` | `#081220` | app shell nav |
| Panel | `--bg-panel` | `#0d1726` | base cards and work areas |
| Panel strong | `--bg-panel-strong` | `#112033` | selected or priority panels |
| Panel subtle | `--bg-panel-soft` | `rgba(13, 23, 38, 0.76)` | translucent overlay cards |
| Border | `--border-soft` | `rgba(155, 180, 214, 0.14)` | normal separators |
| Border strong | `--border-strong` | `rgba(97, 149, 242, 0.24)` | selected/highlight panels |

### Text

| Role | Token suggestion | Value | Usage |
| --- | --- | --- | --- |
| Primary text | `--text-primary` | `#eef4ff` | headings, key labels |
| Secondary text | `--text-secondary` | `#c7d4e8` | normal body |
| Muted text | `--text-muted` | `#8090a8` | helper text, notes |
| Inverse text | `--text-inverse` | `#06101b` | bright chips / button text |

### Semantic states

| Role | Token suggestion | Value | Usage |
| --- | --- | --- | --- |
| Primary highlight | `--accent-blue` | `#3f7cff` | active nav, primary actions |
| Trace / provider | `--accent-cyan` | `#22c6da` | trace path, provider state, latency |
| Success | `--accent-green` | `#2dd88f` | pass, connected, approved |
| Warning | `--accent-amber` | `#ffb55c` | pending, fallback, review queue |
| Risk | `--accent-red` | `#ff647f` | reject, failure, high risk |
| Knowledge / citation | `--accent-violet` | `#8b7cf6` | citation, chunk, knowledge association |
| Metrics neutral | `--accent-slate` | `#5b6f8d` | secondary trends and passive chips |

### Usage rules

- Cyan and blue own `Trace`, `Provider`, `Latency`, `Evidence`.
- Violet owns `Knowledge`, `Citation`, `Chunk`, `RAG Reference`.
- Amber owns `Fallback`, `Pending`, `Requires Review`.
- Red owns `Risk`, `Rejected`, `Miss`, `Blocked`.
- Green owns `Approved`, `Healthy`, `Hit`, `Connected`.
- Do not use more than 2 accent colors in the same local cluster unless it is a status summary block.
- Avoid color noise and large gradient washes.

## 4. Typography Hierarchy

### Font families

- Body UI: `Aptos, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif`
- Mono / IDs / JSON / metrics microdata: `Cascadia Code, Consolas, SFMono-Regular, monospace`

### Scale

| Role | Size / weight guidance | Usage |
| --- | --- | --- |
| Page title | `34px` to `40px`, `700` | major page headline |
| Page subtitle | `14px` to `16px`, `400` | explanatory strapline |
| Section title | `18px` to `22px`, `600` | module heading |
| KPI number | `26px` to `32px`, `700` | metric cards |
| Card title | `15px` to `17px`, `600` | panel and card headings |
| Body copy | `13px` to `14px`, `400` | normal UI copy |
| Status chip | `11px` to `12px`, `600` | chips and tags |
| Citation text | `12px` to `13px`, `500` | chunk snippet and source path |
| Trace / code text | `12px` to `13px`, `500` mono | IDs, JSON, logs, latency |

### Typography rules

- Keep letter spacing at `0`.
- Use short section titles.
- Do not use hero-scale type inside compact work panels.
- Use mono only for IDs, trace fields, JSON, timestamps, numeric precision, and diagnostic metadata.

## 5. Card And Component Language

### KPI cards

- Small, dense, single-purpose.
- Structure:
  - label
  - value
  - short note or delta
  - optional status chip
- Avoid chart-heavy cards in the first row.

### Ticket cards

- Queue row or compact card style.
- Must show:
  - title
  - priority
  - status
  - requester/source
  - updated time
  - confidence or review badge if relevant
- Selected state should be obvious without glow excess.

### Evidence cards

- Used for citation, retrieval hit, chunk preview, expected hit/miss notes.
- Must show:
  - source id
  - title or path
  - reason or score
  - whether final answer cited it

### Trace nodes

- Compact step cards in a chain or timeline.
- Each node should expose:
  - step name
  - state
  - latency
  - linked detail
- JSON remains secondary and collapsible.

### Review panel

- Evidence-first.
- Keep final actions permanently visible:
  - `Approve`
  - `Request Changes`
  - `Reject`
- Risk level and review reason sit above action buttons.

### Knowledge source cards

- Show:
  - source type
  - title
  - tags
  - current retrieval mode
  - update state
  - linked ticket count

### Metrics snapshot cards

- Show:
  - metric name
  - value
  - scope note
  - boundary tooltip or footnote
- Separate product-style metrics from boundary disclosures visually.

### Provider status cards

- Show:
  - provider path
  - current mode
  - fallback state
  - last run note
- Must not expose API keys or imply verified production availability.

### Right detail panel

- Sticky or fixed-width side panel.
- Best for:
  - AI draft
  - evidence summary
  - provider metadata
  - reviewer action
- Width target: `320px` to `380px`

## 6. Layout System

### App shell

- `Sidebar` + `Topbar` + `Content shell`
- One topbar only
- No stacked navigation bars

### Sidebar

- Width target: `220px` to `236px`
- Order:
  - brand
  - primary nav
  - boundary state / provider status / build badge area
- Primary nav must include the existing showcase surfaces plus the new `Evaluation / Metrics`.

### Topbar

- Height target: `58px` to `64px`
- Content:
  - search or quick jump
  - current provider mode
  - runtime boundary
  - role / reviewer identity
  - minor action icons only
- No duplicate KPI strip in topbar.

### Main content zone

- Wide but constrained.
- Recommended content max width:
  - desktop content shell: `1440px` to `1560px`
  - local panel max width per module to preserve screenshot readability

### Right panel

- Use when the page needs persistent draft/evidence/review context.
- Prefer a real panel over modal or hidden tabs.

### Screenshot-safe area

- The first viewport must always contain the page's main argument.
- README-safe primary capture target:
  - page title
  - one key data cluster
  - one main workflow surface
  - at least one evidence/review cue

### README-friendly layout rule

- Every page must have a screenshot-safe first viewport that tells a complete story without scrolling.
- Avoid placing the critical detail below the fold.

## 7. Core Page Specs

### Dashboard

- Page goal:
  - summarize system activity, provider path, ticket load, evaluation snapshot, and recent runs
- Information modules:
  - KPI row
  - provider / fallback status
  - evaluation snapshot
  - recent ticket runs
  - top knowledge sources
  - recent human review queue
  - latest trace or audit activity
- Reference sources:
  - `Langfuse`, `Helicone`, `PostHog`, `Supabase`
- Visual focus:
  - compact operational summary with one clear center of gravity
- Data boundary:
  - all numbers are local demo or local evaluation values
  - `Resolved by AI` must be labeled as assisted/demo resolution, not autonomous production resolution
- README screenshot focus:
  - show product identity, KPI row, provider mode, evaluation snapshot, recent runs

### Ticket Workbench

- Page goal:
  - handle a single ticket with full context, AI-assisted analysis, citations, and review actions
- Information modules:
  - ticket queue
  - current ticket context
  - timeline/activity
  - AI analysis result
  - suggested handling plan
  - knowledge references
  - draft reply
  - risk notes
  - human confirmation panel
- Reference sources:
  - `Linear`, `GitHub Copilot`, `Open WebUI`
- Visual focus:
  - queue + detail + copilot/evidence should coexist in one viewport
- Data boundary:
  - analysis remains local-rule/demo unless real provider evidence exists
- README screenshot focus:
  - selected ticket, evidence block, AI draft, visible review action

### Knowledge Base

- Page goal:
  - present knowledge sources, keyword retrieval context, citation preview, and related tickets
- Information modules:
  - document/source list
  - search
  - tags and status
  - chunk / snippet preview
  - related tickets
  - citation preview
  - current retrieval mode
- Reference sources:
  - `Dify Docs`, `LlamaCloud`, `AnythingLLM`, `Open WebUI README`
- Visual focus:
  - make it feel like a knowledge operations surface, not a table dump
- Data boundary:
  - current retrieval mode must state `keyword retrieval`
  - vectorization / rerank only as `next-stage`
- README screenshot focus:
  - document list + snippet/evidence panel + clear retrieval-mode label

### Retrieval Evidence

- Page goal:
  - explain why a given ticket or query retrieved certain knowledge and whether it was used
- Information modules:
  - query input
  - rewritten query if available; otherwise `next-stage`
  - Top-K results
  - chunk content
  - keyword score
  - hit reason
  - citation used flag
  - eval hit/miss state
  - fallback note
- Reference sources:
  - `Flowise`, `OpenTelemetry Demo`, `LangSmith`
- Visual focus:
  - evidence table + workflow summary, not just raw text
- Data boundary:
  - `Rerank` appears as `next-stage` unless implemented
  - score labels should say `keyword score` if not similarity score
- README screenshot focus:
  - one query, three retrieved sources, one final cited source path, one miss/warning note

### Trace Timeline

- Page goal:
  - show the end-to-end run as an auditable series of steps
- Information modules:
  - run header
  - step chain
  - step detail
  - provider metadata
  - generation record summary
  - JSON input/output summary
  - logs
  - status history
  - human review gate
- Reference sources:
  - `Langfuse`, `OpenTelemetry Demo`, `Flowise`, `Arize Phoenix`
- Visual focus:
  - the step chain is primary; JSON is secondary
- Data boundary:
  - `traceId` and `runId` are showcase identifiers, not proof of full distributed tracing runtime
- README screenshot focus:
  - step chain + provider/fallback card + detail panel visible at once

### Human Review

- Page goal:
  - make review a visible safety and quality gate, not an afterthought
- Information modules:
  - pending review queue
  - risk summary
  - AI reply draft
  - knowledge references
  - trace evidence summary
  - reviewer note editor
  - review decision state
  - action buttons
- Reference sources:
  - `Linear`, `GitHub Copilot`, `Raycast`
- Visual focus:
  - evidence before action
- Data boundary:
  - do not imply autonomous closure or auto-reply to customers
- README screenshot focus:
  - review queue + draft + evidence + explicit decision buttons

### Evaluation / Metrics

- Page goal:
  - surface the local evaluation loop as a first-class product page
- Information modules:
  - dataset summary
  - key metrics row
  - baseline comparison
  - failed cases
  - retrieval latency summary
  - human review required cases
  - boundary notes
- Reference sources:
  - `Helicone`, `PostHog`, `Langfuse`, `Langfuse README`
- Visual focus:
  - “measured and explainable” rather than “AI wow factor”
- Data boundary:
  - metrics must come from `docs/metrics/rag_metrics_snapshot.md` or `docs/metrics/rag_metrics_latest.json`
  - no invented trend lines
  - `Provider Fallback Rate = 100%` must be explained as no-key local run
- README screenshot focus:
  - metric cards + baseline table + boundary note in the same shot

## 8. Page Data Plan

### Dashboard

- `Total Tickets`
- `Resolved by AI`
  - boundary note: for this project, this should mean assisted/demo resolution path, not autonomous production AI
- `Avg Response Time`
- `Knowledge Hits`
- `Human Reviews`
- `Provider Status`
- `Recent Ticket Runs`
- `Top Knowledge Sources`
- `Evaluation Snapshot`

### Ticket Workbench

- ticket input
- user question
- priority
- category
- AI analysis result
- recommended handling plan
- cited knowledge sources
- one-click draft generation entry
- human confirmation

### Knowledge Base

- document list
- chunk count
- document status
- current `keyword retrieval` state
- recent update time
- labels / tags
- source
- search

### Retrieval Evidence

- query
- Top-K retrieval results
- chunk content
- `keyword score`
- cited source
- hit reason
- whether final answer used it
- eval hit/miss label

### Trace Timeline

- `Ticket Input`
- `Query Rewrite`
  - if not implemented, show `next-stage`
- `Retrieval`
- `Rerank`
  - if not implemented, show `next-stage`
- `Prompt Build`
- `Provider Call`
- `Answer Generate`
- `Citation Attach`
- `Human Review`

### Human Review

- AI reply
- risk note
- citation evidence
- reviewer edit
- review status
- approve / reject / regenerate
- review reason

### Evaluation / Metrics

- `Samples`
- `Top-K Hit Rate`
- `Context Recall@K`
- `Citation Coverage`
- `Citation Precision`
- `Failed Case Count`
- `Human Review Required Count`
- `Retrieval Latency`
- demo dataset boundary note

Current local snapshot that the page may safely use:

- `Samples = 16`
- `Top-K = 3`
- `Top-K Hit Rate = 100.00%`
- `Context Recall@K = 90.00%`
- `Citation Coverage = 100.00%`
- `Citation Precision = 81.11%`
- `Failed Case Count = 6`
- `Human Review Required Count = 15`
- `Avg Retrieval Latency = 0.0994 ms`
- `Provider Fallback Rate = 100.00%` with explicit no-key explanation

## 9. README Screenshot Order

Recommended final local screenshot order:

1. `Dashboard`
2. `Ticket Workbench`
3. `Retrieval Evidence` or `Trace Timeline`
4. `Human Review`
5. `Knowledge Base`
6. `Evaluation / Metrics`

Notes:

- Only local project screenshots may appear in README.
- Third-party reference screenshots may never appear in README.
- `Evaluation / Metrics` should become the sixth image only after the page exists and is screenshot-safe.

## 10. Borrowing And Originality Boundary

### Borrow from Linear

- workbench density
- selected-item focus
- left / center / right productivity rhythm
- calm dark enterprise tone

### Borrow from Flowise

- workflow step chain
- active step emphasis
- branch logic framing for retrieval/provider/review

### Borrow from Helicone / PostHog

- metrics grouping
- provider / fallback operational language
- evaluation snapshot organization

### Borrow from OpenTelemetry

- evidence seriousness
- trace topology thinking
- layered detail inspection

### Borrow from Langfuse

- trace/eval/metrics relationship
- engineering-tool trust cues
- portfolio-safe feature grouping

### Must remain original

- visual identity of the product
- exact card layouts
- wording and page names
- all screenshots
- all citations, trace steps, evaluation values, and boundary explanations

### Absolutely must not copy

- logos
- screenshots
- trademarked icons
- brand copy
- feature names unique to the source products
- full layouts or code

## 11. User Confirmation Checklist

Please confirm:

- whether the dark enterprise AI SaaS console direction is approved
- whether the page-level reference binding is approved
- whether adding a dedicated `Evaluation / Metrics` frontend page is approved
- whether the final README screenshot order is approved
- whether phase one remains a showcase/portfolio surface without pretending to be a real online AI platform
- whether the next step may begin frontend code implementation one page at a time

## 12. Implementation Sequence

Recommended order after approval:

1. `Dashboard`
2. `Ticket Workbench`
3. `Trace Timeline`
4. `Knowledge Base`
5. `Human Review`
6. `Evaluation / Metrics`

Reason:

- `Dashboard` sets shell, KPI rhythm, and provider boundary language.
- `Ticket Workbench` is the portfolio centerpiece.
- `Trace Timeline` and `Knowledge Base` anchor explainability.
- `Human Review` completes the safety story.
- `Evaluation / Metrics` closes the measurable portfolio loop.

## 13. Boundary Notes

- Third-party screenshots are only for local visual research.
- Third-party screenshots must not be committed to GitHub.
- Third-party screenshots must not appear in README.
- Final README screenshots must come from this project running locally.
- `local-rule fallback`, synthetic dataset cases, and local metrics must stay labeled.
- Evaluation / Metrics numbers must come from `docs/metrics/` or local scripts.
- Do not present demo metrics as real business performance.
