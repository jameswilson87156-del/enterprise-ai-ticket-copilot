# Enterprise AI Ticket Copilot：第一阶段 staging 审计与部署计划

审计日期：2026-09-05（Asia/Shanghai）。本报告只适用于 `D:/workhome/enterprise-ai-ticket-copilot`。

**当前总状态：BLOCKED。** 本轮发现的部署资产问题已完成本地整改并复验；最新后端测试、应用生产构建、配置守卫、空库初始化和 Compose 静态检查为 LOCAL_PASS。云端资源、真实 Secret、OIDC、RDS 连通性、镜像构建与公网验收仍为 STAGING_PENDING 或 BLOCKED，不能据此宣称已部署。

## 1. 当前工作区

- 已读取总入口、本项目专用部署文档、AGENTS、README、HANDOFF、TODO、DESIGN、architecture、REAL_AUTH_PROVIDER_DEPLOYMENT、staging 与 local 验证脚本及脱敏历史证据。AGENTS 所列 `docs/PRD.md` 实际缺失；不编造，也不因此中止已授权的审计。
- 分支 `main`，HEAD `bc80e13e70c44d7530c96a932d68480bf185b15d`。
- 审计开始时 58 个 tracked 文件有修改，44 个 untracked 状态条目（包括目录）。OIDC、安全守卫、部署资产、前端认证等尚有未跟踪内容；HEAD 不等于本轮构建源码。
- `git diff --check` 为 LOCAL_PASS。保留用户修改；本轮不提交、不推送，仅对 staging/production 部署边界、配置守卫、schema-only 迁移基线和相关文档做了可追溯的本地整改。
- Java 17.0.19、Maven 3.9.11、Node 24.16.0、Docker Compose 5.3.1；容器/CI Node 20 一致性未验证。

## 2. 本轮实际验证

证据目录 `D:/workhome/deployment-audit-20260905-1800/`，本项目日志前缀 `ticket-`。

| 验证 | 实际命令 | 结果与范围 |
| --- | --- | --- |
| 后端全量测试和可执行 JAR | backend：`mvn -B test package` | LOCAL_PASS：289 tests，0 failures/errors/skipped，repackage 完成；数据库使用 test-profile H2，不代表 MySQL/RDS 验收 |
| 前端类型检查及生产构建 | frontend：`npm.cmd run build` | LOCAL_PASS：脚本包含 `vue-tsc -b --pretty false`；Vite 68 modules |
| 前端生产依赖 | frontend：`npm.cmd audit --omit=dev --audit-level=high --json` | LOCAL_PASS：total=0；不覆盖 Java 和容器镜像 |
| Compose 普通配置 | 根目录：`docker compose --env-file deploy/staging/.env.example -f deploy/staging/docker-compose.yml config -q` | LOCAL_PASS，退出码0，仅静态插值/语法 |
| Compose TLS 配置 | 上述命令加 `--profile tls`（config 前） | LOCAL_PASS，退出码0，未启动 Caddy/ACME |
| 环境守卫负向测试 | `scripts/staging/validate-env.ps1 -EnvironmentFile deploy/staging/.env.example` | LOCAL_PASS：按预期拒绝 TICKET_HOST 占位符 |
| 独立 staging 配置守卫正向验证 | 同一脚本读取审计目录内仅含假值的 ticket fixture | LOCAL_PASS：外部 MySQL/TLS、OIDC、Provider、fallback=false 和跨项目变量隔离规则通过；不代表远端可达 |
| 空库 schema-only MySQL 验证 | 隔离 `mysql:8.4`、无网络、无发布端口的临时容器 | LOCAL_PASS：10 个业务表为空，首次初始化成功，重复执行被拒绝；容器停止保留，未触碰用户数据库 |
| 实际 staging env 校验 | `scripts/staging/validate-env.ps1` | BLOCKED：TICKET_HOST 仍为占位符 |
| 前端独立单测 | package.json 没有 test script | BLOCKED：未执行不存在的 npm test；不能把 typecheck 当浏览器回归 |
| 完整容器构建/运行、Java/镜像漏洞审计、staging MySQL | 未执行 | STAGING_PENDING |

