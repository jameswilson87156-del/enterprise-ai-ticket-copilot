# Enterprise Ticket RAG Copilot Frontend Moodboard

## 1. Moodboard Purpose

This document filters the existing references down to the visual and structural directions that best fit Enterprise Ticket RAG Copilot. It is not an implementation spec yet. It is the bridge between raw reference collection and page-level showcase design.

Boundary:

- Third-party screenshots are for local visual research only.
- Third-party screenshots must not be committed to GitHub.
- Third-party screenshots must not be copied into `docs/images/`.
- Third-party screenshots must not appear in README.
- Final README screenshots must come from this project running locally.
- Mock / demo / local-rule data must remain labeled as such.

## 2. Chosen Overall Direction

### Primary Direction

`Enterprise audit and governance platform` mixed with `dense operations cockpit`.

Why this direction fits:

- The product is about ticket triage, evidence, traceability, and human confirmation.
- The strongest unique story is not “chat with AI”; it is “AI-assisted enterprise workflow with visible evidence and human gates”.
- The UI should feel more like a serious operations tool than a marketing-heavy AI playground.

What makes it different from generic AI dashboards:

- Evidence panels matter more than charts.
- Review gates matter more than animations.
- Trace and citation are first-class UI objects.
- Metrics are framed as local evaluation evidence, not vanity analytics.

## 3. Final Recommended Reference Directions

### Direction A: Linear workbench density

- Source: `Linear`
- URL: https://linear.app/
- Best pages: `Ticket Workbench`, `Human Review`
- Why it fits:
  - quiet, premium dark surface
  - strong issue/workflow semantics
  - clear selected-object focus
- Information architecture to borrow:
  - left queue
  - central detailed context
  - right-side metadata and actions
- Visual mood to borrow:
  - dark matte panels
  - thin separators
  - restrained highlight states
- Do not copy:
  - full layout
  - feature names
  - icon system
- Phase 1 landing:
  - use it to define the workbench shell and review rhythm

### Direction B: Flowise workflow expression

- Source: `Flowise`
- URL: https://flowiseai.com/
- Best pages: `Retrieval Evidence`, `Trace Timeline`
- Why it fits:
  - visibly explains multi-step agent/RAG flow
  - helps encode fallback and review as explicit steps
- Information architecture to borrow:
  - start -> decision -> branch -> output
  - process summary on the side
- Visual mood to borrow:
  - connected nodes
  - glow only around active or selected states
- Do not copy:
  - editable graph canvas
  - node editor mechanics
- Phase 1 landing:
  - convert node language into a compact evidence timeline

### Direction C: Helicone provider observability

- Source: `Helicone`
- URL: https://www.helicone.ai/
- Best pages: `Dashboard`, `Evaluation / Metrics`
- Why it fits:
  - strongest source for request / error / latency grouping
  - aligns with provider fallback and local metrics storytelling
- Information architecture to borrow:
  - metric cards
  - range filters
  - request/error summaries
- Visual mood to borrow:
  - clean metric-first dashboard
  - low-noise panel composition
- Do not copy:
  - model logos
  - request volumes
  - cost claims
- Phase 1 landing:
  - adapt to `Top-K Hit Rate`, `Citation Precision`, `Fallback Rate`, `Human Review Count`

### Direction D: PostHog metrics center

- Source: `PostHog`
- URL: https://posthog.com/product-analytics-explorer
- Best pages: `Dashboard`, `Evaluation / Metrics`
- Why it fits:
  - turns metrics into a navigable product surface
  - useful for failed-case and eval drilldown planning
- Information architecture to borrow:
  - left support menu
  - center insight hub
  - multi-entry metrics layout
- Visual mood to borrow:
  - information-packed center area
  - clear route into related capability sections
- Do not copy:
  - retro shell
  - illustrations
  - branded whimsy
- Phase 1 landing:
  - use for a local eval snapshot page, not for core ticket workflow

### Direction E: OpenTelemetry evidence seriousness

