# 前端页面规格

> 版本：2026-09-03  
> 设计基线：`FRONTEND_REDESIGN_MASTER_PLAN.md`、`docs/frontend_showcase_design.md`、`docs/design/ui-style-guide.md`  
> 目标：把当前真实 API / Demo fixture 证据组织成企业级 AI 支持运营工作台。

## 1. 统一设计系统

### 视觉方向

采用参考案例导向的“浅色中性客服工作台”方向：浅灰 canvas、白色内容面板、克制蓝色操作、低饱和状态色、紧凑但不拥挤的卡片和连续上下文。页面应让人感觉在处理真实工单和运行证据，而不是浏览营销落地页或科技大屏。

### Token

| Token | 值 | 用途 |
| --- | --- | --- |
| `--app-canvas` | `#f7f7f4` | 全局背景 |
| `--app-sidebar` | `#f0f1ee` | 左侧导航 |
| `--app-panel` | `#ffffff` | 主面板 |
| `--app-panel-raised` | `#fbfbf9` | 选中/强调面板 |
| `--app-line` | `#e4e6e0` | 默认分隔线 |
| `--app-line-strong` | `#d1d5ce` | 选中、键盘焦点 |
| `--app-text` | `#1f2630` | 主文本 |
| `--app-text-soft` | `#596572` | 次级文本 |
| `--app-muted` | `#858d95` | 说明和低优先级元数据 |
| `--app-blue` | `#315bb2` | 主操作/链接 |
| `--app-cyan` | `#2c7884` | 证据/系统状态 |
| `--app-green` | `#2c7657` | 成功/已验证 |
| `--app-amber` | `#a66a1d` | 待处理/注意 |
| `--app-red` | `#b9575e` | 风险/失败 |
| `--app-violet` | `#725fa0` | AI/评测 |
| `--app-coral` | `#bd7158` | 自有品牌识别点 |

规则：主色只用于交互和状态，不做大面积霓虹渐变；卡片圆角 10–12px；边框比阴影更重要；标题使用 `Aptos Display` / `Segoe UI Variable Display` 本地字体栈，辅助文本不低于可读灰度；数字、ID、延迟和 run ID 使用等宽字体；品牌和导航图标使用自绘 SVG。

### 共享组件约定

- `AppSidebar`：品牌、主导航、待复核计数、Demo/Real 边界、工作区身份。
- `AppTopbar`：页面标题、面包屑、runtime badge、Provider badge、全局快捷搜索入口。
- `PageHeader`：eyebrow、标题、描述、右侧动作，所有页面结构一致。
- `PanelHeader`：标题、说明、数量/状态和局部操作。
- `StatusBadge`：统一 `success/info/warning/danger/neutral` 语义。
- `MetricCard`：数字、标签、delta/来源说明；不能仅用装饰性 KPI。
- `EvidenceRow`：编号、标题、来源、score/validation、excerpt。
- `EmptyState` / `LoadingState` / `ErrorState`：明确区分等待、没有结果和请求失败。
- `KeyValueList`：低频技术字段统一收纳，长 ID 可换行或使用 `title`。

### 全局状态

| 状态 | 视觉 | 文案原则 |
| --- | --- | --- |
| Loading | skeleton 或低动效 shimmer | 说明正在请求的资源，不写“系统正常” |
| Empty | 中性图标/边框 | 说明为什么为空、下一步是什么 |
| Error | 红色窄提示 + 重试按钮 | 使用后端安全错误，不显示敏感原始响应 |
| Demo | neutral/lavender badge | 明确“浏览器内存合成数据”，不写持久化 |
| Real | green/blue badge | 明确“Backend API”，事实以 API 为准 |
| Review required | amber/red badge | 解释按钮为何可用/不可用 |

## 2. 共享壳层规格

