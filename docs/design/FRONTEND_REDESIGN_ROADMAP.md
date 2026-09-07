# 企业智能工单前端改造规划

## 1. 文档目的

这份规划用于指导 Enterprise AI Ticket Copilot 从当前的 Showcase / 内部演示版，逐步收口为可长期使用的企业级支持工作台。

本规划覆盖前端视觉、信息架构、交互状态、AI 建议可信度、证据呈现、人工复核、响应式、无障碍、可维护性和浏览器验收；不把不存在的后端能力包装成已经完成的前端能力。

规划日期：2026-09-03

当前基线：PHASE_9G_FRONTEND_RELEASE_ACCEPTANCE（已完成）

## 2. 当前设计决策

### 2.1 目标方向

继续采用“以证据为核心的浅色企业支持工作台”（Evidence-first Support Workspace）：

- 队列优先：先帮助支持人员找到最需要处理的工单。
- 上下文集中：在一个工作区内理解请求人、系统、描述、错误日志和状态时间线。
- 建议靠近依据：AI / 规则建议必须同时呈现风险、置信度和引用证据。
- 人工保留决策权：批准、修改、驳回和升级要有清晰的状态与审计边界。
- 技术字段渐进披露：runId、traceId、fallback 等字段服务排查和审计，不压过一线用户的主任务。

### 2.2 不采用的方向

- 不回到暗色科技风、霓虹、发光和装饰性网格。
- 不用紫色渐变、玻璃拟态或重复的大圆角卡片制造“AI 感”。
- 不把 Dashboard 做成无业务动作的 KPI / 图表模板。
- 不直接复制 Intercom、Zendesk、Jira Service Management、ServiceNow、Linear 或其他产品的品牌资产、Logo、图片、字体、源码和页面。

## 3. 不可破坏的边界

- 保留现有后端 Java 业务逻辑、数据库 schema、API 路径、DTO、鉴权边界和 Provider 边界。
- 保留 Demo / Real / Fallback 的真实语义，不把 synthetic fixture、local-rule 或 fallback 写成真实模型成功。
- 保留既有 hash 路由兼容别名和 `data-e2e` 选择器。
- 保留用户当前工作区已有修改；不执行 reset、checkout、clean、强制覆盖或递归删除。
- 不新增生产依赖、真实密钥、外部账号、远程字体、第三方 Logo 或远程图片，除非用户另行明确批准。
- 每轮只完成一个名称明确、范围可验收的前端 TODO；不在同一轮顺手清理整个项目。
- 任何结论都必须区分“已实现”“已验证”“历史证据”和“待确认”。

## 4. 分阶段路线图

| 阶段 | 名称 | 目标 | 主要范围 | 状态 |
| --- | --- | --- | --- | --- |
| 9A | 交互与可访问性基础 | 让页面承诺的键盘交互真实可用，并消除关键对比度风险 | Command Palette、焦点管理、NavIcon 复用、语义状态、核心 color token | 已完成（2026-09-03） |
| 9B | Copilot 决策中心 | 让一线人员快速看懂“建议、依据、风险和下一步” | Workbench 右栏、建议主卡、证据摘要、动作层级 | 已完成（2026-09-03） |
| 9C | 字体与设计 token 收口 | 统一全站视觉节奏，减少小字号和页面级 token 漂移 | typography、muted/semantic colors、Evaluation 页面、inline style | 已完成（2026-09-03） |
| 9D | 移动端工作流 | 让 390px / 平板不只是不溢出，而是可以完成核心任务 | 队列、工单上下文、Copilot、复核动作、折叠和固定操作 | 已完成（2026-09-03） |
| 9E | 页面语言与渐进披露 | 让技术审计字段服务于用户，而不是主导用户 | 中文优先文案、Trace/Evidence 技术字段、Demo 边界表达 | 已完成（2026-09-03） |
| 9F | Legacy 组件和样式债务 | 在确认引用关系后减少重复实现 | legacy 组件盘点、共享组件迁移、CSS 收口 | 已完成（2026-09-03） |
| 9G | 前端发布验收 | 为进入后端改造前建立可重复的前端验收门槛 | build、截图、Demo smoke、Real smoke、a11y、响应式、文档 | 已完成（2026-09-03） |

