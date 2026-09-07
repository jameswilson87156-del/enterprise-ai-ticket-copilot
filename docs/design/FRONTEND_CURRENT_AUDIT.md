# 前端现状审查

> 审查日期：2026-09-03（网络中断后复核）  
> 审查范围：`frontend/`、当前截图、`docs/design/` 设计约束、真实接口与 Demo fixture 边界  
> 本轮目标：在不改变后端业务链路的前提下，完成一套可运行、可浏览器验收的企业工单 AI 运营工作台。

> 审查性质：这是本轮全量重做前的基线审查；“问题清单”记录改造前事实，“验收基线”记录改造后的可验证目标。

## 1. 结论摘要

当前前端已经具备真实闭环的最小能力：工单列表、工单详情、AI 分析、Trace Evidence、Copilot 运行、结构化输出、Citation 校验摘要、人工复核和 review history 均通过 `useTicketRealFlow` 共享状态连接。Demo 模式走浏览器内存 fixture，真实模式只走 `/api`，没有用静态 Mock 替换后端失败。

本轮不重写数据层，也不增加后端接口。主要问题集中在信息架构和可操作性：

1. `App.vue` 同时承担路由、壳层、运行边界提示和评测上下文，页面入口可以运行，但复用边界不清晰。
2. Ticket Workbench 已经是三栏，但队列缺少搜索、状态/优先级过滤和清晰的选择态；详情区和 Copilot 区的纵向层级不够紧凑。
3. Copilot 右栏当前把大量 `runId`、状态字段平铺成元数据墙，用户最需要的“建议、证据、风险、下一步”被挤到下面。
4. Knowledge、Retrieval、Trace、Human Review 页面能够如实显示 API 返回，但仍更像 API 字段检查器，而不是面向支持人员的操作页面。
5. Human Review 首屏依赖当前选中工单；Demo 初始队列没有稳定的 `REVIEW_REQUIRED` / `AI_DRAFTED` 样本，浏览器验收难以直接验证审核动作。
6. 旧的全局浅色样式仍保留在 `frontend/src/styles.css` 前部，后续深色样式通过追加覆盖生效，维护成本和选择器冲突风险偏高。
7. 各页虽然有部分空态和错误文案，但 loading、disabled、`aria-busy`、`aria-live`、键盘焦点和移动端折叠没有统一验收基线。

## 2. 当前路由与数据绑定

路由采用 hash 映射，未引入 Vue Router；本轮保留既有 URL 兼容别名。

| 页面 | 入口 hash | 当前组件 | 真实来源 | Demo 来源 | 关键动作 |
| --- | --- | --- | --- | --- | --- |
| Dashboard | `#dashboard` | `DashboardShowcaseView.vue` | `/health`、`/tickets`、`/tickets/metrics` | `demoTickets.ts` 内存列表和指标 | 刷新、跳转工作台 |
| Ticket Workbench | `#ticket-detail`、`#ticket-workbench` | `TicketWorkbenchShowcaseView.vue` | tickets、detail、analysis、trace、run、review API | 同名 demo API | 选择、创建、运行 Copilot、复核 |
| Knowledge Base | `#knowledge-base`、`#knowledge` | `KnowledgeRagShowcaseView.vue` | 当前工单的 AI knowledge hits + Trace RAG references | demo 分析和派生 trace | 只读浏览、跳转 Trace |
| Retrieval Evidence | `#retrieval-evidence`、`#evidence` | `TraceShowcaseView.vue` | Trace 的 retrieval reference + validated citation | demo 派生快照 | 只读比对证据层 |
| Trace Timeline | `#trace-timeline`、`#trace` | `TraceTimelineShowcaseView.vue` | Trace、run、step/status/review evidence | demo 派生 trace | 刷新、查看运行证据 |
| Human Review | `#human-review`、`#review` | `HumanReviewShowcaseView.vue` | reviewable ticket、analysis、trace、review API | demo reviewable fixture | 选择、Approve、Request changes、Reject |
| Evaluation / Metrics | `#evaluation-metrics`、`#eval` | `EvaluationMetricsShowcaseView.vue` | `evaluationMetrics.ts` 本地评测快照 | 同一合成评测数据 | 查看 baseline、失败样本、边界 |

### 当前状态边界

- `isDemoRuntime` 由 Vite 配置决定。Demo 只使用本地内存 fixture；不会暗示数据库持久化。
- 真实模式先探测 `/api/health`，随后读取真实 API；后端不可用时展示错误/空态，不回退到 Demo。
- Provider、模型、RAG、Citation、fallback 和 Trace 事实由后端响应决定；前端只格式化和分组，不直接请求 Provider。
- `retrieval_hit` 与 `validated citation` 必须分层展示。Citation membership validation 不等于逐句事实蕴含验证。
- 本轮不新增生产依赖、Vector DB、真实 Provider、知识库 CRUD 或新的业务接口。

## 3. 现有实现资产

以下资产继续复用，不在本轮删除或回滚：

- `frontend/src/api/tickets.ts`：真实 API 与 Demo API 边界、脱敏错误处理、内存 session。
- `frontend/src/composables/useTicketRealFlow.ts`：共享 ticket/detail/analysis/trace/run/review 状态。
- `frontend/src/types/ticket.ts`：API DTO 与证据结构。
- `frontend/src/data/demoTickets.ts`：合成工单、分析、Trace、review history 的本地 fixture。
- `frontend/src/data/evaluationMetrics.ts`：本地评测快照和 provider/boundary 展示数据。
- `frontend/src/utils/realFlowFormat.ts`：安全文本、状态、时间、风险与拒答格式化。
- `docs/design/references/`：本项目已生成的页面方向参考图，仅作为构图和层级参考。

