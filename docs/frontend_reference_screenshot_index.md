# Enterprise Ticket RAG Copilot Frontend Reference Screenshot Index

## Research Boundary

- Collection date: 2026-07-04
- Third-party screenshots are for local visual research only.
- Third-party screenshots are stored only in `.local/reference_screenshots/`.
- Third-party screenshots must not be committed to GitHub.
- Third-party screenshots must not be copied into `docs/images/`.
- Third-party screenshots must not be used in README.
- Final README screenshots must come from this project running locally.
- Do not copy third-party source code, logos, trademarks, copywriting, media assets, or complete page layouts.
- No login, paid page, CAPTCHA bypass, private page, or account-only dashboard was used.

## Current Project Frontend Snapshot

- Tech stack: Vue 3, TypeScript, Vite.
- Routing style: `frontend/src/App.vue` uses hash-based route mapping, not Vue Router.
- Current showcase pages: Dashboard, Ticket Workbench, Knowledge / RAG, Trace Evidence, Human Review.
- Current data source: frontend supports API calls and demo runtime fallback; Showcase views use local demo constants for portfolio screenshots.
- README current screenshots: `docs/images/dashboard.png`, `ticket-detail.png`, `knowledge-base.png`, `trace-evidence.png`, `human-review.png`.
- Existing large screenshots: `docs/images/large/` mirrors the five README showcase screenshots.
- Avoid mixing: `docs/images/ai-analysis.png` is an older/compatibility screenshot and should not be mixed with the newer five-page dark showcase set unless explicitly explained.
- Missing visual surface: Evaluation / Metrics exists in README/docs as text and metrics files, but there is no dedicated frontend Evaluation / Metrics page yet.
- Main portfolio risk: the current UI is already closer to an AI SaaS showcase than a plain CRUD admin panel, but it still reads as a static demo/showcase in places and would benefit from stronger product references for trace, evidence, eval, and review workflows.

## Project Page Mapping

| Project page | Current status | Reference goals |
| --- | --- | --- |
| Dashboard | Existing `DashboardShowcaseView.vue`; README core screenshot | Stronger AI SaaS command center, metrics hierarchy, provider/eval boundary chips |
| Ticket Workbench | Existing `TicketWorkbenchShowcaseView.vue`; strongest current README page | Real ticket triage density, left queue + center context + right Copilot evidence panel |
| Knowledge Base | Existing `KnowledgeRagShowcaseView.vue` | Knowledge source list, chunk/evidence preview, citation readiness |
| Retrieval Evidence | Folded into Knowledge / RAG and Trace Evidence | Retrieval hits, expected source ids, citation gating, fallback state |
| Trace Timeline | Existing `TraceShowcaseView.vue` covers trace evidence | Step chain, provider/tool call records, logs, JSON evidence, latency |
| Human Review | Existing `HumanReviewShowcaseView.vue` | Risk queue, evidence-before-action layout, approval/rollback boundaries |
| Evaluation / Metrics | No standalone frontend page | Demo dataset, metric cards, failed cases, baseline boundary table |
| README / Portfolio | README uses five local project screenshots | Better screenshot narrative, source-bound visual language, honest capability boundaries |

## Reference Screenshot Index

