# Enterprise AI Ticket Copilot 阿里云 Staging 部署与持续升级交接文档

适用项目：D:/workhome/enterprise-ai-ticket-copilot

目标：把当前本地已经验收的企业工单 AI Copilot 部署到阿里云 staging 环境，再按照验收结果持续升级。

生成日期：2026-09-05

重要边界：本文件是部署交接和给下一个 AI 的执行提示词，不代表项目已经部署到公网。任何购买资源、修改 DNS、申请证书、导入真实数据、删除数据、切换正式域名或产生费用的操作，都必须先停下来获得用户明确确认。

---

## 1. 当前项目真实状态

### 已完成

- 后端全量 Maven 测试当前为 289/289，0 failures、0 errors、0 skipped。
- 前端 type-check 和 production build 已通过。
- 前端 Demo 与真实 API 边界已明确，真实模式不会因为后端不可用而静默回退到 Mock。
- 后端已经具备 OIDC Resource Server、issuer/JWKS、audience 和角色映射代码路径。
- 前端已经具备 Authorization Code + PKCE 企业登录适配；AI API Key 只在服务端。
- staging Compose 骨架已经包含 MySQL、Java API、前端 Edge 和可选 Caddy TLS。
- 启动守卫会拒绝不完整 OIDC、数据库、Provider、unsafe fallback 和不安全配置。
- 真实 DeepSeek 本地合成验收已通过：

~~~text
Provider: deepseek
Model: deepseek-chat
runStatus: SUCCESS
fallbackUsed: false
validated citations: 1
structured output: VALID
citation membership: VALID
Reviewer final status: RESOLVED
~~~

- 本地隔离 Keycloak OIDC/PKCE 已通过 ticket-agent 的协议验证；本地 API 401/403/200 权限路径也已验证。
- 用户已经在本地 5298 页面手动看到真实后端、真实 Provider 状态和已解决合成工单。

### 尚未完成，不能写成已上线

- 尚未完成阿里云 ECS staging 的真实部署。
- 尚未完成正式 OIDC/IdP、正式用户生命周期和 MFA 验收。
- 尚未完成正式 HTTPS 域名下的浏览器验收。
- 尚未完成云端 MySQL 备份恢复。
- 尚未完成 staging Provider 失败矩阵。
- 当前进程内 rate limit 只适合单实例；多副本需要网关/WAF 或共享 Redis 限流。
- 当前本地 Keycloak、H2、Demo 用户和测试密码不能进入 production。
- 尚未完成正式 DNS、TLS、监控、告警、回滚和公网发布。

### 证据位置

- 真实 DeepSeek：docs/evidence/deepseek-synthetic-smoke-20260905.md
- 本地 HTTP smoke：scripts/local/verify-http-smoke.ps1
- 本地 OIDC/PKCE：D:/workhome/local-oidc/PKCE_SMOKE_EVIDENCE_20260905.md
- 本地身份边界：D:/workhome/local-oidc/OIDC_SMOKE_EVIDENCE_20260905.md
- 认证、Provider 和 staging 说明：docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md
- 项目交接：HANDOFF.md
- 任务清单：TODO.md

如果仓库文档和最新可复现证据冲突，以最新测试输出和实际配置为准；交付前必须修正文档。

---

## 2. 推荐的阿里云 staging 拓扑

第一阶段只做 staging，不直接切换正式域名。

~~~text
ticket-staging.<DOMAIN>
        |
        v
      ECS Edge
  Nginx / Caddy
  80 / 443
        |
        +-- Vue 工单前端静态资源
        +-- Spring Boot Ticket API
                 |
                 +-- MySQL：同地域、同 VPC、私网连接
                 +-- 正式 OIDC/IdP：HTTPS issuer
                 +-- DeepSeek：服务端 API Key
~~~

项目当前 Compose 使用 MySQL、Java API、Ticket Edge 和可选 Caddy TLS。不要把初始化 demo-data.sql 直接挂到 production 数据库。

注意：当前限流是单进程保护。单实例 staging 可以先验收；如果生产部署多个 API 副本，必须增加共享网关/WAF 或 Redis 限流，不得把进程内限流描述成分布式防护。