## 4. 视觉和交互问题清单

### P0：影响主流程验收

- 需要在 Ticket Workbench 首屏同时看见“队列 → 工单上下文 → Copilot 建议/证据 → 复核动作”，并确保在 1366px 宽度不产生横向滚动。
- Human Review 必须在 Demo 首屏提供可选的待复核工单，并把按钮 disabled、提交中、成功反馈和 review history 做成可重复验收路径。
- 所有主要异步区域需要具备 loading、empty、error 三种可区分状态。

### P1：影响日常操作效率

- 队列支持关键字、状态、优先级过滤，结果数量和选中态清楚。
- 右栏优先呈现建议摘要、风险门禁、知识引用和下一步动作；低频运行元数据收进可展开区域。
- Knowledge/Retrieval/Trace 页面使用统一 panel header、证据行、状态 badge 和 key-value 组件语言。
- 顶部只保留当前页面上下文、运行模式、Provider 边界和快捷搜索入口，避免每页重复解释。

### P2：影响品质与可维护性

- 以 CSS token 统一颜色、边框、间距、圆角、阴影和状态语义。
- 抽取共享壳层和 UI primitives；业务页面保留业务数据绑定。
- 增加 `prefers-reduced-motion`、明显 `:focus-visible`、语义按钮/链接、`aria-live` 和 `aria-busy`。
- 移动端采用单列和可折叠区域，不通过缩小字体硬塞三栏。

## 5. 本轮保留与本轮淘汰

### 保留

- hash 路由和所有既有别名。
- 现有 API 路径、请求方法、session 机制、Demo/Real 运行边界。
- 既有 `data-e2e` 选择器：`ticket-title`、`ticket-description`、`ticket-system`、`create-ticket`、`run-copilot`、`review-comment`、`approve-review`、`reject-review`。
- 当前文档、截图和工作区中与上一轮真实链路有关的修改。

### 弃用/不再作为页面主结构

- `App.vue` 中评测页专属的右侧上下文墙：改为通用顶部 runtime context，评测事实仍留在评测页。
- Workbench 中平铺全部运行字段的主视图：改为“建议优先、元数据可展开”。
- 基线记录（Phase 6 前）：旧的通用浅色 token 与后追加的深色样式存在冲突；Phase 7 已将样式表收敛为当前浅色 token，不再保留这组覆盖冲突。

现有较早的 `frontend/src/components/` 目录组件先不删除：它们可能属于用户已有工作区资产，本轮只通过页面和新 primitives 逐步替代；未被新页面引用的组件会在交付文档中标记为 legacy candidate，而不是未经确认直接删除。

## 6. 验收基线

- `npm run build` 必须通过。
- 可用的后端 Maven 测试必须通过；前端没有新增测试依赖。
- Demo 浏览器路径：进入 Workbench → 选择/创建工单 → 运行本地 Copilot → 查看证据/Trace → Human Review → 提交审核 → 查看结果。
- Real 浏览器路径：进入 Workbench → 创建真实合成工单 → 调用真实后端 run → 观察 `NO_RETRIEVAL_EVIDENCE` 等后端事实 → 复核 → 查看详情状态和 review history。
- 8 条截图路由（包含兼容别名）在 1920×1200、1366×900 和移动宽度下无横向溢出；关键按钮可见且有状态。
- 截图必须来自浏览器页面，而不是组件静态渲染或图片占位。

## 7. 2026-09-03 参考案例导向视觉纠偏（当前状态）

用户反馈上一版深色科技风不符合预期，因此本轮把视觉方向改为浅色中性客服工作台。当前实现的首屏关系是：

1. 队列优先：用搜索、状态和优先级快速定位待处理工单。
2. 上下文清楚：在中间列集中展示请求人、系统、分类、时间线和处理信息。
3. 动作靠近证据：右侧优先呈现处理建议、风险信号、关联知识、Citation 和人工复核动作。

本轮参考 Intercom Inbox、Zendesk Agent Workspace、Jira Service Management、ServiceNow CSM Workspace 和 Linear Search 的公开信息架构；借鉴的是队列、单工单上下文、优先级、活动流、知识关联和搜索密度，不复制第三方品牌或资产。`frontend/src/styles.css` 已移除渐变、发光、霓虹和装饰性科技背景，改用浅灰画布、白色面板、克制蓝色和低饱和状态色。

品牌层也完成了独立化：`BrandMark.vue` 使用票据轮廓与证据节点构成项目自有 mark，`NavIcon.vue` 使用统一笔画的 SVG 线性图标，字体采用本地优先的 `Aptos` / `Segoe UI Variable` 栈；没有把参考产品的 Logo、图片、字体文件或图标包带入运行时。

验证结果：前端生产构建通过；8 个目标页面的 1440×960、1920×1200 和 390×844 浏览器截图已重新生成；1366/390 横向溢出检查通过；Demo 浏览器 smoke 的 console/page errors 为 0；后端 `mvn test` 为 84/0/0/0。API、DTO、hash 路由、`data-e2e` 选择器、Demo/Real/Fallback 和后端工作流保持不变。