- 左栏宽度 224–240px；桌面固定、移动端转为顶部横向导航。
- 顶部栏高度 58–64px；只保留当前上下文、运行模式、Provider 边界和快捷搜索，不重复长段落。
- 内容最大宽度约 1720px，左右 padding 14–22px；禁止页面根节点横向滚动。
- 选中导航项使用浅蓝背景 + 蓝色左侧标记；状态色只承担语义，不用渐变填充承载全部信息。
- 所有页面必须有 `main`、可跳过链接、语义标题层级，动作使用 button，页面跳转使用 anchor。
- 移动端：侧栏导航横向可滚动；多栏页面变为“队列 → 内容 → 证据/动作”的顺序；技术元数据使用 `details`。

## 3. Dashboard

### 任务与结构

目标是回答“系统现在有没有需要处理的事，以及证据链是否健康”。

1. PageHeader：`Operations overview`、当前运行模式、刷新。
2. Signal strip：总工单、待复核、当前列表 pending、AI hit rate；每项必须标记来源。
3. Main split：左侧“优先处理队列”展示最多 5 条真实列表；右侧“Evidence health”展示有无 Trace、引用校验、Provider 边界。
4. Bottom grid：状态分布、最近活动、下一步入口；避免做没有数据来源的折线图。

数据绑定：全部来自 `useTicketRealFlow()` 和 `evaluationMetrics.ts` 的本地评测快照。Dashboard 不编造 SLA、token 消耗、生产流量。

验收重点：Demo 首屏有数据；Real 后端不可用时显示安全错误；点击工单能进入 Workbench 并保持选中项。

## 4. Ticket Workbench

### 任务与结构

这是主页面，重点借鉴企业支持产品常见的队列 + 上下文 + Copilot 侧栏结构，但不复制品牌布局。

1. PageHeader：工单工作台、队列数量、Demo/Real badge、创建合成工单入口。
2. Queue rail：关键字搜索、状态过滤、优先级过滤、结果数；行显示 ID、标题、status、priority、requester、更新时间、confidence。
3. Ticket context：标题、状态/优先级 badge、requester/system/category 元数据；描述、错误日志、状态时间线。
4. Copilot rail：决策主卡（推荐处理、分类置信度、运行按钮）；建议回复；关联 retrieval evidence；独立的风险 / 复核门禁卡（风险等级、置信度、validated citation 数量）；人工复核动作；低频 run metadata 使用 `<details>`。
5. Below fold：创建表单、结构化输出、检索证据、review history；保持 `data-e2e` 选择器。

真实绑定：`fetchTickets`、`fetchTicket`、`fetchAiAnalysis`、`fetchTraceEvidence`、`runCopilot`、`createTicket`、三个 review endpoint。前端不绕过 API。

交互：

- 搜索和过滤只作用于已获取列表，不假装服务端全文检索。
- Copilot 运行中按钮 disabled，并显示 `aria-busy`。
- 非 `REVIEW_REQUIRED` / `AI_DRAFTED` 时审核按钮 disabled，并给出原因。
- Approve / Request changes / Reject 保留确认和 comment 校验；成功后重新读取 detail/list/trace。

## 5. Knowledge Base

目标是让支持人员理解“当前工单命中了哪些知识，以及哪些检索快照进入了草稿”。

- 左列：AI knowledge hits，展示 title、relevance、owner、verified time。
- 中列：RAG references，展示 article、rank、keyword、score、usedInDraft、excerpt。
- 右列：当前工单上下文和边界说明；没有独立知识库 CRUD 时明确只读。
- 提供跳转 Trace 和返回 Workbench，不提供未实现的上传/删除按钮。

空态必须区分“尚未运行 Copilot”和“运行后没有知识命中”。

## 6. Retrieval Evidence

目标是明确区分两层证据：

- `Retrieval Reference`：后端持久化/本地 Demo 派生的检索快照。
- `Validated Model Citation`：后端结构化结果中通过 membership validation 的引用。

页面采用左右对照 + 下方校验摘要：证据行包含 rank、article、score、excerpt；校验摘要包含 output status、citation status、valid/rejected count；边界文案说明不代表事实蕴含验证。

## 7. Trace Timeline

目标是回答“这次 Copilot 运行发生了什么”。