## 5. 本轮 9A 实施定义

### 5.1 行为目标

- Ctrl / Cmd + K 打开命令中心。
- 打开后焦点进入搜索框。
- 输入查询后结果实时过滤。
- ↑ / ↓ 在结果中移动当前项。
- Enter 进入当前项。
- Escape 关闭命令中心。
- Tab 不穿透到背景页面。
- 关闭后焦点回到打开命令中心的控件。
- 命令中心的图标与侧栏复用同一套 `NavIcon`。

### 5.2 视觉目标

- 保留当前浅色中性工作台方向。
- 加强命令中心当前项、键盘焦点和 hover 的可见区分。
- 调整弱化文字和警告文字 token，降低小字号对比度风险。
- 不改变页面布局、后端接口、业务状态和 Demo / Real 逻辑。

### 5.3 9A 验收标准

- `frontend/npm run build` 通过。
- 命令中心鼠标、键盘和屏幕阅读器语义路径一致。
- 命令中心提示与实际能力一致。
- 主要文本 token 在白色面板上满足 WCAG AA 的目标对比度，或被明确限制为非文本装饰用途。
- 8 个截图目标仍然可生成。
- 1366×900 和 390×844 无横向溢出。
- Demo 主流程仍能完成队列筛选、Copilot、人工批准和页面导航。
- 后端目录和 `docs/metrics/` 不产生修改。

### 5.4 9A 实际完成与验证结果

- `frontend/src/App.vue` 已为命令中心补齐焦点进入、Escape 恢复、Tab / Shift+Tab 循环、↑ / ↓ 当前项、Enter 导航、`combobox` / `listbox` / `option` 语义和 `aria-activedescendant`。
- 命令中心结果图标已复用项目自有 `NavIcon.vue`，不再把图标字符串直接渲染为文本。
- 全局弱化文字、警告色和危险色 token 已调整为更适合白色面板的对比度；键盘焦点轮廓和当前项状态已明确可见。
- 实际验证：`frontend/npm run build` 通过；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标、标准 / 大屏 / 移动端截图和横向溢出检查；一次性浏览器 smoke 通过命令中心和 `DEMO-0002` Copilot → Approve → 已解决闭环，console/page error 均为 0。
- 对比度检查为静态 token 检查，不等同于完整 axe / Lighthouse 审计；前端当前也没有独立持续测试 runner。

## 6. 9B～9G 的执行与验收定义

### 9B Copilot 决策中心

将右侧 Copilot 组织为一个主决策模块：

1. 建议结论；
2. 建议回复；
3. 主要证据；
4. 风险和置信度；
5. 批准、修改、驳回、转人工或升级。

runId、traceId、fallback 和 validation 继续通过折叠审计区展示。

验收重点：用户能在 3 秒内找到建议，在 5 秒内理解依据和风险，在同一上下文内完成下一步决策。

### 9B 实际完成与验证结果