---

## 3. 用户需要准备的输入

下一个 AI 不允许要求用户把密码、私钥、API Key、OIDC Token 或浏览器 Cookie 粘贴到聊天中。

### 阿里云资源

~~~text
ALIYUN_REGION
ECS_INSTANCE_ID
ECS_PUBLIC_IP
ECS_PRIVATE_IP
ECS_SSH_USER
ECS_SSH_KEY_PATH
ECS_VPC_ID
ECS_VSWITCH_ID
~~~

如果 ECS 还不存在，下一个 AI 必须先报告预计资源、预算和购买动作，等待用户明确确认后才能创建。

### 域名

~~~text
ROOT_DOMAIN=wzl8.top
STAGING_TICKET_HOST=ticket-staging.wzl8.top
DNS_PROVIDER=aliyun-or-other
ICP_STATUS=completed|in_progress|not_started|unknown
~~~

### OIDC

~~~text
TICKET_AUTH_ISSUER_URI
TICKET_AUTH_AUDIENCE
TICKET_AUTH_ROLES_CLAIM=roles
TICKET_AUTH_PRINCIPAL_CLAIM=sub
VITE_TICKET_AUTH_CLIENT_ID
TICKET_AUTH_REDIRECT_URI
OIDC_TEST_USER_PLAN
~~~

前端 Client 使用 Authorization Code + PKCE（S256），不能使用 Client Secret。

### AI Provider

DeepSeek Key 已经准备过，不需要再次发送给 AI。只需把它放到服务器端 Secret：

~~~text
TICKET_AI_PROVIDER=deepseek
TICKET_AI_BASE_URL=https://api.deepseek.com/v1
TICKET_AI_PROTOCOL=chat-completions
TICKET_AI_MODEL=deepseek-chat
TICKET_AI_API_KEY=<server-side-secret>
TICKET_AI_FALLBACK_TO_LOCAL=false
~~~

Key 不得进入前端、日志、截图、Git 或聊天内容。

---

## 4. 阿里云控制台准备顺序

### Step 0：先盘点，不购买

检查：

1. ECS：实例、地域、VPC、私网 IP、公网 IP。
2. 云解析 DNS：是否管理 wzl8.top。
3. ICP 备案：已备案、备案中还是未备案。
4. MySQL：是否已有 RDS 实例。
5. 证书：是否已有 staging 证书。

### Step 1：ECS 和安全组

公网入方向最终只允许：

~~~text
TCP 22   用户固定公网 IP /32
TCP 80   0.0.0.0/0
TCP 443  0.0.0.0/0
~~~

不要开放：

~~~text
3306、8080、8087、8180、5295、5298
~~~

官方文档：

https://help.aliyun.com/zh/ecs/user-guide/start-using-security-groups

### Step 2：MySQL

优先使用和 ECS 同地域、同 VPC 的 RDS MySQL：

1. 使用私网地址。
2. 单独创建应用用户。
3. 不使用 root 连接 API。
4. 配置白名单。
5. 配置自动备份和恢复演练。
6. 不把数据库地址和密码写入 Git。

官方文档：

https://help.aliyun.com/zh/rds/support/how-do-i-connect-to-an-apsaradb-rds-instance

### Step 3：DNS 和 HTTPS

staging 先添加：

~~~text
类型：A
主机记录：ticket-staging
记录值：ECS 公网 IP
~~~

项目的 Caddy TLS profile 只能在域名已经指向 ECS、80/443 可访问并且 ACME 邮箱真实时启动。

DNS 官方文档：

https://help.aliyun.com/zh/dns/pubz-add-website-parsing

如果 ECS 在中国内地，正式域名对外访问前需要 ICP 备案：

https://beian.aliyun.com/

证书私钥只能存放在服务器 Secret 路径：

https://help.aliyun.com/zh/ssl-certificate/download-an-ssl-certificate

---

## 5. 给下一个 AI 的可复制执行提示词

下面整段可以直接复制给下一个 AI。