| No. | Source | URL | Access date | Result | Local screenshot file | Suitable project page | Information architecture to borrow | Visual style to borrow | Interaction idea to borrow | Do not copy | Concrete inspiration for this project | Priority | Phase 1 fit | Notes / failure reason |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| R01 | Dify official homepage | https://dify.ai/ | 2026-07-04 | Failed | N/A | Dashboard, Agent workflow | Product-level AI workflow positioning | N/A | N/A | Do not invent screenshots after failed capture | Keep as a URL-only candidate for future manual check | Low | No | Playwright capture failed because page execution context was destroyed during navigation. No bypass attempted. |
| R02 | Dify Knowledge Base Docs | https://docs.dify.ai/en/guides/knowledge-base | 2026-07-04 | Partial | `.local/reference_screenshots/001_dify_knowledge_base_docs.png` | Knowledge Base, Retrieval Evidence | Knowledge base concepts, document-source structure, retrieval terminology | Docs layout only, not a polished console | Use documentation hierarchy to clarify source/chunk/citation terms | Do not copy docs text, docs layout, or Dify brand | Helps define Knowledge Base page sections and labels honestly | Medium | Partial | Official public docs page used after homepage instability; useful for IA, less useful for visual polish. |
| R03 | Flowise Agentflow Builder | https://flowiseai.com/ | 2026-07-04 | Success | `.local/reference_screenshots/002_flowise_agentflow_builder.png` | Trace Timeline, Retrieval Evidence | Agent/workflow builder mental model | Developer-tool canvas, modular node language | Show RAG/Provider/Review as connected steps | Do not copy node UI or Flowise branding | Trace page can use clearer step chain and current-step focus | High | Yes | Public product page. |
| R04 | Open WebUI Product Site | https://openwebui.com/ | 2026-07-04 | Success | `.local/reference_screenshots/003_openwebui_product_site.png` | Knowledge Base, Ticket Workbench | Workspace and model/knowledge organization | Dark AI app surface, chat/workspace feel | Attach knowledge context to user workflow | Do not copy chat UI or product marks | Helps make Knowledge/RAG feel like a real user-facing AI workspace | Medium | Partial | Public product page. |
| R05 | AnythingLLM Product Site | https://anythingllm.com/ | 2026-07-04 | Success | `.local/reference_screenshots/004_anythingllm_product_site.png` | Knowledge Base, README / Portfolio | Workspace + documents + agents positioning | Simple AI app product framing | Make document/RAG setup understandable to non-experts | Do not copy marketing copy or mascot/brand assets | Useful for explaining local demo knowledge workflow in README narrative | Medium | Partial | Public product page. |
| R06 | LlamaCloud Document Platform | https://www.llamaindex.ai/llamacloud | 2026-07-04 | Success | `.local/reference_screenshots/005_llamacloud_document_platform.png` | Knowledge Base, Evaluation / Metrics | Document parsing/indexing pipeline | Clean technical product layout | Show data preparation before retrieval | Do not claim this project has LlamaCloud or production parsing | Inspires a future "source ingestion -> retrieval -> citation" visual path | Medium | Partial | Public product page. |
| R07 | LangSmith Platform | https://www.langchain.com/langsmith-platform | 2026-07-04 | Success | `.local/reference_screenshots/006_langsmith_platform.png` | Trace Timeline, Evaluation / Metrics | Observe/debug/evaluate lifecycle | Mature AI engineering platform polish | Combine traces, evals, deployments as one workflow | Do not claim LangSmith integration | Strong reference for a future Metrics + Trace narrative | High | Yes | Public product page. |
| R08 | Langfuse Observability Platform | https://langfuse.com/ | 2026-07-04 | Success | `.local/reference_screenshots/007_langfuse_observability_platform.png` | Trace Timeline, Evaluation / Metrics | Traces, evals, prompts, datasets, dashboards | Dense observability console style | Filterable traces and evaluation cards | Do not copy exact UI, charts, names, or logo | Best fit for trace/eval/dashboard visual direction | High | Yes | Public product page. |
| R09 | Helicone LLM Observability | https://www.helicone.ai/ | 2026-07-04 | Success | `.local/reference_screenshots/008_helicone_llm_observability.png` | Dashboard, Evaluation / Metrics | Request logs, latency/cost/error observability | Developer SaaS metrics dashboard | Show provider/fallback/latency as first-class metrics | Do not claim token cost or real model usage in current project | Good model for provider boundary and local-rule fallback reporting | High | Yes | Public product page. |
| R10 | Arize Phoenix | https://arize.com/phoenix/ | 2026-07-04 | Success | `.local/reference_screenshots/009_arize_phoenix.png` | Trace Timeline, Evaluation / Metrics | Open-source tracing/evaluation positioning | Technical observability product page | Link traces to eval and troubleshooting | Do not claim Phoenix integration | Useful for explaining next-stage real provider observability | Medium | Partial | Public product page. |
| R11 | OpenTelemetry Demo Screenshots | https://opentelemetry.io/docs/demo/screenshots/ | 2026-07-04 | Success | `.local/reference_screenshots/010_opentelemetry_demo_screenshots.png` | Trace Timeline, Retrieval Evidence | Trace viewer, service maps, Grafana/Jaeger examples | Real observability screenshots | Hierarchical trace and span inspection | Do not overfit to infra APM or claim distributed tracing runtime | Helps keep Trace page evidence-like instead of decorative | High | Yes | Public docs screenshots. |
| R12 | Vercel AI Templates | https://vercel.com/templates/ai | 2026-07-04 | Success | `.local/reference_screenshots/011_vercel_ai_templates.png` | README / Portfolio, Dashboard | Template gallery and demo-first presentation | Clean developer platform browsing | Use cards to present demo scenarios and source links | Do not copy Vercel templates or design system | Helps README showcase flow and demo entry points | Medium | Partial | Public template listing. |
| R13 | Supabase Product Site | https://supabase.com/ | 2026-07-04 | Success | `.local/reference_screenshots/012_supabase_product_site.png` | Dashboard, Knowledge Base | Product primitives grouped by developer tasks | Crisp dark developer platform style | Surface database/auth/storage-like capability boundaries | Do not imply Supabase is used | Useful for capability grouping and local/provider boundary chips | Medium | Partial | Public product page. |
| R14 | PostHog Product Analytics | https://posthog.com/product-analytics-explorer | 2026-07-04 | Success | `.local/reference_screenshots/013_posthog_product_analytics.png` | Dashboard, Evaluation / Metrics | Product analytics dashboard and insight hierarchy | Data-dense but readable metrics surface | Metric cards, trend panels, drill-down framing | Do not copy illustration assets or PostHog brand voice | Strong reference for Evaluation / Metrics page | High | Yes | Public product page. |
| R15 | Linear Product Tool | https://linear.app/ | 2026-07-04 | Success | `.local/reference_screenshots/014_linear_product_tool.png` | Ticket Workbench, Human Review | Issue queue, priority, cycle/workflow framing | Minimal dark/productivity UI | Fast triage, keyboard-like density, issue focus | Do not copy Linear layout wholesale or brand language | Good reference for ticket queue polish and issue state clarity | High | Yes | Public product page. |
| R16 | Raycast Product Site | https://www.raycast.com/ | 2026-07-04 | Success | `.local/reference_screenshots/015_raycast_product_site.png` | Dashboard, Human Review | Command palette / action-oriented product flow | Polished dark utility product style | Make actions feel deliberate and reversible | Do not copy app visuals, icons, or slogans | Useful for Human Review action affordances and compact command feel | Medium | Partial | Public product page. |
| R17 | GitHub Copilot Features | https://github.com/features/copilot | 2026-07-04 | Success | `.local/reference_screenshots/016_github_copilot_features.png` | Human Review, README / Portfolio | Human-in-the-loop coding assistant framing | Developer AI assistant credibility | Present AI suggestions with keep/undo/review boundaries | Do not claim Copilot-like coding agent capability | Good reference for "assistant suggests, human confirms" language | Medium | Partial | Public product page. |
| R18 | Open WebUI GitHub README | https://github.com/open-webui/open-webui | 2026-07-04 | Success | `.local/reference_screenshots/017_openwebui_github_readme.png` | README / Portfolio, Knowledge Base | Open-source README screenshot and feature narrative | GitHub README structure | Use screenshot + capability + boundary sections | Do not copy README structure verbatim or badges | Helps calibrate GitHub portfolio readability | Medium | Partial | Public GitHub README. |
| R19 | Langfuse GitHub README | https://github.com/langfuse/langfuse | 2026-07-04 | Success | `.local/reference_screenshots/018_langfuse_github_readme.png` | README / Portfolio, Evaluation / Metrics | Open-source observability README structure | Clear product proof and docs links | Pair screenshots with eval/trace claims and limits | Do not copy metrics claims or screenshots | Best README reference for trace/eval wording discipline | High | Yes | Public GitHub README. |