本轮没有新模型调用、没有运行数据库初始化/删除脚本、没有 Compose up/down。测试框架只使用并清理测试数据；未连接云数据库或改既有真实业务数据。

## 3. 部署阻塞和整改计划

| 编号 | 状态 | 实际发现 | 后续最小整改及验证 |
| --- | --- | --- | --- |
| T1 | LOCAL_PASS | staging Compose 已移除 `demo-data.sql`、本地 MySQL 及种子挂载；新增 `database/V1__schema_only.sql`，不含 Demo 业务写入 | 隔离 MySQL 验证通过；云端新空库初始化和受审查合成数据仍 STAGING_PENDING |
| T2 | LOCAL_PASS | Compose 已移除 MySQL 服务及容器依赖，改为显式 `TICKET_DB_*` RDS 私网 URL，要求 `sslMode=VERIFY_IDENTITY` | Compose 与 fixture 守卫通过；RDS endpoint、CA、白名单和连通性仍 STAGING_PENDING |
| T3 | LOCAL_PASS | `DATABASE_UPGRADES.md` 定义手动版本化 SQL、`ticket_schema_history`、checksum、备份和失败处理；API 启动不执行 demo init | 隔离 MySQL 首次/重放拒绝通过；云端备份恢复和后续版本演练仍 STAGING_PENDING |
| T4 | LOCAL_PASS | staging/production 数据库缺失 URL 时留空并快速失败；示例配置仍为占位并由守卫拒绝 | 真实服务器 Secret、正式 issuer/audience/client 和真实启动验收仍 BLOCKED |
| T5 | LOCAL_PASS | 两个构建上下文的 `.dockerignore` 已递归排除 `.env`、密钥和本地 profile 文件 | 需在网络可用后检查干净 context 和最终镜像；不声称已有公网泄漏 |
| T6 | BLOCKED | 未提交/未跟踪源码未构成完整 release；当前镜像未固定 digest | 独立源码版本、image digest、配置版本、DB schema版本、运行/回滚清单 |
| T7 | STAGING_PENDING | 限流仍为进程内；底层兼容配置保留历史回退键，但 staging 守卫禁止跨项目变量 | staging 固定单 API 实例并显式设置 TICKET_AI_*；多副本前另做共享 Redis/网关限流并验收 |

同主机直接运行两项目 Caddy 会争用80/443；两个目录名均为 staging，必须显式指定 `-p ticket-staging`，不得依赖默认 Compose project 名称。

## 3.1 本轮整改结果（2026-09-05）

以下改动已经写入工作区并在本地复验，范围只覆盖企业工单项目：

- `deploy/staging/docker-compose.yml` 不再创建或依赖本地 MySQL，也不挂载 `demo-data.sql`；API 通过 `TICKET_DB_*` 连接外部 RDS 私网地址，SQL init 保持关闭，OIDC/Provider/fallback 配置设为显式门禁。
- 新增 `deploy/staging/database/V1__schema_only.sql` 和 `manifest.json`，只创建空业务表与 `ticket_schema_history`，不含 `CREATE DATABASE`、`USE`、Demo 知识/工单写入或删除语句；`DATABASE_UPGRADES.md` 明确手动版本化执行、checksum、备份与失败恢复流程。
- staging/production 数据库 URL 缺失时现在留空并快速失败，不会悄悄回落到 localhost；`.dockerignore` 递归排除 `.env`、密钥和本地 profile 文件。
- `scripts/staging/validate-env.ps1` 的本项目 fixture 正向校验通过，模板按预期拒绝；没有读取、复制或输出任何真实 Secret。前端没有单测脚本，不能把不存在的命令伪装成通过。
- 最新 `mvn -B test package` 为 289/289；前端 typecheck/production build、npm 生产依赖审计和 Compose 普通/TLS config 通过。镜像构建在 Docker Hub 匿名 token 请求处网络超时，属于 STAGING_PENDING，不归因于源码构建成功。

