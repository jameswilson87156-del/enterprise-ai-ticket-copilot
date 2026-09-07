# 前端组件引用与 Legacy 盘点

盘点日期：2026-09-03  
范围：`frontend/src/components`、`frontend/src/views`、`frontend/src/styles.css`

## 结论

当前运行入口已经统一使用 `components/layout/*` 的壳层组件和 `components/ui/*` 的共享页面原语。根目录下 7 个历史业务组件在当前 Showcase 路由没有被引用；它们保留在仓库中作为历史兼容资产，不在本轮删除或强行迁移。删除这些组件会扩大回归范围，而它们的旧 props / emit / class 契约与当前 `useTicketRealFlow()` 页面结构并不相同。

## 引用清单

| 组件 | 当前引用 | 归类 | 处理决定 |
| --- | --- | --- | --- |
| `components/layout/AppSidebar.vue` | `App.vue` | 当前壳层 | 保留；导航、运行模式和待复核数量统一入口 |
| `components/layout/AppTopbar.vue` | `App.vue` | 当前壳层 | 保留；搜索入口、路由上下文和当前用户统一入口 |
| `components/layout/BrandMark.vue` | `AppSidebar.vue` | 当前壳层 | 保留；自有品牌标记 |
| `components/layout/NavIcon.vue` | `App.vue`、`AppSidebar.vue` | 当前共享 | 保留；导航图标的唯一实现 |
| `components/ui/PageHeader.vue` | 6 个 Showcase 页面 | 当前共享 | 保留；页面标题、边界说明和动作槽统一实现 |
| `components/ui/PanelHeader.vue` | 6 个 Showcase 页面 | 当前共享 | 保留；工作区 panel 标题、说明和计数统一实现 |
| `components/ui/StatusBadge.vue` | Dashboard、Workbench、Knowledge、Retrieval、Trace、Review | 当前共享 | 保留；状态颜色和文字语义统一实现 |
| `components/ui/EmptyState.vue` | 6 个 Showcase 页面 | 当前共享 | 保留；空状态不补造 fixture |
| `components/ui/ErrorState.vue` | 6 个 Showcase 页面 | 当前共享 | 保留；安全错误统一出口 |
| `components/ui/LoadingState.vue` | 6 个 Showcase 页面 | 当前共享 | 保留；资源级 Loading 统一出口 |
| `components/ui/MetricCard.vue` | Dashboard | 当前共享 | 保留；只展示真实/评测来源指标 |
| `components/AiRecommendationPanel.vue` | 当前路由无引用 | Legacy | 保留；旧的 status/draft/confirmDraft emit 与当前 review API 不同 |
| `components/MetricStrip.vue` | 当前路由无引用 | Legacy | 保留；旧指标 delta 契约与当前事实型指标不同 |
| `components/SearchFilterBar.vue` | 当前路由无引用 | Legacy | 保留；旧筛选枚举不足以覆盖当前状态过滤 |
| `components/StatusTimeline.vue` | 当前路由无引用 | Legacy | 保留；旧 TimelineEvent shape 与当前 TicketDetail timeline 不同 |
| `components/TicketDetailPanel.vue` | 当前路由无引用 | Legacy | 保留；当前 Workbench 已把详情、证据和决策放入同一工作流 |
| `components/TicketIntakePanel.vue` | 当前路由无引用 | Legacy | 保留；当前 Workbench 使用 `CreateTicketRequest` 和真实/Demo flow |
| `components/TicketQueue.vue` | 当前路由无引用 | Legacy | 保留；当前 Workbench 队列增加状态/优先级过滤和共享选择状态 |

## 已确认的迁移与未迁移项

- 已确认并完成的共享迁移：所有当前 Showcase 页面使用 `PageHeader`、`PanelHeader`、`StatusBadge`、`EmptyState`、`ErrorState`、`LoadingState`；导航统一使用 `AppSidebar`、`AppTopbar`、`NavIcon` 和 `BrandMark`。
- 当前不迁移 7 个 Legacy 业务组件：它们不是当前页面的重复 import，而是旧页面架构的完整组件；逐个替换会同时改变 props、事件、数据源和 CSS 语义，不属于安全的样式债务收口。
- `AiRecommendationPanel.vue`、`TicketQueue.vue` 内的动态进度条 `:style` 是运行时数据表达，不应被静态 inline-style 规则误删。
- `EvaluationMetricsShowcaseView.vue` 保留局部 scoped CSS，但颜色已映射到全局 `--app-*` token；它不是第二套产品主题。

## 复核命令

在 `frontend/` 目录执行：

```powershell
rg --files src/components
rg -n "from '../components|from './components|<PageHeader|<PanelHeader|<StatusBadge" src
npm run typecheck
npm run build
```

本轮结果：组件引用关系已核对；静态 inline margin/padding/字体已迁移为语义 class；动态百分比宽度仍保留；类型检查和生产构建通过。

## 后续删除条件

只有在确认没有外部页面、历史演示入口或面试截图脚本依赖这些 Legacy 组件，并为替代实现补齐行为测试后，才单独创建删除任务。本盘点不把“当前无引用”当作“可以直接删除”。