## Page-Level Reference Binding

### Dashboard

- Reference A: Langfuse Observability Platform, https://langfuse.com/
- Reference B: Helicone LLM Observability, https://www.helicone.ai/
- Reference C: PostHog Product Analytics, https://posthog.com/product-analytics-explorer
- Borrow: metric hierarchy, provider/latency/fallback cards, trace/eval status, compact activity feed.
- Project landing advice: keep the Dashboard as a command center, but make demo/local-rule/provider boundaries visible as status chips rather than explanatory paragraphs.

### Ticket Workbench

- Reference A: Linear Product Tool, https://linear.app/
- Reference B: GitHub Copilot Features, https://github.com/features/copilot
- Reference C: Open WebUI Product Site, https://openwebui.com/
- Borrow: dense issue queue, selected ticket focus, assistant suggestion panel, human confirmation controls.
- Project landing advice: keep the three-column layout; improve queue scan density, evidence prominence, and action states without adding generic admin table chrome.

### Knowledge Base

- Reference A: Dify Knowledge Base Docs, https://docs.dify.ai/en/guides/knowledge-base
- Reference B: AnythingLLM Product Site, https://anythingllm.com/
- Reference C: LlamaCloud Document Platform, https://www.llamaindex.ai/llamacloud
- Borrow: source organization, workspace/document mental model, ingestion/index/retrieval wording.
- Project landing advice: separate "knowledge source", "retrieved chunk", "citation", and "demo keyword retrieval" so the UI does not imply real vector search.