- Source: `OpenTelemetry Demo`
- URL: https://opentelemetry.io/docs/demo/screenshots/
- Best pages: `Trace Timeline`, `Retrieval Evidence`
- Why it fits:
  - makes evidence inspectable and engineering-grade
  - helps avoid fake “AI trace” aesthetics
- Information architecture to borrow:
  - topology / graph mental model
  - layered trace + metrics + logs relationship
- Visual mood to borrow:
  - evidence-first presentation
  - low-decoration detail panels
- Do not copy:
  - real system screenshots
  - tracing tool semantics not implemented here
- Phase 1 landing:
  - support trace metadata, step logs, JSON summaries, status history

### Direction F: Langfuse trace/eval system framing

- Source: `Langfuse`
- URL: https://langfuse.com/
- Best pages: `Dashboard`, `Trace Timeline`, `Evaluation / Metrics`
- Why it fits:
  - ties traces and evaluations into one product story
  - adds “maintained engineering tool” trust cues
- Information architecture to borrow:
  - trace/eval/dataset/metrics grouping
  - changelog and status support rails
- Visual mood to borrow:
  - calm engineering-tool tone
  - documentary trust-building layout
- Do not copy:
  - customer logos
  - OSS community counts
  - platform module language
- Phase 1 landing:
  - use to define trace/eval relationships and dashboard support panels

### Direction G: Langfuse README portfolio structure

- Source: `Langfuse GitHub README`
- URL: https://github.com/langfuse/langfuse
- Best pages: `README / Portfolio`, `Evaluation / Metrics`
- Why it fits:
  - translates technical capability into scan-friendly sections
  - best source for a capability matrix without hype
- Information architecture to borrow:
  - feature matrix
  - section-by-section explanation
  - docs-linked proof rhythm
- Visual mood to borrow:
  - straightforward evidence-first documentation layout
- Do not copy:
  - screenshots
  - wording
  - feature scope
- Phase 1 landing:
  - use as a model for final README restructuring after local screenshots are refreshed

## 4. Page-Level Binding

### Dashboard

- Reference sources:
  - `Langfuse`
  - `Helicone`
  - `PostHog`
  - `Supabase`
- Borrow:
  - metric hierarchy
  - provider and fallback state
  - support-rail style capability grouping
  - recent activity and evaluation snapshot
- Project landing:
  - top area should show operational summary, not a big hero
  - one section should explicitly separate `current demo capability` from `next-stage capability`
  - one section should surface the latest local evaluation snapshot
- Screenshot focus:
  - one glance must show product name, provider state, metrics, recent ticket runs, and evaluation presence

### Ticket Workbench

- Reference sources:
  - `Linear`
  - `GitHub Copilot`
  - `Open WebUI`
- Borrow:
  - queue density
  - selected ticket focus
  - right-side assistant and review framing
- Project landing:
  - keep left queue / center context / right copilot-evidence panel
  - show priority, requester, category, SLA, citation preview, risk notes, and review action without tabs hiding the core story
- Screenshot focus:
  - first viewport must reveal “queue + ticket detail + assistant + evidence”

### Knowledge Base

- Reference sources:
  - `Dify Docs`
  - `LlamaCloud`
  - `AnythingLLM`
  - `Open WebUI README`
- Borrow:
  - source/chunk/citation vocabulary
  - multi-stage document lifecycle
  - workspace clarity for knowledge assets
- Project landing:
  - show document list, chunk/evidence preview, keyword-hit basis, related tickets, and status
  - explicitly label current mode as `keyword retrieval`
- Screenshot focus:
  - first screen should prove this is a knowledge workbench, not a generic admin table

### Retrieval Evidence

- Reference sources:
  - `Flowise`
  - `OpenTelemetry Demo`
  - `LangSmith`
- Borrow:
  - retrieval as a visible workflow step
  - hit/miss reasoning
  - engineering workflow credibility
- Project landing:
  - include `Query`, `Top-K`, `matched chunks`, `keyword score`, `citation used`, `hit/miss`, `fallback`
  - leave `rerank` visible as `next-stage` rather than pretending it exists
