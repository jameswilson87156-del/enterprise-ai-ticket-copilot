# Enterprise Ticket RAG Copilot UI Visual References

本目录存放 Enterprise Ticket RAG Copilot 的 AI-generated visual references。

这些图片由 AI 图像生成工具生成，仅用于前端视觉方向、信息层级、页面布局和设计系统参考。

这些图片不是项目真实运行截图，不代表当前系统已经完整实现所有画面中的能力。

## 使用边界

* 这些图片不能作为 README 主项目截图。
* 这些图片不能作为真实运行证据。
* README 中的项目截图必须来自 Playwright 对真实运行前端页面的截图。
* 真实项目截图统一存放在 docs/images/。
* 本目录图片只能用于设计参考和前端实现方向讨论。
* 后续真实前端实现必须以 backend/frontend 的实际接口和真实能力为准。

## 当前真实能力边界

当前项目已有：

* 工单创建、列表、详情与状态流转
* 规则引擎辅助分类
* 关键词知识库匹配
* 模板化建议草稿
* 人工确认动作
* 状态历史与生成记录
* OpenAPI 文档
* H2 集成测试
* GitHub Actions CI
* 真实浏览器截图

当前项目尚未完整实现：

* 真实 LLM Provider 调用
* 生产级 JWT / RBAC 权限系统
* 向量数据库或 embedding 检索
* 完整 RAG Pipeline
* 完整 Tool Call Runtime
* 完整 Trace / Span 数据模型
* 完整 Human Review 审核任务系统
* 生产级部署与监控

## 图片清单

| 文件                                    | 页面               | 用途                                                                           |
| ------------------------------------- | ---------------- | ---------------------------------------------------------------------------- |
| 01-dashboard-ai-concept-cn.png        | 企业工单 Copilot 仪表盘 | 用于参考 Dashboard 总览、KPI、Provider 状态、Agentic Workflow 视觉方向                      |
| 02-ticket-workbench-ai-concept-cn.png | 工单处理工作台          | 用于参考三栏工单处理工作台、AI Copilot 面板、RAG 引用、Human Review 操作区                          |
| 03-ticket-trace-ai-concept-cn.png     | 工单处理 Trace       | 用于参考 Trace、Workflow Step、Provider Call、Tool Call、JSON 输入输出和日志展示              |
| 04-knowledge-base-ai-concept-cn.png   | 知识库 / RAG 引用     | 用于参考知识文档、Chunk、关键词命中、RAG 引用预览和关联 Trace                                       |
| 05-human-review-ai-concept-cn.png     | 人工审核中心           | 用于参考 Human-in-the-loop 审核队列、风险原因、审核意见和 Approve / Request Changes / Reject 操作 |

## 后续实现原则

后续真实前端实现时，应优先参考：

1. 02-ticket-workbench-ai-concept-cn.png 的三栏工作台结构。
2. 03-ticket-trace-ai-concept-cn.png 的 Trace 信息层级。
3. 04-knowledge-base-ai-concept-cn.png 的知识引用证据链。
4. 05-human-review-ai-concept-cn.png 的人工审核边界。
5. 01-dashboard-ai-concept-cn.png 的整体深色企业 AI SaaS 视觉语言。

后续真实实现时必须保持诚实边界：

* 如果没有向量数据库，不写 Vector Search 已实现。
* 如果没有真实 LLM Provider，不写真实大模型已接入。
* 如果只是 local-rule fallback，不包装成生产级 AI Agent。
* 如果只是 demo 级 JWT / RBAC，不包装成生产级权限系统。
* 如果只是 Human-in-the-loop MVP，不包装成无人值守自动处理。