- `frontend/src/views/TicketWorkbenchShowcaseView.vue` 将右侧内容收口为“Copilot 决策建议 → 建议回复 → 关联证据 → 风险与复核门禁 → 人工复核 → 运行信息 / 审计字段”。
- 新增决策主卡，首屏显示推荐处理、分类置信度、运行入口和下一步提示；风险、Citation 数量和复核门禁独立成卡，`runId`、`traceId`、fallback 和 validation 继续留在折叠区。
- `frontend/src/styles.css` 增加决策主卡、风险摘要卡和手机端可读样式；未改变 API、状态流转、Demo / Real / Fallback 语义或既有 `data-e2e` 选择器。
- 真实浏览器 Demo smoke：选择 `DEMO-0004` → 运行本地 Copilot → 页面显示“建议结论 / 建议回复 / 关联证据 / 风险与复核门禁” → 确认 Approve；最终工单为“已解决”，`review history` 含 `APPROVED_RESOLUTION`。
- 响应式复核：390px 单列队列 → 详情 → 决策中心；768px、1024px 为两列内容加整行决策中心；1440px 为三列工作台。四个视口均无横向溢出、破图数量为 0，页面控制台 error/warning 为 0。
- `frontend/npm run typecheck`、`frontend/npm run build` 通过；`SCREENSHOT_URL=http://127.0.0.1:5190 npm run screenshots` 通过 8 个目标并生成标准、大屏、移动端截图。
- Accessibility scanner 作为静态候选审查执行，当前仍有组件级 landmark / label heuristic 候选；本轮未将其误写成完整 axe / Lighthouse 合规结果。
- 本轮未修改 backend、数据库、Provider、真实密钥、外部账号或部署配置；未执行 Git commit / push。

### 9C 字体与 token 收口

- 正常辅助正文以 12～13px 为下限；9～10px 仅用于极少量不可扫描的技术 ID。
- 统一全局颜色 token，评测页只使用语义映射，不再维护完整的第二套颜色体系。
- 将重复 inline margin、padding、按钮宽度和指标文本样式转为可复用 class 或组件变体。
- 保留评测页需要的技术指标密度，但提高解释性和中文上下文。

验收重点：视觉截图中辅助信息可读；状态色、边框和面板在所有页面一致；不改变评测数据口径。

### 9C 实际完成与验证结果

- `frontend/src/styles.css` 增加共享字号、间距和 focus ring token；普通 `tiny-note`、Copilot 辅助说明、证据摘录和复核卡上下文提高到可读的辅助正文层级，技术 ID 仍保留等宽小字号。
- `EvaluationMetricsShowcaseView.vue` 的 `--eval-*` 语义变量改为映射全局 `--app-*` token，不再维护一套独立颜色值；评测数据、指标名称和口径没有改变。
- 移除 Workbench、Trace Timeline、Knowledge Base 和 Human Review 中的静态 inline margin/padding/字体声明，改为 `flow-note--top`、`review-comment-input`、`trace-reference-list`、`history-list--flush`、`history-list--compact`、`knowledge-hit__score` 等语义 class；动态进度条宽度的 `:style` 仍保留，因为它来自真实运行时百分比。
- 验证：`frontend/npm run typecheck` 通过；`frontend/npm run build` 通过（63 modules）；`git diff --check` 无 diff 错误。当前尚未将静态候选扫描描述为完整 axe / Lighthouse 合规结论。
- 边界：本阶段只收口 token、字号和样式表达，没有修改后端、API、Demo / Real / Fallback 语义、评测数据或真实 Provider 配置。

### 9D 移动端工作流

- 队列、工单上下文、Copilot / Evidence 按任务优先级顺序展示。
- 复杂证据和审计字段可折叠。
- 复核动作保持可触达，但不遮挡正文。
- 对 390px、768px 和 1024px 分别进行真实浏览器操作验证。

验收重点：移动端不仅无横向溢出，还能完成选择工单、运行或查看建议、查看证据和提交复核的关键路径。

### 9D 实际完成与验证结果

