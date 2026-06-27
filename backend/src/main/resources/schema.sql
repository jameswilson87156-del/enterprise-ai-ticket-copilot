CREATE DATABASE IF NOT EXISTS enterprise_ai_ticket_copilot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE enterprise_ai_ticket_copilot;

CREATE TABLE IF NOT EXISTS support_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(40) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  description TEXT NOT NULL,
  system_name VARCHAR(96) NOT NULL,
  error_log MEDIUMTEXT,
  urgency VARCHAR(16) NOT NULL,
  requester VARCHAR(64) NOT NULL,
  requester_department VARCHAR(96) NOT NULL,
  status VARCHAR(32) NOT NULL,
  category VARCHAR(32) NOT NULL,
  ai_confidence TINYINT UNSIGNED DEFAULT 0,
  resolved_summary TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_support_ticket_status (status),
  INDEX idx_support_ticket_category (category),
  INDEX idx_support_ticket_updated_at (updated_at)
);

CREATE TABLE IF NOT EXISTS knowledge_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_no VARCHAR(48) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  category VARCHAR(32) NOT NULL,
  keywords VARCHAR(500) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  owner VARCHAR(96) NOT NULL,
  status VARCHAR(24) NOT NULL,
  source_ticket_id BIGINT NULL,
  last_verified_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_knowledge_status_category (status, category),
  INDEX idx_knowledge_source_ticket (source_ticket_id),
  CONSTRAINT fk_knowledge_source_ticket FOREIGN KEY (source_ticket_id) REFERENCES support_ticket (id)
);

CREATE TABLE IF NOT EXISTS ticket_ai_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  classification VARCHAR(64) NOT NULL,
  classification_reason VARCHAR(600),
  confidence TINYINT UNSIGNED NOT NULL,
  confirmation_state VARCHAR(32) NOT NULL,
  matched_knowledge_nos TEXT,
  troubleshooting_steps MEDIUMTEXT,
  reply_suggestion TEXT,
  risk_notes MEDIUMTEXT,
  source_type VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  INDEX idx_ticket_ai_analysis_ticket (ticket_id),
  CONSTRAINT fk_ticket_ai_analysis_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

CREATE TABLE IF NOT EXISTS generation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(48) NOT NULL,
  business_id BIGINT NOT NULL,
  source_type VARCHAR(48) NOT NULL,
  provider_name VARCHAR(64) NOT NULL DEFAULT 'local-rule',
  model_name VARCHAR(128) NOT NULL DEFAULT 'N/A (no LLM)',
  fallback_used BOOLEAN NOT NULL DEFAULT TRUE,
  fallback_reason VARCHAR(64),
  error_message VARCHAR(300),
  input_summary VARCHAR(300) NOT NULL,
  output_summary VARCHAR(300) NOT NULL,
  latency_ms BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_generation_record_business (business_type, business_id),
  INDEX idx_generation_record_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS ticket_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  actor VARCHAR(96) NOT NULL,
  note VARCHAR(500),
  occurred_at DATETIME NOT NULL,
  INDEX idx_ticket_status_history_ticket_time (ticket_id, occurred_at),
  CONSTRAINT fk_ticket_status_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

INSERT INTO knowledge_article (
  article_no, title, category, keywords, content, owner, status, last_verified_at, created_at, updated_at
) VALUES
  (
    'KB-ACCOUNT-001',
    '账号登录失败与 MFA 重置处理流程',
    '账号问题',
    '账号,登录,密码,MFA,SSO,认证,锁定',
    '核验员工身份后检查账号锁定状态、MFA 绑定和 SSO 审计日志。任何重置动作必须由人工确认。',
    'IT Identity',
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
  ),
  (
    'KB-IAM-002',
    '权限申请与最小权限核验清单',
    '权限问题',
    '权限,授权,角色,RBAC,审批,访问,共享盘,报表',
    '核验申请人、审批记录、目标资源和最小权限角色。授权动作不得由规则引擎自动执行。',
    'IT Governance',
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
  ),
  (
    'KB-OPS-003',
    '系统故障超时与 5xx 排查手册',
    '系统故障',
    '502,500,timeout,超时,异常,报错,服务,网关,error',
    '比对发布窗口、告警、依赖服务延迟和错误率。生产变更、降级或回滚必须由值班人员确认。',
    'SRE Enablement',
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
  ),
  (
    'KB-DATA-004',
    '数据同步异常与报表口径排查',
    '数据问题',
    '数据,同步,报表,不一致,缺失,导入,导出,指标',
    '确认数据源、同步批次、口径变更和抽样记录，必要时保留修复前后的审计材料。',
    'Data Platform',
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
  ),
  (
    'KB-PROCESS-005',
    '内部流程咨询分派与知识沉淀规范',
    '流程咨询',
    '流程,如何,怎么,申请,规范,指引,步骤,咨询',
    '将流程类问题转交对应流程负责人，处理完成后补充可复用指引并进入知识审核。',
    'Service Desk',
    'PUBLISHED',
    NOW(),
    NOW(),
    NOW()
  )
ON DUPLICATE KEY UPDATE
  title = VALUES(title),
  category = VALUES(category),
  keywords = VALUES(keywords),
  content = VALUES(content),
  owner = VALUES(owner),
  status = VALUES(status),
  updated_at = NOW();
