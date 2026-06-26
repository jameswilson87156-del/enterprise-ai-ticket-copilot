# 前端风格规范

## 设计方向

当前前端采用“深色企业 RAG Copilot 工作台”的视觉语言。页面第一屏就是可操作的工单处理界面：左侧导航、顶部运行边界、指标概览、工单队列、工单详情、Copilot / RAG / Human Review 面板同时可见。支持人员能快速扫读工单、查看规则/模板建议草稿、核验知识库命中，并明确知道所有处理动作都需要人工确认。

关键词：

- 深色企业 SaaS 工作台背景。
- 左侧固定导航 + 顶部搜索和环境状态栏。
- 三栏主体布局：工单队列、工单详情、Copilot / RAG / Human Review。
- 信息密度更高，但用分隔线、状态色和卡片内层级保持可扫读。
- 蓝、青、绿色用于运行状态和可解释规则命中，琥珀和红色只用于风险与人工复核。
- 日志、时间、置信度等数字信息使用等宽字体，强调审计和追踪感。

## Design Tokens

```text
Canvas: #07101d
Deep Canvas: #040912
Panel: #0c1726
Strong Panel: #101f33
Primary Text: #eef5ff
Secondary Text: #c7d5ea
Muted Text: #7e91ab
Border: rgba(151, 180, 214, 0.16)
Accent Blue: #3d7cff
Accent Cyan: #21c7d9
Success State: #2bd88f
Warm State: #ffb45c
Risk State: #ff5c7a
Knowledge State: #8b7cf6
Radius: 8px / 10px / 14px / 18px
Shadow: 0 24px 60px rgba(0, 0, 0, 0.28)
```

避免：

- 把界面包装成真实生产级 SaaS。
- 写“已接入真实大模型”“向量数据库已实现”“自动关闭工单”等未实现能力。
- 把 docs/design/references 的 AI 参考图当成 README 真实截图。
- 使用一整页监控大屏式装饰而弱化工单处理流程。
- 把 demo/mock 数据描述成真实企业生产数据。

## 组件规范

- 顶部栏需要清楚说明 Portfolio Demo、Provider 为 local-rule fallback、No real LLM、No vector DB。
- 搜索与筛选栏使用大圆角输入和状态筛选，不做复杂表单。
- 指标卡片只做轻量状态摘要，不做大屏化数字面板。
- 工单队列使用深色列表卡片，强调标题、请求人、来源系统、时间、状态、优先级和规则置信度。
- 当前工单详情按问题描述、系统上下文、日志、业务上下文和处理记录分层。
- Copilot 面板必须展示规则引擎辅助分类、优先级判断、关键词知识匹配、模板化建议草稿、风险提示与 Human Review 操作区。
- Knowledge Base / RAG 区域必须说明当前基于关键词匹配与知识引用，向量检索是后续扩展。
- Trace 区域必须展示 `ticketId`、派生 `runId/traceId`、当前步骤、状态历史和 `generation_record` 摘要，并说明当前不是完整 Workflow Step Trace / Span Runtime。
- RAG Reference 区域必须展示知识标题、`sourcePath`、命中关键词、相关度、片段、是否用于草稿和关联工单/运行标识；没有真实命中时保持空状态。
- Human Review 区域必须展示人工审核状态、reviewer、decision、comment、reviewedAt 与 nextAction；当前数据由状态历史推导，不能包装成完整审核中心。

## 交互原则

- 第一屏就是可用工作台，不放营销式 landing page。
- 队列点击即可切换当前工单。
- 搜索和筛选只影响队列显示，不改变数据状态。
- 状态流转和知识入库必须通过人工按钮确认。
- 页面支持桌面与移动宽度，移动端左导航、顶部栏和所有多列布局折叠为单列。
- 键盘焦点必须清晰可见，动态成功/错误状态使用 live region 通知辅助技术。
- 搜索、筛选、工单选择和人工确认都必须具有真实交互结果，不做静态展示控件。