- `TicketWorkbenchShowcaseView.vue` 增加移动端流程导航：队列、工单、建议与证据、复核四个入口在当前页面内滚动，不改变应用级 hash 路由；滚动行为尊重 `prefers-reduced-motion`。
- 390px 下将工单上下文、Copilot 决策、风险摘要和人工复核保持单列；复核按钮改为可触达的 44px 操作高度，Request changes / Reject 在窄屏改为纵向排列，避免两个危险动作挤在小按钮中。
- 桌面和窄桌面保留原有信息架构：768px / 1024px 为队列与上下文两列后接 Copilot，1440px 为队列、上下文、Copilot 三列；移动流程导航仅在 760px 以下出现。
- 实际浏览器 Demo smoke：390px 选择已有合成工单 → 运行本地 Copilot → 查看决策 / 证据 → Approve；工单状态更新为“已解决”，浏览器仍停留在 `#ticket-detail`，移动流程锚点不会误切换到 Dashboard。
- 响应式验证：390×844、768×844、1024×900、1440×900 均无横向溢出、破图为 0；390px 页面宽度为 375 CSS px，核心移动流程导航 4 个链接均存在，操作按钮最小高度 44px。
- 验证：`frontend/npm run typecheck`、`frontend/npm run build` 通过；本阶段没有改变 API、Demo / Real / Fallback 语义或后端。

### 9E 页面语言与渐进披露

- 产品动作统一中文主导，保留必要的 Provider、Trace、Citation、JSON、RBAC 等技术词。
- 用户层文案与排查层字段分开。
- Demo、Real、Fallback、只读和 synthetic 数据边界在页面上清楚但不喧宾夺主。

验收重点：非技术支持人员可以完成主流程；技术人员仍能展开查看审计信息。

### 9E 实际完成与验证结果

- Workbench、Human Review、Knowledge Base、Retrieval Evidence、Trace Timeline 和 Dashboard 的页面 aria label、空态、解释文案与主要动作统一为中文优先；英文只保留在用户需要识别的技术概念之后，例如“批准（Approve）”“检索快照（retrieval snapshot）”“复核历史（review history）”。
- Human Review 的动作改为“批准 · Approve / 要求修改 · Request changes / 驳回 · Reject”，并同步更新确认提示、必填原因提示和安全边界说明；没有改变 review API 的 decision 枚举。
- 运行元数据继续保留 `runId`、`traceId`、Provider、fallback、errorCategory 等原始字段，作为审计标识而不是主操作文案；Workbench 的低频审计字段继续使用 `details` 折叠。
- Knowledge / Retrieval 页面明确区分“检索命中”“检索参考”和“已校验引用”，同时将 `Citation membership validation` 解释为引用归属校验，不暗示完成事实蕴含验证。
- 边界：本阶段只改 UI 文案、可访问名称和渐进披露表达，没有改 API、状态码、评测数据、Demo fixture 或后端业务。
- 验证：`frontend/npm run typecheck`、`frontend/npm run build` 通过；后续 `PHASE_9G` 将统一重跑浏览器截图、状态闭环、溢出、控制台和静态候选审查。

### 9F Legacy 组件和样式债务

- 先生成引用清单，再逐个迁移。
- 未确认的旧组件不删除。
- 不在视觉改造阶段顺手做大规模目录重构。

验收重点：构建、截图和主要浏览器流程前后行为一致，且 diff 可以清楚归因到一个小任务。

### 9F 实际完成与验证结果

- 新增 `docs/design/LEGACY_COMPONENT_REFERENCE_MAP.md`，按当前源码引用结果区分壳层共享组件、UI 共享原语和 7 个未被当前 Showcase 路由引用的 Legacy 业务组件。
- 确认当前页面已经统一使用 `AppSidebar` / `AppTopbar` / `NavIcon` / `BrandMark` 以及 `PageHeader` / `PanelHeader` / `StatusBadge` / `EmptyState` / `ErrorState` / `LoadingState`；不再为当前页面引入旧的队列、详情、AI 推荐和 intake 组件。
- 未删除未引用的 Legacy 组件：它们仍可能被历史页面、外部演示入口或截图脚本依赖，且旧 props / emit / 数据 shape 与当前 `useTicketRealFlow()` 不兼容；删除条件已写入引用清单。
- CSS 债务处理延续 9C：静态 inline margin/padding/字体已迁移为语义 class；运行时百分比进度条保留；Evaluation 局部样式使用全局 token 映射。
- 验证：组件引用清单通过 `rg` 复核；`frontend/npm run typecheck`、`frontend/npm run build` 通过。没有改后端或扩大到目录级重构。

