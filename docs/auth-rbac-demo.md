# JWT + RBAC Demo 说明

本文说明当前权限能力的真实边界：这是 demo 级 JWT + RBAC，不是生产级鉴权系统。

## Demo 用户

| 用户名 | 密码 | 角色 | 用途 |
| --- | --- | --- | --- |
| `admin` | `admin123` | `ADMIN` | 管理员演示 |
| `agent` | `agent123` | `AGENT` | 运行 Copilot |
| `reviewer` | `reviewer123` | `REVIEWER` | 审核建议草稿 |
| `viewer` | `viewer123` | `VIEWER` | 只读查看 |

以上账号只用于本地演示，不代表生产账号体系。

## 接口

登录：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"username":"agent","password":"agent123"}'
```

当前用户：

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/auth/me" `
  -Headers @{ Authorization = "Bearer <TOKEN>" }
```

## 角色边界

- `run-copilot`：`ADMIN / AGENT`
- `review/approve`：`ADMIN / REVIEWER`
- `review/request-changes`：`ADMIN / REVIEWER`
- `review/reject`：`ADMIN / REVIEWER`
- 查询工单、Trace Evidence：登录用户可查看，覆盖 `ADMIN / AGENT / REVIEWER / VIEWER`

未登录请求返回统一错误响应；角色不足返回 `403`。

## 明确边界

- 不做 SSO、OAuth2、LDAP、MFA 或多租户。
- 不做生产级会话管理、刷新令牌、审计登录、设备管理或权限后台。
- JWT 签名 Key 默认来自 `TICKET_AUTH_JWT_SIGNING_KEY`，没有配置时使用 demo 值，仅适合本地演示。
- 前端角色切换按钮用于演示 RBAC，不应理解为真实用户管理系统。
