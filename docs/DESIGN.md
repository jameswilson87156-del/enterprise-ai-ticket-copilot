# 前端设计说明

> 当前版本：2026-09-05
>
> 最新精修规范见 [FRONTEND_CRAFT](design/FRONTEND_CRAFT.md)：工作台创建改为原生弹窗，完整证据与审核记录按需展开；桌面三栏独立滚动，移动端单列。下文保留既有业务与页面设计背景；涉及精修交互和视觉参数时以该补充规范为准。
>
> 本文是当前前端实现的设计契约。历史版本中的深色科技风描述不再作为当前页面的设计依据；历史审查和交接记录仍保留，便于追溯任务变化。

## 1. 产品体验方向

当前产品是面向支持人员的企业工单辅助处理工作台，不是科技感大屏，也不是营销落地页。视觉目标是安静、清楚、可信、可操作：用户打开页面后应该先看到待处理队列和工单上下文，再看到处理建议、引用证据和人工复核动作。

当前页面采用以下层次：

- 左侧：工作区导航和当前支持团队身份。
- 顶部：页面上下文、运行模式、Provider 边界和全局搜索。
- 主内容：根据页面呈现队列、工单、知识、证据、Trace、复核或评测信息。
- 动作侧栏：先给出 Copilot 决策建议和推荐处理，再给建议回复、关联证据、风险 / 复核门禁和下一步动作；runId、traceId、fallback 等低频字段折叠展示。

## 2. 公开案例带来的信息架构参考

本项目只参考公开产品的工作流语义和信息组织，不复制任何第三方品牌资产、页面代码、Logo、图片或字体。