### 9G 前端发布验收

进入后端改造前，前端必须完成：

- 前端生产构建；
- 现有后端回归测试；
- Demo 浏览器 smoke；
- Real API 合成数据 smoke；
- 标准、大屏、平板和移动端截图；
- 横向溢出检查；
- console/page error 检查；
- 键盘和焦点检查；
- 对比度和基本无障碍检查；
- Demo / Real / Provider 边界复核；
- TODO、HANDOFF、设计和测试文档同步。

### 9G 实际完成与验证结果

- 前端构建：`frontend/npm run typecheck` 与 `frontend/npm run build` 均通过；Vite 完成 63 个模块转换。
- 截图：`$env:SCREENSHOT_URL='http://127.0.0.1:5190'; frontend/npm run screenshots` 通过，生成 `docs/images/`、`docs/images/large/`、`docs/images/mobile/` 三套 Showcase 截图；脚本覆盖 8 个页面目标，并执行 1366px / 390px 横向溢出检查。
- 后端回归：`backend/mvn test` 为 84/84 通过，0 failure/error/skip；本轮前端改造没有修改后端源码、数据库迁移或 Provider 配置。
- Demo 浏览器 smoke：在 390px 页面看到移动端四步导航，运行本地 Copilot 后显示 `DEMO_LOCAL`、`DEMO-RUN-*` 和本地 Trace；此前已验证 Approve 后进入“已解决”并产生 `APPROVED_RESOLUTION` review history。本轮截图在表单显式 label 修复后重新生成。
- Real API 语义 smoke：沿用已记录的隔离 H2 schema-only 后端 + Vite 真实模式证据，验证 `/api/health`、创建合成工单、`NO_RETRIEVAL_EVIDENCE` 安全拒答、人工复核和状态更新；没有把本地空知识库结果写成 Citation-positive 成功。
- 响应式：390、768、1024、1440 视口均复核；页面没有横向溢出，破图数量为 0。移动端队列 → 工单 → 建议与证据 → 复核导航通过 `@click.prevent` 定位，不会错误改变应用级 hash 路由。
- 交互与边界：命令中心键盘路径、焦点恢复、Demo / Real / Fallback 标签、loading/error/empty/action 状态和人工复核确认流程均保留；未调用真实 Provider、未新增 API Key、未部署公网。
- 可访问性候选：显式补充 Workbench 创建表单和 Human Review 备注的 `id/for` 后，静态扫描从 7 critical / 63 serious / 27 moderate 降至 0 critical / 63 serious / 25 moderate。剩余候选主要来自把独立 Vue 组件当完整页面扫描的 landmark / h1 / nav 启发式；当前真实 App 已有 skip link、`main` 和页面级 `h1`，项目没有宣称完整 axe/Lighthouse 合规。
- 结论：前端 9G 的本地验收门槛已达到，可以进入后端设计规划；后端目标、接口演进、权限、Provider 和 staging 方案另见 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md`。

只有以上内容实际完成并记录后，才进入后端改造规划。

## 7. 本轮与后端改造的关系

本轮前端改造不改变后端功能，也不提前实现后端待办。

前端已经达到 9G 本地验收门槛，已单独创建 `docs/design/BACKEND_NEXT_PHASE_DESIGN.md` 作为后端改造规划。后端实施仍需要重新确认 API、数据库、权限、Provider、知识库和生产数据边界，不从前端页面推断不存在的后端能力。

## 8. 记录规则

- 每个阶段完成后追加 `TODO.md` 的阶段记录。
- 每轮完成、阻塞、验证和遗留风险追加到 `HANDOFF.md` 顶部。
- 页面或交互变化时同步 `docs/DESIGN.md` 或对应设计文档。
- 测试和截图只记录真实执行结果。
- 本规划的“待开始”不等于已经完成，未验证内容必须保持未验证状态。