### Retrieval Evidence

- Reference A: Flowise Agentflow Builder, https://flowiseai.com/
- Reference B: OpenTelemetry Demo Screenshots, https://opentelemetry.io/docs/demo/screenshots/
- Reference C: LangSmith Platform, https://www.langchain.com/langsmith-platform
- Borrow: retrieval as a step in a workflow, source-hit inspection, evidence chain and fallback status.
- Project landing advice: add a future evidence panel that shows expected knowledge ids, matched snippets, citation precision state, and fallback reason.

### Trace Timeline

- Reference A: Langfuse Observability Platform, https://langfuse.com/
- Reference B: LangSmith Platform, https://www.langchain.com/langsmith-platform
- Reference C: OpenTelemetry Demo Screenshots, https://opentelemetry.io/docs/demo/screenshots/
- Borrow: hierarchical traces, step timing, request/response summaries, tool/provider call panels.
- Project landing advice: keep trace visible as audit evidence; avoid claiming distributed tracing or full span runtime unless implemented.

### Human Review

- Reference A: Linear Product Tool, https://linear.app/
- Reference B: Raycast Product Site, https://www.raycast.com/
- Reference C: GitHub Copilot Features, https://github.com/features/copilot
- Borrow: decision queue, compact action controls, review-before-apply pattern, reversible action language.
- Project landing advice: make reviewer decision states and risk reasons more prominent than decorative AI badges.

### Evaluation / Metrics

- Reference A: Langfuse Observability Platform, https://langfuse.com/
- Reference B: Helicone LLM Observability, https://www.helicone.ai/
- Reference C: PostHog Product Analytics, https://posthog.com/product-analytics-explorer
- Borrow: metric cards, failed-case drilldown, latency distribution, dataset/eval tabs.
- Project landing advice: create a future Evaluation / Metrics page only from local demo evaluation JSON, not invented model performance numbers.

### README / Portfolio

- Reference A: Langfuse GitHub README, https://github.com/langfuse/langfuse
- Reference B: Open WebUI GitHub README, https://github.com/open-webui/open-webui
- Reference C: Vercel AI Templates, https://vercel.com/templates/ai
- Borrow: screenshot-first story, clear feature sections, public demo/readme navigation, honest limitations.
- Project landing advice: README should continue using only this repo's own `docs/images/` screenshots and cite metrics from local evaluation artifacts.

## Recommended Phase-One Directions

