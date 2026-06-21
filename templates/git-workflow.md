# 新手 Git 工作流

Git 是项目存档工具，不等于 GitHub。以下命令只针对当前项目；除非用户明确要求，不推送远程仓库。

## 1. 初始化当前项目

```powershell
cd "你的项目文件夹"
git init
git status
```

如果需要设置提交身份，优先只设置当前项目：

```powershell
git config user.name "你的名字"
git config user.email "你的邮箱"
```

不要把邮箱、Token 或密码写进项目文档。

## 2. 先创建 `.gitignore`

根据技术栈保留需要的部分。下面是常见起点：

```gitignore
# 密钥和本地配置
.env
.env.*
!.env.example

# JavaScript / Node.js
node_modules/
dist/
build/
.cache/

# Java / Kotlin
target/
.gradle/

# Python
__pycache__/
.venv/
*.pyc

# 日志和临时文件
*.log
tmp/
temp/

# 编辑器和系统文件
.idea/
.vscode/
.DS_Store
Thumbs.db
```

说明：`.vscode/` 是否忽略要看项目是否需要共享编辑器配置；不确定时先让 Agent 解释。

## 3. 每个小任务的固定顺序

### 开始前

```powershell
git status
```

确认当前已有改动是谁产生的。不要覆盖与本轮无关的用户修改。

### 修改完成后

先运行项目实际可用的测试、构建或手工验收，再检查差异：

```powershell
git status
git diff
```

重点检查：

- 是否只改了当前 TODO 的文件；
- 是否出现 `.env`、密钥或个人信息；
- 是否混入依赖、构建输出和缓存；
- 文档是否与实际实现一致。

### 暂存并复查

```powershell
git add "本轮确认过的文件"
git diff --cached
```

新手不必总用 `git add .`；明确列出文件更容易避免误提交。

### 提交

```powershell
git commit -m "feat: 完成一个已验证的小任务"
```

常见前缀：

- `feat:` 新功能；
- `fix:` 修复；
- `docs:` 只改文档；
- `test:` 增加或修改测试；
- `refactor:` 行为不变的代码整理。

## 4. 分阶段提交建议

- Phase 0：项目初始化和文档基线。
- Phase 1：一个基础页面或一个页面状态。
- Phase 2：一个核心功能的正常流程，再单独提交异常处理。
- Phase 3：一个数据模型或一个接口。
- Phase 4：一组相关测试或一次范围明确的优化。
- Phase 5：文档同步和部署准备。

不要把多个阶段压进一次提交。

## 5. 出问题时先停下

- 不要使用 `git reset --hard` 或强制覆盖命令。
- 先运行 `git status`，把输出交给 Codex解释。
- 如果改动未提交，先确认哪些是用户改动、哪些是 Agent 改动。
- 只有明确知道影响后，才撤销指定文件或指定片段。

## 6. 提交前最终检查

- [ ] 当前 TODO 的验收方式已实际执行。
- [ ] 测试结果记录在 `HANDOFF.md`。
- [ ] `TODO.md` 状态已更新。
- [ ] `git diff --cached` 只有本轮内容。
- [ ] 没有敏感信息、依赖目录、构建输出和缓存。
- [ ] 提交信息准确，没有把失败状态写成“完成”。

