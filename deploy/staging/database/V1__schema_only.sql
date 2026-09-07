-- New empty database only; run with mysql batch mode, without --force.
CREATE TABLE support_ticket (
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

CREATE TABLE knowledge_article (
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

CREATE TABLE ticket_ai_analysis (
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
  created_at DATETIME NOT NULL,
  INDEX idx_generation_record_business (business_type, business_id),
  INDEX idx_generation_record_created_at (created_at)
);

CREATE TABLE ticket_status_history (
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
  started_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  total_latency_ms BIGINT NOT NULL DEFAULT 0,
  retrieval_hit_count INT NOT NULL DEFAULT 0,
  output_produced BOOLEAN NOT NULL DEFAULT FALSE,
  human_review_required BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME NOT NULL,
  INDEX idx_copilot_run_ticket_time (ticket_id, started_at),
  INDEX idx_copilot_run_status (run_status),
  INDEX idx_copilot_run_error_category (error_category),
  CONSTRAINT fk_copilot_run_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id),
  CONSTRAINT fk_copilot_run_analysis FOREIGN KEY (analysis_id) REFERENCES ticket_ai_analysis (id),
  CONSTRAINT fk_copilot_run_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

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
  retrieved_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_retrieval_hit_run_rank (run_id, rank_order),
  INDEX idx_retrieval_hit_article_no (knowledge_article_no),
  CONSTRAINT fk_retrieval_hit_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_retrieval_hit_article FOREIGN KEY (knowledge_article_id) REFERENCES knowledge_article (id)
);

CREATE TABLE review_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(96) NULL,
  ticket_id BIGINT NOT NULL,
  decision VARCHAR(32) NOT NULL,
  reviewer_name VARCHAR(96) NOT NULL,
  review_comment VARCHAR(500),
  previous_status VARCHAR(32),
  new_status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_review_record_run_time (run_id, created_at),
  INDEX idx_review_record_ticket_time (ticket_id, created_at),
  CONSTRAINT fk_review_record_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_review_record_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (id)
);

CREATE TABLE copilot_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  run_id VARCHAR(96) NOT NULL,
  analysis_id BIGINT NULL,
  generation_record_id BIGINT NULL,
  answer VARCHAR(1200) NOT NULL,
  abstained BOOLEAN NOT NULL DEFAULT FALSE,
  abstention_reason_code VARCHAR(64) NOT NULL DEFAULT 'NONE',
  risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
  model_human_review_required BOOLEAN NOT NULL DEFAULT TRUE,
  final_human_review_required BOOLEAN NOT NULL DEFAULT TRUE,
  citation_validation_status VARCHAR(48) NOT NULL DEFAULT 'NOT_APPLICABLE',
  citation_rejection_reason_code VARCHAR(64) NOT NULL DEFAULT 'NONE',
  valid_citation_count INT NOT NULL DEFAULT 0,
  rejected_citation_count INT NOT NULL DEFAULT 0,
  output_validation_status VARCHAR(48) NOT NULL DEFAULT 'NOT_APPLICABLE',
  missing_information_json VARCHAR(600),
  source_type VARCHAR(48) NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_copilot_result_run (run_id),
  INDEX idx_copilot_result_analysis (analysis_id),
  INDEX idx_copilot_result_validation (citation_validation_status, output_validation_status),
  CONSTRAINT fk_copilot_result_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_copilot_result_analysis FOREIGN KEY (analysis_id) REFERENCES ticket_ai_analysis (id),
  CONSTRAINT fk_copilot_result_generation FOREIGN KEY (generation_record_id) REFERENCES generation_record (id)
);

CREATE TABLE copilot_result_citation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  result_id BIGINT NOT NULL,
  run_id VARCHAR(96) NOT NULL,
  retrieval_hit_id BIGINT NOT NULL,
  knowledge_article_id VARCHAR(48) NOT NULL,
  knowledge_title_snapshot VARCHAR(180) NOT NULL,
  citation_type VARCHAR(32) NOT NULL DEFAULT 'VALIDATED_CITATION',
  supported_claim VARCHAR(300),
  evidence_excerpt VARCHAR(300),
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_copilot_result_citation_run_article (run_id, knowledge_article_id),
  INDEX idx_copilot_result_citation_result (result_id),
  INDEX idx_copilot_result_citation_hit (retrieval_hit_id),
  CONSTRAINT fk_copilot_result_citation_result FOREIGN KEY (result_id) REFERENCES copilot_result (id),
  CONSTRAINT fk_copilot_result_citation_run FOREIGN KEY (run_id) REFERENCES copilot_run (run_id),
  CONSTRAINT fk_copilot_result_citation_hit FOREIGN KEY (retrieval_hit_id) REFERENCES retrieval_hit (id)
);

CREATE TABLE ticket_schema_history (
  version INT PRIMARY KEY,
  script_name VARCHAR(160) NOT NULL,
  applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO ticket_schema_history (version, script_name) VALUES (1, 'V1__schema_only.sql');