整改后的剩余门禁仍是外部条件：实际阿里云账号会话、ECS/RDS 资源、正式 OIDC、服务器 Secret、Docker registry 可达性、镜像 digest/SBOM、DNS/ICP/TLS 和公网验收。没有执行公网部署或数据库删除。

## 3.2 用户提供的阿里云盘点证据（2026-09-05）

- 轻量应用服务器页面显示：华东 1（杭州）、`OpenClaw-astw`、运行中、公网 `47.98.192.15`、私网 `172.25.5.238`、2 vCPU/2 GiB/40 GiB、到期 2026-12-26。该主机只能作为候选 staging 宿主机；在确认现有工作负载、监听端口、CPU/内存/磁盘余量、系统用户、Docker、轻量应用服务器防火墙和 SSH 方式前，不得覆盖或启动任何项目。
- 域名页面显示 `wzl8.top` 状态“正常”、备案“已备案”、到期 2027-06-26，并显示“添加域名解析”操作。该证据不授权本轮修改 DNS；`ticket-staging.wzl8.top` 此前仍解析为 NXDOMAIN。
- 以上截图只更新资源盘点证据，不等于工单 staging 已部署、域名已解析、证书已签发或公网验收通过。

## 3.3 Workbench 只读主机审计（2026-09-06）

用户通过阿里云 Workbench 进入 `OpenClaw-astw`，并执行了不读取环境变量或 Secret 的只读检查。可确认：Alibaba Cloud Linux 4（Agentic Edition）、x86_64、Docker client/server 24.0.9 且服务 active、运行约 71 天、2 个 CPU、根盘 40 GiB 已用 11 GiB 可用 27 GiB。内存总量约 1.8 GiB，当前 available 约 1.0 GiB，swap 2 GiB。

当前只读监听结果包含 80、443、22，以及 127.0.0.1 上的 8080、18080、18443、35207，另有 Node 进程监听 18352。Docker 运行中的容器至少有 `searxng`（`searxng/searxng:latest`，已运行约两个月）；宿主机还有 Node、Nginx、Python 等现有进程。`firewalld` 和 `ufw` 服务显示 inactive，但阿里云轻量应用服务器的云侧防火墙规则尚未读取。

结论：该主机当前不能直接作为两个项目的 staging 目标。80/443 已被占用，约 1 GiB 可用内存不足以在未知现有负载下安全承载两个 Spring Boot API、Ticket Edge 和构建任务；现有 OpenClaw/SearXNG 服务也没有获得停机或覆盖授权。候选方案是单独准备容量更合适的主机，或由用户明确批准后先做资源扩容和共享入口设计；本轮不停止、重启、重配或删除现有服务。

## 4. 阿里云、域名与外部条件

本机未发现 PATH aliyun CLI、常规 `~/.aliyun/config.json` 或阿里云 connector。用户随后提供了阿里云控制台截图；截图属于用户提供的只读盘点证据，不能替代对实例详情、端口、防火墙、磁盘、VPC、RDS 和权限的逐项核实。