- 顶部：当前 ticket、trace/run ID、run status、total latency、evidence source。
- 主区：步骤时间线，显示 step name、source type、status、latency、summary。
- 侧区：运行元数据、fallback、error category、review gate；默认展示摘要，原始字段放 details。
- 下方：retrieval snapshot、structured output、status history、review records。

Demo 页面必须明确它是“派生回放”；Real 页面必须保留 `IMMUTABLE_RUN` / `LEGACY_DERIVED` 的后端事实。

## 8. Human Review

目标是让审核人快速判断“建议能否被执行”。

- 左列：待复核队列，显示风险、状态、AI confidence、更新时间。
- 中列：建议回复、结构化字段、风险说明、证据引用。
- 右列：知识参考、相似工单（只有 API 有数据时显示）、review comment 输入、三个决策按钮。
- Demo fixture 至少提供一个 reviewable case，确保截图和浏览器验收可重复。

按钮状态：未选中/非待复核 disabled；comment 为空时 Request changes / Reject 不可提交；提交中全部锁定；成功显示新状态和 history。

## 9. Evaluation / Metrics

保留当前 `evaluationMetrics.ts` 的事实范围，但采用和其他页面一致的 shell/panel 语言：

- 顶部显示 synthetic dataset、Top-K、local keyword retrieval、citation gating、fallback。
- 第一行显示 Top-K hit、citation coverage、citation precision、failed cases。
- 中部展示 baseline 表和补充指标；将“下一阶段 Hybrid/embedding/Vector DB/Rerank/真实 Provider”明确标为规划，不冒充已实现。
- 右侧上下文不重复大段 boundary；保留数据集路径、生成时间和无真实用户数据声明。

## 10. 外部产品参考的借鉴边界

本轮只借鉴信息架构和操作节奏，不复制 logo、插画、品牌色或原页面代码：

