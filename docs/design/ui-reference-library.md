# UI Reference Library

这份资料库只做一件事：给后续前端改造提供稳定的视觉参照，避免把 Enterprise Ticket RAG Copilot 做回普通后台。

使用原则：

1. 先看 `docs/design/references/` 的 image2 参考图。
2. 再看成熟开源 dashboard / SaaS / admin 资源。
3. 只借 layout、token、composition、密度，不复制工程和源码。
4. 每次真正实现前，先读这份资料库，再写页面 recipe。

## 1. image2 参考图

> 这些图是 AI-generated visual references，只用于前端视觉方向，不是项目真实截图。

| 文件 | 用途 | 主要借鉴 | 明确不借鉴 |
| --- | --- | --- | --- |
| `docs/design/references/01-dashboard-ai-concept-cn.png` | Dashboard 总览方向 | 深色企业控制台、KPI 密度、Provider 状态、近期活动、工作流摘要 | 营销式 hero、空白大页、花哨装饰 |
| `docs/design/references/02-ticket-workbench-ai-concept-cn.png` | Ticket Workbench 主参考 | 固定侧栏、单层 topbar、三栏工作台、队列 / 详情 / Copilot 结构 | 普通后台卡片堆叠、重复信息条、过多胶囊标签 |
| `docs/design/references/03-ticket-trace-ai-concept-cn.png` | Trace 页面参考 | workflow steps、step detail、Provider Call、Tool Call、日志、JSON 摘要 | 纯开发者调试台、无层级日志墙 |
| `docs/design/references/04-knowledge-base-ai-concept-cn.png` | Knowledge / RAG 参考 | 文档列表、Chunk、关键词命中、RAG 引用预览、关联工单 | 真向量库界面、数据库管理台 |
| `docs/design/references/05-human-review-ai-concept-cn.png` | Human Review 参考 | 待审核队列、AI 草稿、风险原因、知识引用、审核意见、审批操作 | 只放表单、不呈现证据链 |

### 01-dashboard-ai-concept-cn.png

- 借鉴什么：总览页的“控制台感”、一屏内可扫读的 KPI、状态卡和活动流。
- 不借鉴什么：大 banner、媒体区、营销文案、纯展示型排版。
- 如何转成 Vue + CSS：用 `grid` 做 2-4 列摘要卡，保留一个主工作区和一个轻量活动区，卡片只承载关键指标和边界状态。

### 02-ticket-workbench-ai-concept-cn.png

- 借鉴什么：左队列 / 中详情 / 右 Copilot 的三栏比例、顶部单层状态栏、密集但清晰的工单控制台。
- 不借鉴什么：普通后台的上下堆叠面板、重复 topbar、过度饱和的 badge 堆。
- 如何转成 Vue + CSS：把工作台拆成三个独立区块，左栏做队列筛选，中栏做工单上下文，右栏做 AI / RAG / Review 证据面板，全部用 CSS token 统一。

### 03-ticket-trace-ai-concept-cn.png

- 借鉴什么：步骤链、调用链、证据折叠、日志与 JSON 的层级关系。
- 不借鉴什么：开发者工具式满屏 JSON、未分层的 debug dump。
- 如何转成 Vue + CSS：先把 trace 变成“摘要 + 可折叠证据”，再按 step / provider / tool call 分段渲染。

### 04-knowledge-base-ai-concept-cn.png

- 借鉴什么：知识文档、chunk、命中词、引用预览、关联工单的知识工作台语义。
- 不借鉴什么：单纯文档库、白底表格、搜索引擎式结果列表。
- 如何转成 Vue + CSS：把知识页做成“资料列表 + 详情预览 + 关联关系”三段式，右侧保留引用和关联工单摘要。

### 05-human-review-ai-concept-cn.png

- 借鉴什么：审核待办、风险原因、草稿预览、人工决策按钮、证据来源。
- 不借鉴什么：只剩审批按钮的流程页、没有上下文的空白表单。
- 如何转成 Vue + CSS：让审核页保持“先看证据、再决策”的顺序，动作按钮始终放在可见区域。