| 条件 | 实际观察 | 状态 |
| --- | --- | --- |
| ECS/地域/VPC/vSwitch/IP/SSH/安全组 | 截图和 Workbench 只读终端显示华东 1（杭州）轻量应用服务器 `OpenClaw-astw`，公网 `47.98.192.15`、私网 `172.25.5.238`、2 vCPU/约 1.8 GiB/40 GiB、Docker 24.0.9；当前可用内存约 1.0 GiB，80/443 已有监听，且存在现有 SearXNG/Node/Nginx/本地服务。它不是可直接占用的 ECS，主机防火墙、云侧规则、Docker 网络、端口归属和负载仍未完整核实 | BLOCKED |
| RDS MySQL | 实例、私网地址、数据库账号、白名单、备份/恢复均未验证 | BLOCKED |
| Redis | 当前工单单实例拓扑没有 Redis依赖；未核实云资源。多副本共享限流设计/验收另行进行 | STAGING_PENDING：不使用电商Redis验收替代 |
| 公开根域名DNS | wzl8.top A=47.98.192.15；NS=dns7.hichina.com、dns8.hichina.com（本机解析器，2026-09-05） | LOCAL_PASS：仅公开查询 |
| 工单子域名DNS | ticket-staging.wzl8.top A/AAAA返回NXDOMAIN | STAGING_PENDING |
| DNS管理权、ICP | 域名控制台截图显示 `wzl8.top` 状态“正常”、备案“已备案”、到期 2027-06-26，并提供“添加域名解析”操作；未点击、未修改记录，备案主体、接入商、解析权限和 staging 子域名关系仍需逐项核实 | STAGING_PENDING |
| TLS证书 | 云证书资产、有效期、续期和密钥路径未确认 | BLOCKED |
| 正式OIDC | HTTPS issuer、JWKS、audience、独立client、角色、生命周期/MFA未确认 | BLOCKED |
| 本地OIDC历史 | 已读取 local-oidc 的 ticket-agent PKCE及API401/403/200记录；本轮未重跑 | STAGING_PENDING：不作为本轮/公网验收 |
| 本地DeepSeek配置 | 仅做存在性/占位分类；TICKET_AI_API_KEY非空且非模板；文件被Git忽略，未输出值 | LOCAL_PASS：仅本地配置存在性；有效性/余额/服务器Secret为STAGING_PENDING |
| 真实Provider证据 | 本项目历史记录：SUCCESS、fallbackUsed=false、1 citation、VALID结构化输出/引用、人工复核RESOLVED；另记载一次输出门禁失败后重试成功 | STAGING_PENDING：本轮未新调用，也不代表稳定质量/SLA |

