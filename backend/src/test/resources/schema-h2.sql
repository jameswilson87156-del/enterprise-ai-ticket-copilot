-- 测试专用 H2（MySQL 模式）建表脚本：结构与 schema.sql 等价，去掉 MySQL 专有索引/外键语法
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
  ai_confidence TINYINT DEFAULT 0,
  resolved_summary TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
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
  last_verified_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_ai_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  classification VARCHAR(64) NOT NULL,
  classification_reason VARCHAR(600),
  confidence TINYINT NOT NULL,
  confirmation_state VARCHAR(32) NOT NULL,
  matched_knowledge_nos TEXT,
  troubleshooting_steps MEDIUMTEXT,
  reply_suggestion TEXT,
  risk_notes MEDIUMTEXT,
  source_type VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS generation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(48) NOT NULL,
  business_id BIGINT NOT NULL,
  source_type VARCHAR(48) NOT NULL,
  input_summary VARCHAR(300) NOT NULL,
  output_summary VARCHAR(300) NOT NULL,
  latency_ms BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_id BIGINT NOT NULL,
  from_status VARCHAR(32),
  to_status VARCHAR(32) NOT NULL,
  actor VARCHAR(96) NOT NULL,
  note VARCHAR(500),
  occurred_at TIMESTAMP NOT NULL
);
