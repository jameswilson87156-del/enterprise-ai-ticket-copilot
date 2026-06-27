# Page Recipes

这份文档只负责“每类页面该怎么长”，不负责具体实现。后续每次改前端，先对照这里，再动代码。

## 1. Ticket Workbench

参考图：`docs/design/references/02-ticket-workbench-ai-concept-cn.png`

### 布局

- 固定左侧导航 + 单层 topbar + 三栏工作台。
- 左栏：工单队列、搜索、筛选、状态、优先级、选中高亮。
- 中栏：工单详情、元数据、描述、状态时间线、处理按钮。
- 右栏：AI Copilot / RAG / Human Review / Trace 入口。

### 必须出现

- 工单队列。
- 当前工单标题与摘要。
- Priority / status / source / requester / assignee。
- 处理中的状态时间线。
- 规则分类建议。
- 优先级判断。
- 相似工单。
- RAG 引用。
- 回复草稿。
- 风险提示。
- Human Review 操作。

### 不要出现

- 重复顶部状态条。
- 过多 tab 抢首屏。
- 隐藏主要操作按钮。
- 只剩“漂亮卡片”但没有工单语义。

## 2. Dashboard

参考图：`docs/design/references/01-dashboard-ai-concept-cn.png`

### 布局

- KPI 摘要。
- Provider 状态。
- Workflow 摘要。
- Recent Tickets。
- Activity Timeline。

### 必须出现

- 今日工单或待处理量。
- 人工复核压力。
- Provider / fallback 状态。
- 最近工单活动。
- 近期审计或状态变化。

### 不要出现

- 大型 hero。
- 只展示概念卡，不展示真实工作数据。
- 过深的详情区。

## 3. Trace

参考图：`docs/design/references/03-ticket-trace-ai-concept-cn.png`

### 布局

- workflow steps。
- step detail。
- 结构化 JSON 摘要。
- Provider Call。
- Tool Call。
- 日志。

### 必须出现

- ticketId / runId / traceId。
- 当前步骤。
- 生成记录摘要。
- Provider / model / fallback。
- 可折叠 JSON。

### 不要出现

- 开发者工具式 dump。
- 无层级长日志墙。
- 只剩调试信息，没有工单上下文。

## 4. Knowledge / RAG

参考图：`docs/design/references/04-knowledge-base-ai-concept-cn.png`

### 布局

- 文档列表。
- Chunk / 段落。
- 关键词命中。
- RAG 引用预览。
- 关联工单。

### 必须出现

- 知识标题。
- sourcePath。
- 命中关键词。
- 相关度或权重。
- 片段预览。
- 关联工单 / 来源工单。

### 不要出现

- 真向量库界面。
- 纯搜索引擎结果页。
- 复杂知识管理后台。

## 5. Human Review

参考图：`docs/design/references/05-human-review-ai-concept-cn.png`

### 布局

- 待审核队列。
- AI 草稿。
- 风险原因。
- 知识引用。
- 审核意见。
- Approve / Request Changes / Reject。

### 必须出现

- 当前工单证据。
- review comment。
- next action。
- reviewer / decision / reviewedAt。
- 审核按钮始终可见。

### 不要出现

- 没有证据链的审批表单。
- 冗长说明压过决策区。

## 6. Provider

### 布局

- 只展示 local-rule fallback 和 OpenAI-compatible 扩展边界。
- 展示 providerName / modelName / fallbackReason / 环境变量名称。

### 必须出现

- 当前是否 local-rule fallback。
- 是否存在真实 Provider 验证记录。
- 验证文档入口。

### 不要出现

- 暗示真实 Provider 已经生产验证，除非有真实记录。
- API Key。
- 不必要的配置面板堆叠。

## 7. 使用规则

1. 每次只设计一个页面。
2. 先写 page recipe，再写实现。
3. 功能完整度不能压过视觉还原。
4. 如果旧组件带着旧后台风格，就不要硬修，改用新的 showcase 页面。
