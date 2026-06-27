DROP TABLE IF EXISTS ticket_status_history;
DROP TABLE IF EXISTS generation_record;
DROP TABLE IF EXISTS ticket_ai_analysis;
DROP TABLE IF EXISTS knowledge_article;
DROP TABLE IF EXISTS support_ticket;

CREATE TABLE support_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(40) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  description CLOB NOT NULL,
  system_name VARCHAR(96) NOT NULL,
  error_log CLOB,
  urgency VARCHAR(16) NOT NULL,
  requester VARCHAR(64) NOT NULL,
  requester_department VARCHAR(96) NOT NULL,
  status VARCHAR(32) NOT NULL,
  category VARCHAR(32) NOT NULL,
  ai_confidence TINYINT DEFAULT 0,
  resolved_summary CLOB,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_support_ticket_status ON support_ticket (status);
CREATE INDEX idx_support_ticket_category ON support_ticket (category);
CREATE INDEX idx_support_ticket_updated_at ON support_ticket (updated_at);

CREATE TABLE knowledge_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_no VARCHAR(48) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  category VARCHAR(32) NOT NULL,
  keywords VARCHAR(500) NOT NULL,
  content CLOB NOT NULL,
  owner VARCHAR(96) NOT NULL,
  status VARCHAR(24) NOT NULL,
  source_ticket_id BIGINT NULL,
  last_verified_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_knowledge_source_ticket FOREIGN KEY (source_ticket_id) REFERENCES support_ticket (id)
);

CREATE INDEX idx_knowledge_status_category ON knowledge_article (status, category);
CREATE INDEX idx_knowledge_source_ticket ON knowledge_article (source_ticket_id);

CREATE TABLE ticket_ai_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  classification VARCHAR(64) NOT NULL,
  classification_reason VARCHAR(600),
  confidence TINYINT NOT NULL,
  confirmation_state VARCHAR(32) NOT NULL,
  matched_knowledge_nos CLOB,
  troubleshooting_steps CLOB,
  reply_suggestion CLOB,
  risk_notes CLOB,
  source_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_ticket_ai_analysis_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

CREATE INDEX idx_ticket_ai_analysis_ticket ON ticket_ai_analysis (ticket_id);

CREATE TABLE generation_record (
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
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_generation_record_business ON generation_record (business_type, business_id);
CREATE INDEX idx_generation_record_created_at ON generation_record (created_at);

CREATE TABLE ticket_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  actor VARCHAR(96) NOT NULL,
  note VARCHAR(500),
  occurred_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_ticket_status_history_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

CREATE INDEX idx_ticket_status_history_ticket_time ON ticket_status_history (ticket_id, occurred_at);

INSERT INTO knowledge_article (
  article_no, title, category, keywords, content, owner, status, last_verified_at, created_at, updated_at
) VALUES (
  'KB-OPS-003',
  '系统故障超时与 5xx 排查手册',
  '系统故障',
  '500,error,timeout,payment-service',
  '对照发布窗口、错误率和依赖服务状态，处理动作必须人工确认。',
  'SRE Enablement',
  'PUBLISHED',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
