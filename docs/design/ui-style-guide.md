# UI Style Guide

Enterprise Ticket RAG Copilot 的前端视觉目标是：深色、克制、可扫读、像成熟企业 AI SaaS / 开发者工具 / RAG Copilot 控制台，而不是普通后台。

## 1. 整体风格

- 深色企业控制台。
- 重点是“工作台感”和“证据链感”。
- 页面优先显示操作区和信息层级，不做 landing page。
- 视觉重心是三栏工作台、证据面板和状态摘要。

## 2. Design Tokens

### 颜色

| Token | Value | 用途 |
| --- | --- | --- |
| `--canvas` | `#07101d` | 页面主背景 |
| `--canvas-deep` | `#040912` | 更深一层背景 / sidebar 底色 |
| `--sidebar` | `#08111f` | 侧栏底色 |
| `--panel` | `#0c1726` | 标准卡片底色 |
| `--panel-strong` | `#101f33` | 更强调的卡片 / 内容区 |
| `--border` | `rgba(151, 180, 214, 0.16)` | 常规边框 |
| `--border-strong` | `rgba(91, 141, 239, 0.22)` | 聚焦 / active / 高亮边框 |
| `--text-primary` | `#eef5ff` | 主标题 / 正文 |
| `--text-secondary` | `#c7d5ea` | 次级正文 |
| `--text-muted` | `#7e91ab` | 注释 / 辅助信息 |

### 状态色

| State | Value | 用途 |
| --- | --- | --- |
| Blue | `#3d7cff` | 主操作、选中、高亮 |
| Cyan | `#21c7d9` | 信息、追踪、链接、边界提示 |
| Green | `#2bd88f` | 成功、已接入、通过、已确认 |
| Orange | `#ffb45c` | 待处理、注意、风险提示中的中间态 |
| Red | `#ff5c7a` | 失败、拒绝、危险、阻断 |
| Purple | `#8b7cf6` | 知识、RAG、参考、相关性 |

### 圆角

- 默认卡片：`8px`
- 辅助面板：`10px`
- 小型 chip / badge：`6px` 到 `8px`
- pill 仅用于状态，不用于大段文字
- 不要用过大的圆角把控制台做成软糖风

### 阴影与 glow

- 标准卡片阴影：`0 18px 42px rgba(0, 0, 0, 0.26)`
- 强调面板阴影：`0 22px 54px rgba(0, 0, 0, 0.36)`
- 选中高亮：`0 0 0 1px rgba(61, 124, 255, 0.36), 0 0 24px rgba(61, 124, 255, 0.22)`
- glow 只用于 active、selected、connected、high-risk，不用于所有元素

### Typography

- Body：`Aptos, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif`
- Monospace：`Cascadia Code, SFMono-Regular, Consolas, monospace`
- 数字、ID、时间戳、置信度、日志优先用等宽字体
- 不做 viewport-scale 字体
- 字距保持 `0`

## 3. 组件规范

### Sidebar

- 宽度建议：`210px` 到 `238px`
- 结构：logo -> nav -> 角色/边界状态
- 导航项高度：`38px` 到 `42px`
- 当前项必须明显高亮，最好有左侧高亮条或发光背景
- sidebar 里只保留必要边界信息，不堆重复状态

### Topbar

- 只保留一层 topbar
- topbar 内最多放：搜索、环境标签、Provider/角色状态、少量图标按钮、用户信息
- 高度建议：`56px` 到 `64px`
- 不要在页面内部再复制第二套 topbar 或状态条

### Card / Panel

- 使用深色面板底色，边框轻、层级清晰
- 标题区与内容区要分层，但不要切成太多小盒子
- 卡片内信息优先纵向分组，避免大段横向塞满
- 重要内容使用更强的面板，次要信息用弱面板

### Tag / Badge

- 用于短语义：状态、优先级、来源、分类、知识引用
- 每行不要堆太多 tag
- tag 不能承担长句说明
- status chip 要能一眼读出流程位置

### Button

- 主要操作优先用 icon+text
- 次要操作可用 ghost / outline
- destructive 只给明确危险操作
- button 高度建议 `32px` 到 `38px`
- 不要把主要操作做成“胶囊文字按钮”堆满一排

### Table / List

- 列表要密，但每行只保留一层核心信息
- ID、时间、来源、置信度使用等宽或半等宽表达
- hover 要明确，selected 要比 hover 更强
- 白底表格禁止出现

## 4. 页面信息密度

- 第一屏必须可用，不能先放解释文案再找主要内容。
- 重点页面应在首屏同时看到工作对象、上下文和操作。
- 多栏页面宁可信息更密，也不要拉成大空白。
- 每个页面只允许一个主信息结构，不要多个主视觉并列抢戏。

## 5. 禁止项

- 普通后台风。
- 白底表格。
- 大面积空白。
- 胶囊标签堆叠。
- 重复 topbar。
- 营销式 landing page。
- 过度紫色渐变。
- 把 demo/mock 数据写成真实生产数据。
- 把参考图当成真实截图。