- Screenshot focus:
  - first viewport should show query, retrieved sources, and why the final answer used or skipped each source

### Trace Timeline

- Reference sources:
  - `Langfuse`
  - `OpenTelemetry Demo`
  - `Flowise`
  - `Arize Phoenix`
- Borrow:
  - step hierarchy
  - metadata panels
  - run/eval relationship
- Project landing:
  - trace should read like an audit timeline:
    input -> classification -> retrieval -> prompt build -> provider path -> citation attach -> review gate
  - JSON should be secondary and collapsible
- Screenshot focus:
  - one screen should clearly show the chain, current step, provider/fallback state, and detail panel

### Human Review

- Reference sources:
  - `Linear`
  - `GitHub Copilot`
  - `Raycast`
- Borrow:
  - decision queue structure
  - review-before-apply language
  - compact action emphasis
- Project landing:
  - order content as:
    evidence -> risk -> AI draft -> reviewer note -> approve/request changes/reject
  - make review state and rationale more prominent than decorative AI labels
- Screenshot focus:
  - first screen should show pending review item, evidence, draft, and visible final actions

### Evaluation / Metrics

- Reference sources:
  - `Helicone`
  - `PostHog`
  - `Langfuse`
  - `Langfuse README`
- Borrow:
  - metrics center composition
  - dataset/eval grouping
  - failed-case entry and trend hierarchy
- Project landing:
  - create a dedicated page using only local evaluation artifacts
  - highlight `Samples`, `Top-K Hit Rate`, `Context Recall@K`, `Citation Coverage`, `Citation Precision`, `Failed Case Count`, `Human Review Required Count`, `Retrieval Latency`
  - show `Provider Fallback Rate = 100%` with a boundary note, not as a quality metric
- Screenshot focus:
  - first viewport should make the project feel measured and reproducible, not hand-wavy

### README / Portfolio

- Reference sources:
  - `Langfuse README`
  - `Open WebUI README`
  - `Vercel AI Templates`
  - `LangSmith`
- Borrow:
  - screenshot-first storytelling
  - capability matrix
  - concise product positioning
- Project landing:
  - keep only local screenshots
  - pair each screenshot with one honest capability statement
  - keep boundary notes near metrics and provider claims
- Screenshot focus:
  - the screenshot set should tell a full workflow story in 4 to 6 images

## 5. Visual Mood Rules

- Chinese-first enterprise copy with retained English technical terms where useful.
- Dark enterprise console, but avoid generic “purple neon AI dashboard”.
- Fine borders, low-gloss panels, restrained depth.
- Accent colors should encode meaning:
  - cyan / blue for trace and provider path
  - green for pass / ready / healthy
  - amber for warning / pending
  - red for risk / reject / failure
  - violet only for citation/knowledge context, and in limited use
- Motion should be supportive:
  - reveal, focus, hover, timeline emphasis
  - no decorative background animation

## 6. Phase-One Landing Strategy

1. Use `Linear` as the dominant workbench reference.
2. Use `Flowise` and `OpenTelemetry` only to shape retrieval/trace evidence semantics.
3. Use `Helicone` and `PostHog` to give `Evaluation / Metrics` a product-like structure.
4. Use `Langfuse` and `Langfuse README` to unify trace/eval/portfolio storytelling.
5. Keep `Knowledge Base` honest: `keyword retrieval now`, `vector/rerank next-stage`.

## 7. Moodboard Decision

Recommended next step after this document:

`docs/frontend_showcase_design.md`

Reason:

- The references are now sufficiently narrowed.
- The project has a stable design direction.
- The next missing artifact is an implementation-ready page spec, not more inspiration.

## 8. Boundary Notes

- Third-party screenshots are only for local visual research.
- Third-party screenshots must not be committed to GitHub.
- Third-party screenshots must not appear in README.
- Final README screenshots must come from this project running locally.
- `local-rule fallback`, demo metrics, and synthetic evaluation cases must remain labeled.
- Do not present demo screenshots or local metrics as real production proof.