~~~text
你是 Enterprise AI Ticket Copilot 的部署、发布、认证、安全和持续升级负责人。

项目目录：
D:/workhome/enterprise-ai-ticket-copilot

目标：
先把项目部署到阿里云 staging，不直接上线正式域名；staging 验收通过后，再按 TODO 持续升级。

必须先做：
1. 读取 README.md、HANDOFF.md、TODO.md、docs/REAL_AUTH_PROVIDER_DEPLOYMENT.md、docs/evidence、deploy/staging 和 scripts/local。
2. 检查当前工作区，不得使用 git reset --hard、git checkout --、git clean、删除用户文件或覆盖既有未提交修改。
3. 运行后端全量测试、前端 type-check、前端 production build、Compose 静态校验和依赖审计。
4. 先输出部署计划，列出已通过、未完成、需要用户提供的资源、预计费用、每一步回滚方式和需要用户确认的动作。
5. 在用户确认前，不购买 ECS/RDS/证书，不改 DNS，不切换正式域名，不删除数据，不执行破坏性迁移。

当前真实边界：
- 本地真实 DeepSeek 合成验收已通过，但不等于公网模型质量、成本、并发或生产 SLA 已通过。
- 本地 OIDC/PKCE 和 API 401/403/200 边界已通过，但不等于正式 IdP、真实用户生命周期、MFA 和公网浏览器验收已通过。
- H2、Demo 用户、local Keycloak 和测试密码不得进入 staging/production。
- 不能把本地协议 smoke 写成公网浏览器验收。

推荐阿里云拓扑：
- 一个 ECS 运行 Caddy/Nginx、Vue 前端静态资源和 Spring Boot Ticket API。
- MySQL 与 ECS 同地域同 VPC，应用使用私网连接。
- 正式 OIDC 使用 HTTPS issuer。
- DeepSeek API Key 只注入 Spring Boot 服务端 Secret。
- 公网只暴露 80/443。
- SSH 22 只允许用户固定公网 IP。
- MySQL、Java 内部端口、Keycloak 内部端口和本地开发端口不暴露公网。

安全规则：
- API Key、数据库密码、SSH 私钥、OIDC Token 不能出现在命令输出、日志、截图、前端 bundle、Git 或聊天内容中。
- 不要自动登录第三方控制台或替用户购买付费资源。
- 不要使用不明来源的第三方部署脚本。
- 不要因为部署失败而关闭 TLS、CORS、OIDC 或 fail-closed 限流。
- TICKET_AI_FALLBACK_TO_LOCAL=false 必须在 staging/production 保持关闭。
- 单进程限流不能宣称为多副本分布式防护；多副本前必须增加共享网关/WAF 或 Redis 限流。

Phase A - 资源盘点：
- 记录 ECS ID、地域、VPC、vSwitch、公网 IP、私网 IP、SSH 用户和密钥路径。
- 确认 wzl8.top 的 DNS 管理权和 ICP 状态。
- 确认 MySQL、OIDC、DeepSeek Secret 是否存在。
- 缺少信息时，一次只向用户询问最少的一项。

Phase B - 本地基线：
- 执行 backend/mvn test。
- 执行 frontend 的 type-check 和 production build。
- 执行生产依赖审计。
- 执行 docker compose --env-file deploy/staging/.env.example -f deploy/staging/docker-compose.yml config -q。
- 执行真实 DeepSeek synthetic smoke，但不要把 API Key 写进命令输出。
- 如果失败，先修复根因，再进入云端。

Phase C - 云资源和网络：
- 只有在用户确认费用和地域后创建资源。
- ECS 采用 SSH Key，不把 root 密码作为长期方案。
- 安全组只开 22（可信 IP）、80、443。
- MySQL 使用私网地址和白名单。
- 记录所有资源 ID 和回滚操作。

Phase D - Secret 和配置：
- 使用服务器未提交 .env、阿里云 Secret 或同等安全存储。
- 复制 deploy/staging/.env.example 到服务器受保护路径。
- 替换全部 CHANGE_ME。
- TICKET_AUTH_ISSUER_URI 必须是 HTTPS 且容器能访问 discovery/JWKS。
- 前端只允许公开 OIDC Client ID，不允许出现 Client Secret 或 AI Key。
- CORS 只写准确的 HTTPS origin。
- 不要把 demo-data.sql 作为 production 数据初始化脚本。