- [Intercom Inbox explained](https://www.intercom.com/help/en/articles/6258745-the-inbox-explained)：队列/表格视图、过滤、从列表直接采取动作、右侧上下文。
- [Intercom AI features in the Inbox](https://www.intercom.com/help/en/articles/6955446-ai-features-available-in-the-inbox)：把 Copilot 放进支持工作流，并将知识上下文与回复动作并置。
- [Jira Service Management queues](https://support.atlassian.com/jira-service-management-cloud/docs/check-out-your-queues/)：队列用于 triage、assign 和 SLA/优先级上下文。
- [Linear features](https://linear.app/features)：紧凑列表、键盘优先、低噪声状态和清晰的上下文层级。
- Zendesk Agent Workspace、ServiceNow CSM/ITSM、GitHub Copilot/Actions、OpenTelemetry/Langfuse 等仅作为产品类别参考；本项目不接入它们的账号、组件或资源。

## 11. 浏览器截图验收

每页至少截图：标准桌面、1920×1200 大屏、1366×900 桌面验收、390×844 移动；Workbench 和 Human Review 要覆盖 populated/interactive 状态。截图文件写入 `docs/images/`、`docs/images/large/` 与 `docs/images/mobile/`，来源记录在 `docs/TEST_REPORT.md` 和 `HANDOFF.md`。

检查项：

- 首屏能看出当前页面和运行模式。
- 关键按钮不被折叠或裁切；运行中/禁用态可见。
- 1366px 和 390px 无横向滚动。
- 长 ID、引用 excerpt、错误文案可以换行。
- 键盘 Tab 有清晰焦点，`prefers-reduced-motion` 下无持续动效。
- Demo 和 Real 文案没有互相冒充；没有外部图片、logo 或远程字体依赖。

## 12. 逐页实施规格矩阵

下面的矩阵把每个核心页面的产品目标、数据字段、状态、响应式和浏览器验收要求固化下来。参考案例只贡献信息架构和操作节奏，不复制其源码、Logo、品牌色、插画、文案或远程资源。

### 12.1 Dashboard

- 页面目标：回答“现在有哪些工单需要处理，证据链是否健康”。目标用户是支持主管、值班工程师和项目面试评审者；主任务是快速定位待复核工单并进入 Workbench。
- 参考与借鉴：Jira Service Management queues 的 triage/优先级语义，Linear 的紧凑列表和低噪声状态表达。
- 布局与组件：`PageHeader` → `MetricCard` signal strip → “优先处理队列 / Evidence health” split → 状态分布和活动 panel；复用 `StatusBadge`、`LoadingState`、`EmptyState`、`ErrorState`。
- 信息与字段：`TicketSummary.id/title/status/priority/category/aiConfidence/updatedAt`、`WorkbenchMetrics.pendingTickets/aiHitRate/knowledgeCoverage/todayKnowledgeDrafts`、当前 ticket 的 Trace/Citation 状态。
- Demo/Real/Fallback：Demo 标记 `Local Demo Fixture` 和浏览器内存列表；Real 标记 `Backend API`，只显示 `/health`、`/tickets`、`/tickets/metrics` 返回的事实；失败只进入 Error，不补静态 Mock。
- 状态：首次读取 Loading；真实列表为空 Empty；后端不可用 Error + 重试；有数据 Success；跳转 Workbench 的按钮在无选中项时保持可用但目标明确；指标不以颜色作为唯一含义。
- 桌面/390px：桌面使用 2 列主区和 4 张紧凑指标卡；390px 改为单列，指标/队列/Evidence 依次堆叠，操作按钮换行且不超出 viewport。
- 键盘与截图：`main`、跳过链接、语义 heading、按钮/anchor 区分、`:focus-visible`；截图验收首屏可见运行模式、待复核数量、至少一条队列和 Evidence boundary，标准/large/mobile 三套均无溢出。

### 12.2 Ticket Workbench

- 页面目标：在一次连续工作区中完成 queue → TicketDetail → Copilot → evidence → Human Review。目标用户是 IT 支持人员、审核人和运维值班人员；主任务是处理当前工单并决定是否采纳建议。
- 参考与借鉴：Intercom Inbox 的三栏队列/上下文/动作节奏，Zendesk Agent Workspace 的单工单上下文，Jira 的 IT 服务队列语义和 Linear 的搜索/密度。
- 布局与组件：左 `queue rail`（搜索、状态/优先级筛选、结果数），中 TicketDetail（元数据、描述、日志、时间线），右 Copilot rail（建议、信号、证据预览、复核）；下方 `Create synthetic`、结构化结果和 append-only history。
- 信息与字段：`TicketSummary` 全字段；`TicketDetail.id/title/status/priority/requester/department/systemContext/description/errorLogs/timeline`；`AiAnalysis.classification/confidence/knowledgeHits/replySuggestion/riskLevel/abstentionReasonCode`；`TraceEvidence` 的 run/provider/latency/fallback/RAG/Citation/review 字段。
- Demo/Real/Fallback：Demo 所有动作走 `demoTickets.ts` 并标识 `DEMO_LOCAL`；Real 只走现有 `/api` 与内存 session；后端错误只显示安全错误，不静默切换 Demo；Provider Key 永不进入前端。
- 状态：队列/详情/动作分别有 Loading、Empty、Error；run 中按钮 disabled + `Copilot 运行中…`；非 `REVIEW_REQUIRED`/`AI_DRAFTED` 的审核按钮 disabled；确认、成功回读、失败提示和表单校验均可见。
- 桌面/390px：桌面保持三栏；390px 改为 queue → detail → Copilot/evidence → create/result/history 单列，`details` 收纳低频 audit fields，长 ID/excerpt 可换行。
- 键盘与截图：保留 `data-e2e` 选择器，搜索、按钮、textarea 可 Tab 到达且有 focus ring；Workbench 截图必须能看到队列/上下文/Copilot 三层，移动截图必须能看到状态和主按钮。

### 12.3 Knowledge Base

- 页面目标：解释当前工单命中了哪些知识、哪些检索快照被用于草稿。目标用户是支持人员和知识负责人；主任务是判断知识上下文是否足够，不执行未实现的知识 CRUD。
- 参考与借鉴：Intercom AI features 的工作流内知识上下文，ServiceNow 的企业上下文 panel；只借鉴“证据伴随任务”的信息层级。
- 布局与组件：左 AI knowledge hits，中 RAG references，右 current ticket context/boundary；证据行使用 panel header、score badge 和 excerpt。
- 信息与字段：`AiAnalysis.knowledgeHits[id/title/relevance/owner/lastVerifiedAt]`；`TraceEvidence.ragReferences[articleNo/knowledgeTitle/matchedKeyword/relevanceScore/snippet/usedInDraft/linkedRunId]`。
- Demo/Real/Fallback：Demo 标明 fixture；Real 仅从当前 ticket 的 analysis/Trace API 读取；没有独立 article list API 时明确“只读”，不提供假上传/删除按钮；网络失败不 fallback。
- 状态：尚未运行 Copilot 与运行后无命中必须区分；读取 Loading；空命中 Empty；API Error；有命中 Success；Trace/Workbench anchor 始终给出下一步。
- 桌面/390px：桌面 3 列证据对照；390px 按 AI hits → RAG refs → context 顺序单列，每条 excerpt 自然换行，避免横向滚动。
- 键盘与截图：证据标题不是伪按钮；链接有可见 focus；截图必须展示只读边界、至少一条证据或明确空态，并显示 Demo/Real badge。

### 12.4 Retrieval Evidence

- 页面目标：把 `Retrieval Reference` 与 `Validated Model Citation` 分开，避免把召回命中误读成模型事实正确。目标用户是审核人、调试人员和评测人员；主任务是检查 Citation membership validation 结果。
- 参考与借鉴：Intercom 的工作流内上下文并置、Linear 的低噪声证据列表；校验语义沿用当前后端契约，不引入新的 RAG 算法。
- 布局与组件：顶部 ticket/trace chips 和 validation strip；左右两层证据 panel；下方 AI analysis/context boundary/next step；复用 `StatusBadge`、`EmptyState`、`ErrorState`。
- 信息与字段：`TraceEvidence.ragReferences` 的 rank/article/score/excerpt/usedInDraft；`validatedCitations` 的 resultCitationId/retrievalHitId/citationType/knowledgeArticleId/knowledgeTitle/evidenceExcerpt；structured output 的 `outputValidationStatus/citationValidationStatus/validCitationCount/rejectedCitationCount`。
- Demo/Real/Fallback：Demo 明确 `DEMO_LOCAL` 和派生 trace；Real 保留 `IMMUTABLE_RUN` 或 `LEGACY_DERIVED` 原值；Fallback/未运行时显示未返回，不将 Retrieval Reference 伪装为 Validated Citation。
- 状态：两侧分别 Loading/Empty/Error/Success；无 validated citation 时说明“未返回/未适用/校验失败”而不是绿色成功；下一步链接到 Trace。
- 桌面/390px：桌面左右对照；390px 先 Retrieval Reference、后 Validated Citation、再 validation/boundary，所有 ID 可折行。
- 键盘与截图：validation 不依赖颜色，使用文字、计数和状态标签；截图必须同时出现两层标题和“不代表事实蕴含验证”的边界文案。

### 12.5 Trace Timeline

- 页面目标：回答“这次 Copilot run 发生了什么”。目标用户是支持工程师、审计人员和开发者；主任务是回放步骤、状态、延迟、Provider/fallback 和 review。
- 参考与借鉴：ServiceNow 的企业活动上下文、GitHub Actions 的步骤状态节奏、Linear 的紧凑 timeline；不接入第三方 Trace 服务。
- 布局与组件：顶部 run overview；主区 step timeline；侧区 run metadata/fallback/review gate；下方 retrieval、structured output、status history、review records；低频 raw fields 使用 `details`。
- 信息与字段：`TraceEvidence.runId/traceId/traceMode/currentStep/totalLatency/evidenceSource`、`stepTimeline`、`copilotRun`、`generationRecords`、`statusHistory`、`reviewRecords`、structured/Citation 字段。
- Demo/Real/Fallback：Demo 使用 `demo-local-derived`/`demo-local-run` 与 `DEMO-*`；Real 原样展示 `IMMUTABLE_RUN`/`LEGACY_DERIVED`；Provider fallback 原样映射 safe reason，不在前端重算。
- 状态：未运行为派生/空 timeline；请求 Loading；API Error；有步骤 Success；失败/abstention/review required 使用文字状态和下一步提示；页面只读，不触发 rerun。
- 桌面/390px：桌面 timeline + metadata 两区；390px 改为 overview → timeline → evidence → metadata，run/trace ID 使用等宽字体并可换行。
- 键盘与截图：时间线使用 heading/`time` 语义，`details` 可键盘展开，focus-visible 清晰；截图必须可读出至少 3 个步骤、run status、latency、evidence source 和 review gate。

### 12.6 Human Review

- 页面目标：让审核人基于建议、风险和证据做最终决定。目标用户是 reviewer、支持主管和合规/运营负责人；主任务是 Approve、Request changes 或 Reject，并留下可追溯备注。
- 参考与借鉴：Intercom AI 工作流中的人工确认节点、ServiceNow 的活动/上下文审计；动作语义完全使用本项目现有 review API。
- 布局与组件：左待复核队列；中建议/结构化输出/风险/引用；右 comment + 三个决策动作 + history；使用 `StatusBadge`、`PanelHeader`、`ErrorState`。
- 信息与字段：可复核 `TicketSummary`；`TicketDetail` 的 status/priority/description；`AiAnalysis` 的 answer/risk/missingInformation；`TraceEvidence` 的 citations/ragReferences/reviewRecords/copilotRun。
- Demo/Real/Fallback：Demo 固定 `REVIEW_REQUIRED` fixture，review history 留在浏览器内存并标 `DEMO_LOCAL`；Real 只调用三个 review endpoint 并重新读取；失败显示安全错误，绝不自动 approve。
- 状态：队列 Loading/Empty/Error；未选中或非 reviewable 时动作 disabled；提交中所有决策按钮锁定；确认后 Success + 新状态/history；comment 为空时 Request changes/Reject 阻止提交。
- 桌面/390px：桌面 queue → decision context → action rail；390px queue → suggestion/evidence → comment/actions → history，主操作保持在可见顺序内，不固定遮挡内容。
- 键盘与截图：textarea、三按钮和确认对话框可键盘操作，按钮文本表达动作而非只用颜色；截图必须包含待复核 ticket、风险、证据、comment 和至少一个可用 action。

### 12.7 Evaluation / Metrics

- 页面目标：展示当前本地评测 baseline 的指标和限制，不伪装成生产模型质量。目标用户是开发者、面试评审者和项目负责人；主任务是理解指标口径与下一阶段实验。
- 参考与借鉴：Linear 的信息密度和上下文分组、Stripe/企业报表的摘要优先结构；指标定义沿用 `evaluationMetrics.ts` 和 `docs/metrics/`。
- 布局与组件：dataset/boundary header → 核心 KPI strip → baseline/scope → failed cases/review counts → reproduction command/next stage；使用现有页面 scoped components + 新壳层。
- 信息与字段：`evaluationMetrics.ts` 中的 samples、topK、hit rate、context recall、citation coverage/precision、latency、failed/review counts、dataset path、generatedAt 和 scope labels。
- Demo/Real/Fallback：这是本地评测 artifact，不请求后端、不接 Provider；统一标记 synthetic/local keyword retrieval/citation gating/local-rule fallback；未来 Vector/Hybrid/Rerank/真实 Provider 只显示为规划。
- 状态：本地数据加载成功 Success；数据缺失显示 Error/待生成；指标为空 Empty；规划项 Disabled/roadmap，不做可点击假动作。
- 桌面/390px：桌面 KPI + baseline 双列；390px KPI、scope、failure explanation、reproduction 依次单列，长指标标签可换行。
- 键盘与截图：表格/链接有语义和 focus，数字同时有标签和解释；截图必须可见 synthetic dataset、至少四项核心指标、baseline 边界和 reproduction command。