中国内地 ECS 对外 Web 服务在 staging 开放前核实备案及接入条件。参考：[阿里云备案流程 FAQ](https://help.aliyun.com/zh/icp-filing/basic-icp-service/support/for-the-record-process-faq)。wzl8.top 的实际 ICP 状态仍 BLOCKED。

## 5. 拓扑、隔离及费用

```text
ticket-staging.wzl8.top
  -> 工单 ECS Caddy/Nginx 80/443
  -> Vue 静态站点 + ticket-api（单实例，私有Docker网络）
  -> enterprise_ticket_staging（RDS MySQL，同地域/VPC，独立应用账号）
  -> HTTPS正式IdP（独立工单public client，PKCE S256）
  -> DeepSeek（仅后端使用工单服务器Secret）
```

- 优先复用满足条件的既有资源；默认规划独立ECS。容量起点2 vCPU/4 GiB、40–60 GiB盘，RDS MySQL8.x；仅评估假设，不是已核实SKU或压测结论。
- 如实际共用一台ECS，采用一个共享宿主机80/443入口，按域名分别转发工单127.0.0.1:8087和电商8088；禁用各app stack自带public TLS profile，保留独立Compose项目、网络、env、Secret、数据库、账号、日志、测试数据、备份和报告。
- Workbench 只读结果已确认现有候选主机为轻量应用服务器而非 ECS，约 1.0 GiB 可用内存且 80/443 已占用；当前不把它视为两个项目的可部署目标，也不在其上覆盖现有服务。应优先使用独立且容量合适的主机；若用户考虑扩容/复用，必须先完成资源变更评估和两项目隔离设计。
- 工单计划目录 `/srv/ticket-staging`；运行账号只获本项目库DML权限，迁移账号另设。若共用RDS物理实例，仍必须独立数据库与授权，并说明共享故障域。
- 公网只允许入口80/443，SSH22限可信IP/32；数据库/API/认证内部端口不公开。RDS走同地域/VPC私网，白名单最小化。
- 保持 `TICKET_AI_FALLBACK_TO_LOCAL=false`、OIDC、限流和Human Review。Agent/Reviewer/Viewer/Admin分别测试；前端不含client secret或AI Key。
- **费用估算：STAGING_PENDING。** 账号地域、实例余量、计费周期和折扣未核实，无法给可靠人民币报价。工单月增量公式：新增/扩容ECS + 云盘 + 公网流量/EIP + RDS + 超额备份/日志 + IdP（若收费）+ 工单模型调用；单实例不先购Redis。多副本方案另计共享限流成本。
- 本轮无云资源购买/创建、无新模型调用。实际采购前提供每项官方购物车报价、总月上限、自动续费选项；复用不等于零流量/备份费用。参考：[ECS计费概述](https://help.aliyun.com/zh/ecs/billing-overview)、[公网带宽计费](https://help.aliyun.com/zh/ecs/public-bandwidth/)。

## 6. 风险和回滚设计

| 风险/阶段 | 前置保护 | 回滚方法（未演练） |
| --- | --- | --- |
| 工作区改动被遗漏 | 不覆盖用户改动；完整源码和digest manifest | 只撤销独立发布整改，不使用reset/checkout/clean |
| 应用升级失败 | 留存旧image digest、配置版本和schema版本；分别测readiness及权限 | 恢复本项目上一image/config，仅重建API/edge；数据库和卷保留，不down -v |
| 首次发布失败 | 新建本项目schema-only库，使用受审查合成数据 | 停止新app，保留库/日志/证据；没有云端前版本时不得声称版本回退已验证 |
| 数据库迁移不兼容 | 备份并恢复到临时库；检查copilot_run/retrieval_hit/review_record/citation关系 | 兼容则只回退应用；不兼容则停写并恢复到新库，核对review history与增量后经确认切连接；不覆盖源库 |
| Provider结构化输出不稳定 | 单次失败如实记录；引用门禁和人工复核保持；限定重试预算 | 回退已验证Provider配置或暂停Copilot；不启用local fallback制造成功 |
| DNS/TLS错误 | 保存变更前记录、TTL、证书和入口配置 | 只恢复工单子域名及配置；DNS修改仍需确认，不动正式根域名 |
| 多副本绕过限流 | staging保持单API实例，扩容前共享限流 | 回到经验证的单实例配置，保持限流，不能称具备分布式保护 |
| 费用/误释放 | 项目标签、资源清单、预算 | 仅经确认停止/释放本项目无状态资源；不删除数据库、备份或磁盘 |

拟定RPO≤24h、RTO≤60min仅为演练目标，未经验证。每次升级先定义风险与兼容范围，保留完整审计和人工复核记录。

## 7. 独立验收及持续升级

以下均为STAGING_PENDING：DNS/HTTPS及续期；edge /health与内部readiness边界；匿名GET /api/tickets=401；Agent允许范围；无角色403；Viewer/Reviewer/Admin权限；Demo登录失效；真实DeepSeek SUCCESS/结构化输出/validated citation/Trace；401/429/5xx/超时/非法JSON失败矩阵；Human Review不可跳过；Approve/Request changes/Reject及review history；浏览器登录/列表/工作台/Trace/审核/刷新会话；MySQL恢复到临时库；日志/前端Secret检查；告警和版本回滚。

顺序：先完成 T6 的干净 release、镜像 digest/SBOM 和容器复验 → 云端只读资源清单和实际报价 → 用户确认对应外部变更 → 私网内部部署/readiness → 经确认DNS/TLS/公网staging → 完整项目验收 → P0正式身份/备份/监控/回滚/多副本限流 → P1失败矩阵、结构化输出和Citation评测、成本/延迟。不使用电商验收替代，不创建无人值守升级任务。

## 8. 最小外部信息

当前只需用户在已打开的阿里云登录页自行完成登录，并告知可只读盘点；不要发送密码、私钥、Key、Token、Cookie或验证码。

后续优先从控制台取得，仅补不可推断项：本项目资源归属和地域；SSH用户/私钥本地路径/可信IP；ticket-staging域名管理权与ICP页面状态；独立RDS库名、用户及私网endpoint；工单Secret存放位置（仅引用）；正式issuer、audience、public client、redirect；Agent/Reviewer/Viewer/Admin测试用户准备方式；月预算上限。无证据项保持BLOCKED，不借用电商配置。