1. Langfuse trace/eval direction
   - URL: https://langfuse.com/
   - Pages: Dashboard, Trace Timeline, Evaluation / Metrics
   - Why: best match for LLM observability, trace, prompt/eval, and dataset concepts.
   - Borrow: dense trace/eval cards, filtered audit views, run detail hierarchy.
   - Do not copy: Langfuse product UI, exact labels, logo, screenshots, or hosted-platform claims.

2. Helicone provider metrics direction
   - URL: https://www.helicone.ai/
   - Pages: Dashboard, Evaluation / Metrics
   - Why: strong model/request observability framing.
   - Borrow: provider health, latency, fallback, request table structure.
   - Do not copy: token-cost claims unless real provider tests exist.

3. Linear ticket operations direction
   - URL: https://linear.app/
   - Pages: Ticket Workbench, Human Review
   - Why: excellent issue/workflow density and state clarity.
   - Borrow: queue rhythm, selected item focus, compact priority/status language.
   - Do not copy: Linear interaction model, marks, typography, or brand treatment.

4. PostHog metrics direction
   - URL: https://posthog.com/product-analytics-explorer
   - Pages: Dashboard, Evaluation / Metrics
   - Why: readable metrics dashboard for non-toy product analytics.
   - Borrow: metric grouping, trend cards, drilldown panels.
   - Do not copy: illustrations, copy tone, or product-specific analytics claims.

5. OpenTelemetry evidence direction
   - URL: https://opentelemetry.io/docs/demo/screenshots/
   - Pages: Trace Timeline, Retrieval Evidence
   - Why: real trace and observability examples keep evidence UI grounded.
   - Borrow: trace hierarchy, spans, service/pipeline visibility.
   - Do not copy: claim distributed tracing or APM runtime before implementation.

6. Flowise workflow direction
   - URL: https://flowiseai.com/
   - Pages: Retrieval Evidence, Trace Timeline
   - Why: visual workflow language helps explain RAG/provider/review steps.
   - Borrow: modular step chain and clear current step.
   - Do not copy: node canvas UI wholesale.

7. Langfuse README direction
   - URL: https://github.com/langfuse/langfuse
   - Pages: README / Portfolio, Evaluation / Metrics
   - Why: strong open-source AI engineering README model.
   - Borrow: screenshot + capabilities + docs + limitations structure.
   - Do not copy: screenshots, badges, metrics, or hosted usage claims.

## Not Recommended As Primary Visual Sources

- Dify official homepage: not recommended for this round because Playwright capture failed due navigation instability; keep only as URL for future manual review.
- Dify Knowledge Base Docs: useful for terminology, not enough for visual direction because it is documentation-first.
- AnythingLLM homepage: useful for knowledge/workspace concept, but too product-marketing oriented for dense enterprise ticket UI.
- Vercel AI Templates: useful for README/demo-gallery thinking, but not a ticket/RAG console reference.
- Supabase homepage: useful for developer platform grouping, but not directly aligned with trace/eval/human-review workflows.
- Raycast homepage: visually polished, but command-launcher product structure differs from an enterprise workbench.
- GitHub Copilot features page: useful for human confirmation language, but not a dashboard/workbench layout source.

## Next Design Suggestions

- `docs/frontend_reference_research.md`: recommended next. It should turn the screenshot index into deeper research notes, product patterns, and avoid/copy boundaries.
- `docs/frontend_moodboard.md`: recommended after research. It should group the selected visual directions, color/token ideas, density rules, and screenshot goals.
- `docs/frontend_showcase_design.md`: recommended after moodboard approval. It should become the page-level design spec before any Vue/CSS changes.
- Current repository does not have these exact three documents. Existing `docs/design/ui-reference-library.md` and related files are older internal references and should be updated or superseded only after user confirmation.

## Git And Asset Safety Notes

- `.local/` is ignored by `.gitignore`.
- `.local/reference_screenshots/` is local-only and must stay untracked.
- This index document records URLs and local screenshot filenames for review; it does not embed third-party images.
- No frontend source, CSS, route, README screenshot, or screenshot script is part of this reference collection task.
