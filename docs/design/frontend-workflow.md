# Frontend Workflow

这份流程是给 Codex / Claude 共用的前端开发路径。目标不是“尽快写完”，而是“先像，再稳，再接真实数据”。

## 标准流程

### Step 1：只读审查当前页面

- 先读当前页面相关源码。
- 只看这一页的布局、状态和截图目标。
- 不要把所有页面一起重构。

### Step 2：读取设计资料库

必读顺序：

1. `docs/design/ui-reference-library.md`
2. `docs/design/ui-style-guide.md`
3. `docs/design/page-recipes.md`
4. `docs/design/screenshot-acceptance-checklist.md`

### Step 3：先输出页面规格

- 先写页面规格。
- 先定布局、状态、组件职责和截图目标。
- 不要一上来就写代码。

### Step 4：只改一个页面

- 每轮只改一个页面。
- 如有必要，先新建 `ShowcaseView`。
- 可以先用本地 demo 常量把高保真视觉做出来。
- 不要为了复用旧组件去继承旧后台风格。

### Step 5：运行 `npm run build`

- 先确认页面能编译。
- 如果 build 失败，先修构建问题。

### Step 6：运行 `npm run screenshots`

- 必须从真实浏览器生成截图。
- 截图通过后再看视觉。

### Step 7：人工验收截图

- 对照 `screenshot-acceptance-checklist.md` 人工检查。
- 不要凭代码推断截图效果。

### Step 8：合格后 commit

- 只在当前页面通过后提交。
- 一个 commit 只对应一个页面或一个清晰的设计闭环。

### Step 9：不合格则继续返工

- 继续在同一页面迭代。
- 不要进入下一个页面。
- 不要先把功能做全再回头修视觉。

## 额外约束

- 不要一次改多个页面。
- 不要让功能完整度压过视觉还原。
- 不要复用旧组件导致旧后台风格污染。
- 不要把 React / shadcn / Tailwind 的实现直接搬进 Vue 项目。

## 推荐落地方式

1. 先建 `TicketWorkbenchShowcaseView.vue`。
2. 先用本地 demo 常量完成高保真视觉。
3. 再逐步接真实 API。
4. 设计未过关前，不要急着把功能接齐。

