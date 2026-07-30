DROP TABLE IF EXISTS review_record;
DROP TABLE IF EXISTS retrieval_hit;
DROP TABLE IF EXISTS copilot_run;
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

CREATE TABLE copilot_run (
  run_id VARCHAR(96) PRIMARY KEY,
  trace_id VARCHAR(96) NOT NULL UNIQUE,
  ticket_id BIGINT NOT NULL,
  analysis_id BIGINT NULL,
  generation_record_id BIGINT NULL,
  requested_provider VARCHAR(64) NOT NULL,
  requested_protocol VARCHAR(48) NOT NULL,
  requested_model VARCHAR(128) NOT NULL,
  actual_provider VARCHAR(64) NOT NULL,
  actual_protocol VARCHAR(48) NOT NULL,
  run_status VARCHAR(32) NOT NULL,
  fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
  fallback_reason_code VARCHAR(80),
  error_category VARCHAR(64) NOT NULL DEFAULT 'NONE',
  sanitized_error_summary VARCHAR(300),
  started_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP NULL,
  total_latency_ms BIGINT NOT NULL DEFAULT 0,
  retrieval_hit_count INT NOT NULL DEFAULT 0,
  output_produced BOOLEAN NOT NULL DEFAULT FALSE,
  human_review_required BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_copilot_run_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id),
  CONSTRAINT fk_copilot_run_analysis FOREIGN KEY (analysis_id) REFERENCES ticket_ai_analysis (id),
  CONSTRAINT fk_copilot_run_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

CREATE INDEX idx_copilot_run_ticket_time ON copilot_run (ticket_id, started_at);
CREATE INDEX idx_copilot_run_status ON copilot_run (run_status);
CREATE INDEX idx_copilot_run_error_category ON copilot_run (error_category);

CREATE TABLE retrieval_hit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(96) NOT NULL,
  rank_order INT NOT NULL,
  knowledge_article_id BIGINT NULL,
  knowledge_article_no VARCHAR(48) NOT NULL,
  knowledge_title_snapshot VARCHAR(180) NOT NULL,
  knowledge_category_snapshot VARCHAR(64) NOT NULL,
  score INT NOT NULL,
  matched_keywords_snapshot VARCHAR(500) NOT NULL,
  excerpt_snapshot VARCHAR(300),
  used_in_draft BOOLEAN NOT NULL DEFAULT TRUE,
  retrieved_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_retrieval_hit_run_rank UNIQUE (run_id, rank_order),
  CONSTRAINT fk_retrieval_hit_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_retrieval_hit_article FOREIGN KEY (knowledge_article_id) REFERENCES knowledge_article (id)
);

CREATE INDEX idx_retrieval_hit_article_no ON retrieval_hit (knowledge_article_no);

CREATE TABLE review_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(96) NULL,
  ticket_id BIGINT NOT NULL,
  decision VARCHAR(32) NOT NULL,
  reviewer_name VARCHAR(96) NOT NULL,
  review_comment VARCHAR(500),
  previous_status VARCHAR(32),
  new_status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_review_record_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_review_record_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

CREATE INDEX idx_review_record_run_time ON review_record (run_id, created_at);
CREATE INDEX idx_review_record_ticket_time ON review_record (ticket_id, created_at);

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