| 参考案例 | 采用的结构启发 | 在本项目中的落点 |
| --- | --- | --- |
| [Intercom Inbox](https://www.intercom.com/helpdesk/inbox) | 团队队列、会话上下文、右侧客户 / 工单信息和快捷操作 | Workbench 的队列、上下文列和处理建议列 |
| [Zendesk Agent Workspace](https://support.zendesk.com/hc/en-us/articles/4408821259930-About-the-Zendesk-Agent-Workspace) | 单工单工作区、时间线、右侧上下文和知识辅助 | 工单详情、处理时间线、证据和复核区 |
| [Jira Service Management](https://www.atlassian.com/software/helpdesk-software) | 优先级队列、SLA / 状态、知识关联和支持流程 | 队列筛选、优先级 badge、知识关联和 Dashboard |
| [ServiceNow CSM Workspace](https://www.servicenow.com/docs/r/xanadu/customer-service-management/csm-workspaces-configure.html) | 活动流、上下文工作区和业务处理动作 | Trace / review history、活动信息和动作区 |
| [Linear Search](https://linear.app/docs/search) | 紧凑信息密度、搜索优先和快捷入口 | 顶部搜索、Ctrl/Cmd+K 页面导航、队列筛选 |
| [Atlassian Design Foundations](https://atlassian.design/foundations) | token、spacing、typography、iconography、elevation、border 和 radius 的系统化治理 | 项目 CSS token、共享组件和页面层级 |
| [Primer Color Usage](https://primer.style/product/getting-started/foundations/color-usage/) | 轻量 / 深色模式的语义色、可读性和功能色边界 | 低饱和状态色、浅色模式和可访问性约束 |

## 3. 视觉规则

- 画布使用温润的 `#f7f7f4`，侧栏使用 `#f0f1ee`，内容面板使用白色。
- 主操作使用克制的靛蓝 `#315bb2`；证据、成功、待处理、风险和复核使用低饱和语义色，品牌 mark 使用少量赤陶色 `#bd7158` 做识别点。
- 边框和间距负责建立层次；不使用霓虹发光、粒子、网格背景或大面积渐变。
- 标题使用 `Aptos Display` / `Segoe UI Variable Display` 优先的本地字体栈，正文使用 `Aptos` / `Segoe UI Variable` 和中文系统回退；工单编号、run ID、延迟和 JSON 字段使用等宽字体。
- 常规面板圆角保持在 10–14px；按钮、输入框和 badge 使用统一高度与边框语义。
- 选中导航和队列项使用浅蓝背景与蓝色标记；不能只靠颜色表达状态，必须同时有文字或图标。
- 品牌标识是自绘的票据轮廓与证据节点；页面图标是同一笔画宽度的自绘 SVG 线性图标，避免使用 Unicode 符号或第三方图标包造成视觉噪声。

## 4. 页面结构

| 页面 | 首屏重点 | 关键状态 / 动作 |
| --- | --- | --- |
| 总览 | 队列健康、待处理量、知识关联和最近活动 | 刷新、进入工作台、状态检查 |
| 工单工作台 | 工单队列 → 工单上下文 → 决策建议 → 建议回复 → 关联证据 → 风险门禁 → 人工复核 | 搜索、状态 / 优先级筛选、运行 Copilot、Approve / Request changes / Reject |
| 知识库 | 当前工单关联知识、来源和命中片段 | 只读查看、跳转证据 |
| 检索证据 | Retrieval Reference 与 Validated Model Citation 分层 | 查看 score、Citation 校验状态和证据边界 |
| 运行记录 | Run Overview、步骤时间线、Trace detail 和原始信息 | 查看运行步骤、fallback 和复核门禁 |
| 人工复核 | 风险原因、建议回复、引用和审核动作 | Approve、Request changes、Reject、查看历史 |
| 评测指标 | 本地 synthetic dataset 的指标事实和失败样本 | 查看 baseline、评测范围和指标解释 |

## 5. 状态与数据边界

- `Demo`：浏览器内存中的合成工单和本地 fixture；界面明确标记为本地演示，不宣称后端持久化。
- `Real`：只通过现有 `/api` 与后端交互，事实以 API 返回为准；网络失败显示安全错误，不静默换成 Demo 数据。
- `local-rule fallback`：当前默认安全演示路径，表示规则引擎辅助分类和模板化建议，不等于真实模型调用。
- `Citation`：检索引用和已校验模型引用分层展示，不把命中结果包装成事实蕴含验证。
- `Human Review`：高风险、低证据或建议草稿必须保留人工决策入口。
- 空、加载、错误和成功状态必须分别表达；操作按钮在请求中显示 loading / disabled，在完成后显示成功或安全错误反馈。

## 6. 响应式与可访问性

- 桌面端保留队列、上下文、处理建议的三栏关系；窄屏按“队列 → 内容 → 证据 / 动作”顺序单列展示。
- `390x844` 截图不得出现横向滚动；长 ID 可换行或通过 `title` 查看完整值。
- 交互控件使用语义按钮 / 链接、清晰的 `:focus-visible`、`aria-label`、`aria-live` 和 `aria-busy`。
- 顶部命令中心支持 Ctrl / Cmd + K 打开、输入过滤、↑ / ↓ 选择、Enter 导航、Escape 关闭并恢复触发控件焦点；弹层内 Tab / Shift+Tab 不穿透到背景页面，并使用 `combobox` / `listbox` / `option` 语义。
- 遵守 `prefers-reduced-motion`；动效只辅助加载或状态变化，不承担信息传达。
- 不以缩小字体或隐藏关键动作来解决桌面三栏在移动端的空间冲突。

## 7. 浏览器验收

截图由 `frontend/scripts/capture-screenshots.mjs` 通过真实浏览器生成：

- `docs/images/`：1440×960 标准截图。
- `docs/images/large/`：1920×1200 大屏截图。
- `docs/images/mobile/`：390×844 移动截图。
- 额外检查 1366×900 和 390×844 的 `document.scrollWidth`，确保没有横向溢出。

本说明对应的实现文件主要是 `frontend/src/styles.css`、`frontend/src/App.vue`、`frontend/src/components/layout/`、`frontend/src/components/ui/` 和 `frontend/src/views/`。本轮视觉修订没有改变后端 API、数据库、Provider 配置或工单状态流转。