## 2. 开源 UI 参考

### TailAdmin

- 名称：TailAdmin / free-react-tailwind-admin-dashboard
- URL：<https://github.com/TailAdmin/free-react-tailwind-admin-dashboard>
- 技术栈：React、TypeScript、Vite、Tailwind CSS
- License：MIT
- 值得借鉴的点：深色 admin shell、sidebar 层级、dashboard 卡片密度、状态卡片、数据驱动布局。
- 不适合直接搬的点：React 组件、Tailwind 工程结构、图表实现、模板页面代码。
- 如何转成当前 Vue + CSS 项目：把它当作“后台壳层节奏”参考，用 Vue SFC 重写布局骨架，只保留卡片边界、侧栏密度和状态区排版。

### Tailwind-Admin

- 名称：Tailwind-Admin / free-tailwind-admin-dashboard-template
- URL：<https://github.com/Tailwind-Admin/free-tailwind-admin-dashboard-template>
- 技术栈：Tailwind CSS 体系下的多框架模板族，覆盖 React、Next.js、Angular、Vue、TanStack Start
- License：MIT
- 值得借鉴的点：SaaS / CRM / data dashboard 的布局语言、模块化页面结构、深色模式、可扩展的页面族谱。
- 不适合直接搬的点：整套多框架工程、安装脚本、模板目录结构、具体组件源码。
- 如何转成当前 Vue + CSS 项目：借它的“页面组织法”，把当前前端拆成固定壳层、概览页、工作台页、证据页，而不是把所有东西塞进一个大单页。

### satnaing/shadcn-admin

- 名称：satnaing/shadcn-admin
- URL：<https://github.com/satnaing/shadcn-admin>
- 技术栈：Vite、React、TypeScript、shadcn/ui、Tailwind CSS
- License：MIT
- 值得借鉴的点：Vite dashboard shell、built-in sidebar、global command search、card hierarchy、轻量但完整的 admin 语法。
- 不适合直接搬的点：shadcn/ui 源码、React 组件实现、路由和状态管理实现。
- 如何转成当前 Vue + CSS 项目：用一个单层 topbar + 搜索 + 状态标签的结构替代多层状态区，卡片用更清晰的标题层级和更小的半径。

### Ticketfy

- 名称：Pymmdrza/ticketfy
- URL：<https://github.com/Pymmdrza/ticketfy>
- 技术栈：React、TypeScript、Tailwind CSS
- License：MIT
- 值得借鉴的点：support ticket 的业务语义、队列、状态、筛选、通知、管理后台信息密度。
- 不适合直接搬的点：实时通知实现、React 逻辑、具体状态管理代码。
- 如何转成当前 Vue + CSS 项目：优先借它的工单业务语义，把队列 row、状态 chip、通知入口、优先级和筛选层做得更像真实工单产品。

### shadcnspace

- 名称：shadcnspace / shadcnspace
- URL：<https://github.com/shadcnspace/shadcnspace>
- 技术栈：React、TypeScript、Next.js、Tailwind CSS、shadcn/ui、Base UI
- License：MIT
- 值得借鉴的点：dashboard blocks、card composition、系统级 UI foundation、均衡 spacing、可复用区块组织。
- 不适合直接搬的点：blocks 本体、复制式安装流程、组件仓库结构。
- 如何转成当前 Vue + CSS 项目：把 dashboard / trace / knowledge 页面拆成可复用的“block 思维”，先定块的职责，再定块之间的层级与留白。

## 3. 使用顺序建议

1. 先对照 `02-ticket-workbench-ai-concept-cn.png` 定主工作台结构。
2. 再用 TailAdmin 和 shadcn-admin 校准壳层与卡片节奏。
3. 再用 Ticketfy 收工单业务语义。
4. 最后用 shadcnspace 校正 block 组合和 dashboard composition。