Phase E - 部署：
- 先在 ticket-staging 子域名部署。
- 运行环境变量校验。
- 运行 docker compose config -q。
- 拉起 MySQL、Ticket API、Ticket Edge。
- 先从内部网络检查 API readiness，再从公网检查 edge /health。
- Caddy TLS 只有在 DNS 和 80/443 已确认后启动。
- 记录镜像版本、数据库迁移版本、部署时间和回滚版本。

Phase F - 验收：
必须输出每一项的 PASS、FAIL 或 BLOCKED 和证据：

1. staging 域名 DNS 已解析。
2. HTTPS 证书有效且能自动续期。
3. /health 对外可用；内部 actuator/readiness 不对公网开放。
4. OIDC 模式下匿名 GET /api/tickets 返回 401。
5. 有效 Agent Token 可以读取和处理允许范围内的工单。
6. 缺少角色的 Token 对受限动作返回 403。
7. Demo admin/admin123 在 staging 不可用。
8. 真实 DeepSeek 返回 SUCCESS、结构化输出、validated citation 和 Trace。
9. Provider 401、429、5xx、超时、非法 JSON 时返回可识别失败，不生成假成功。
10. 工单必须经过 Human Review，不能由 Copilot 自动跳过审核。
11. Approve、Request changes、Reject 的状态和 review history 正确。
12. 浏览器登录、工单列表、工作台、Trace、人工复核流程通过。
13. 刷新页面后的 OIDC 会话行为符合设计。
14. MySQL 备份可以恢复到临时实例。
15. 服务器日志没有 API Key、Token、密码。

Phase G - 持续升级：
- 先做 P0：正式 OIDC、备份恢复、HTTPS、监控、回滚、多副本限流设计。
- 再做 P1：Provider 失败矩阵、结构化输出稳定性、Citation 质量评测、成本和延迟指标。
- 每次升级必须先写目标和风险，只修改相关文件，运行相关测试，做浏览器验收，更新 HANDOFF/TODO/证据，并输出回滚方式。
- 不要为了“看起来高级”添加没有真实业务闭环的功能。

交付格式：
1. 当前状态：LOCAL_PASS、STAGING_PASS、PUBLIC_PASS 或 BLOCKED。
2. 已完成清单。
3. 未完成清单。
4. 真实命令和测试结果。
5. 云资源清单，但不要输出 Secret。
6. 域名、TLS、OIDC、Provider、MySQL 和限流验收结果。
7. 风险和回滚方式。
8. 下一步只给用户一个最小可执行动作。

任何不能验证的内容必须写成 PENDING 或 BLOCKED，不能写成已完成。
~~~

---

## 6. Staging 完成门槛

只有下面全部满足，才能讨论正式域名：

- ECS 和 MySQL 连接稳定。
- 80/443 可访问，22 不对全网开放。
- MySQL 不暴露公网。
- HTTPS 有效，证书能续期。
- OIDC issuer、audience、roles、PKCE 回调全部正确。
- Agent、Reviewer、Viewer、Admin 的权限边界通过。
- DeepSeek 真实成功和失败矩阵通过。
- API Key、Token、密码不进入日志和前端。
- 数据库备份可以实际恢复。
- 后端、前端、Compose 和依赖审计通过。
- staging 浏览器完整流程通过。
- 有版本号、回滚命令和发布记录。

---

## 7. 当前结论

~~~text
本地功能和测试：已通过
本地真实 DeepSeek：已通过
本地 OIDC/PKCE/API 权限：已通过
阿里云 staging 配置骨架：已准备
阿里云真实资源：待盘点/待确认
正式 OIDC：待配置
DNS/HTTPS/备份恢复：待 staging 验收
公网部署：未完成
~~~

下一个 AI 必须从资源盘点开始，不得直接购买、改 DNS 或宣布公网部署完成。
