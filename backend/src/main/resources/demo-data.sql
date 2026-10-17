USE enterprise_ai_ticket_copilot;

DELETE gr FROM generation_record gr
JOIN support_ticket st ON gr.business_id = st.id
WHERE st.ticket_no LIKE 'DEMO-%';

DELETE tsh FROM ticket_status_history tsh
JOIN support_ticket st ON tsh.ticket_id = st.id
WHERE st.ticket_no LIKE 'DEMO-%';

DELETE taa FROM ticket_ai_analysis taa
JOIN support_ticket st ON taa.ticket_id = st.id
WHERE st.ticket_no LIKE 'DEMO-%';

DELETE FROM knowledge_article
WHERE article_no IN (
  'KB-JAVA-PORT', 'KB-SPRING-BEAN', 'KB-REDIS-CONN', 'KB-MYSQL-SLOW',
  'KB-API-500', 'KB-IAM-ROLE', 'KB-DEPLOY-ENV', 'KB-FAQ-KNOWLEDGE',
  'KB-DRAFT-DEMO-0008'
);

DELETE FROM support_ticket WHERE ticket_no LIKE 'DEMO-%';

INSERT INTO knowledge_article
  (article_no, title, category, keywords, content, owner, status, last_verified_at, created_at, updated_at)
VALUES
  ('KB-JAVA-PORT', 'Java 服务端口占用排查手册', '系统故障', 'Java,端口,占用,BindException,Tomcat,8080', '检查端口监听进程，确认是否为旧实例未退出。释放端口或调整 server.port 前必须由人工确认。', 'SRE Enablement', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-SPRING-BEAN', 'Spring Boot BeanCreationException 处理清单', '系统故障', 'Spring Boot,BeanCreationException,bean,依赖注入,启动失败', '定位异常 bean、配置项、构造器依赖和 profile。变更配置或回滚版本需要人工确认。', 'Java Platform', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-REDIS-CONN', 'Redis 连接失败与连接池排查', '系统故障', 'Redis,连接失败,timeout,Lettuce,Jedis,连接池', '检查 Redis 地址、网络 ACL、密码、连接池耗尽和超时参数。生产参数调整必须人工确认。', 'Infra Platform', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-MYSQL-SLOW', 'MySQL 慢查询定位与索引评估流程', '数据问题', 'MySQL,慢查询,索引,SQL,执行计划,锁等待', '收集慢查询 SQL、执行计划、索引命中和锁等待。上线索引前需要评估写入影响。', 'DBA Team', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-API-500', '接口 500 错误分层排查手册', '系统故障', '接口,500,error,traceId,异常,网关', '使用 traceId 关联网关、应用日志和依赖调用。恢复动作和对外口径必须人工确认。', 'Service Desk', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-IAM-ROLE', '权限配置问题与 RBAC 核验流程', '权限问题', '权限,RBAC,角色,403,Forbidden,访问控制,审批', '核对审批、角色、资源策略和审计记录。授权动作不能自动执行。', 'IT Governance', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-DEPLOY-ENV', '部署环境变量缺失排查清单', '系统故障', '部署,环境变量,配置,profile,container,Kubernetes', '比对环境变量、ConfigMap、Secret 和启动参数。修改部署配置必须人工确认。', 'Release Engineering', 'PUBLISHED', NOW(), NOW(), NOW()),
  ('KB-FAQ-KNOWLEDGE', '重复咨询转知识库沉淀规范', '流程咨询', '重复咨询,知识库,FAQ,流程,沉淀,审核', '识别重复问题后生成知识草稿，由知识负责人确认后发布。', 'Knowledge Ops', 'PUBLISHED', NOW(), NOW(), NOW());

INSERT INTO support_ticket
  (ticket_no, title, description, system_name, error_log, urgency, requester, requester_department, status, category, ai_confidence, resolved_summary, created_at, updated_at)
VALUES
  ('DEMO-0001', 'Java 服务启动失败，提示 8080 端口已被占用', '发布 order-service 后新实例无法启动，启动日志显示端口绑定失败，影响测试环境验收。', 'order-service', 'java.net.BindException: Address already in use: bind 0.0.0.0:8080', 'P2', '李航', '研发效能', 'IN_PROGRESS', '系统故障', 91, NULL, NOW() - INTERVAL 8 HOUR, NOW() - INTERVAL 3 HOUR),
  ('DEMO-0002', 'Spring Boot 启动时报 BeanCreationException', 'payment-service 发布后启动失败，怀疑新增结算策略 bean 缺少配置。', 'payment-service', 'org.springframework.beans.factory.BeanCreationException: Error creating bean with name settlementStrategy', 'P1', '周冉', '支付研发', 'PENDING_PROCESS', '系统故障', 88, NULL, NOW() - INTERVAL 7 HOUR, NOW() - INTERVAL 2 HOUR),
  ('DEMO-0003', 'Redis 连接失败导致会话校验超时', '员工门户登录后频繁掉线，应用日志显示 Redis timeout，影响多个部门。', 'employee-portal', 'io.lettuce.core.RedisConnectionException: Unable to connect to redis-prod:6379', 'P1', '唐宁', '员工体验', 'IN_PROGRESS', '系统故障', 90, NULL, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 90 MINUTE),
  ('DEMO-0004', 'CRM 客户列表查询超过 12 秒', '客户成功团队反馈 CRM 客户列表打开很慢，筛选续费状态后等待时间明显变长。', 'crm-analytics', 'slow sql: select * from customer_account where renewal_status = ? order by updated_at desc', 'P2', '韩岚', '客户成功', 'PENDING_PROCESS', '数据问题', 84, NULL, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 70 MINUTE),
  ('DEMO-0005', '报价接口返回 500，销售无法生成报价单', '销售在报价中心提交企业客户报价时接口返回 500，错误集中在华东区域。', 'quote-center', 'HTTP 500 traceId=qc-20260620-500 NullPointerException at PricePolicyService', 'P1', '林悦', '销售运营', 'IN_PROGRESS', '系统故障', 92, NULL, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 50 MINUTE),
  ('DEMO-0006', '新员工无法访问财务共享盘', '新员工入职后访问财务共享盘提示 403，主管已完成审批。', 'corp-file-service', '403 Forbidden user not in finance-share-rw group', 'P3', '周明', '财务部', 'PENDING_PROCESS', '权限问题', 86, NULL, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 35 MINUTE),
  ('DEMO-0007', '部署后缺少 PAYMENT_GATEWAY_URL 环境变量', 'UAT 环境部署 payment-service 后健康检查失败，日志显示缺少支付网关 URL。', 'payment-service', 'IllegalStateException: Missing required environment variable PAYMENT_GATEWAY_URL', 'P2', '赵琳', '发布工程', 'RESOLVED', '系统故障', 89, '已由发布同学补齐 UAT 环境变量并完成人工验证。', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 20 MINUTE),
  ('DEMO-0008', '多团队重复咨询采购系统审批流程', '近两周多个部门反复咨询采购系统审批链路和材料要求，建议沉淀标准 FAQ。', 'procurement-flow', 'no runtime error; repeated process consultation', 'P3', '王可', '业务支持', 'KNOWLEDGE_BASED', '流程咨询', 82, '已整理采购审批材料要求和流程节点，发布为知识库条目。', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 5 MINUTE);

INSERT INTO ticket_ai_analysis
  (ticket_id, classification, classification_reason, confidence, confirmation_state, matched_knowledge_nos, troubleshooting_steps, reply_suggestion, risk_notes, source_type, created_at, updated_at)
SELECT id, '系统故障', '本地规则识别到 Java、端口、BindException、8080 等关键词，并命中端口占用知识库；结论等待人工确认。', 91, '待人工确认', '["KB-JAVA-PORT"]',
  '["确认 8080 端口监听进程和启动时间。","核对是否存在旧实例未退出或重复启动。","由值班人员确认释放端口或调整 server.port。"]',
  '已识别为 Java 服务端口占用导致启动失败，请支持人员确认占用进程后再执行释放或配置调整。',
  '["不得自动 kill 生产进程。","端口调整需同步调用方和部署配置。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0001';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '系统故障', '本地规则识别到 Spring Boot、BeanCreationException、bean 创建失败等关键词，并命中启动失败处理清单。', 88, '待人工确认', '["KB-SPRING-BEAN"]',
  '["定位异常 bean 和缺失配置项。","检查 profile、配置中心和构造器依赖。","由研发确认补配置或回滚版本。"]',
  '建议先核对 settlementStrategy 相关配置和 profile，确认后再补齐配置或回滚。',
  '["配置变更需人工确认。","回滚会影响当前发布窗口。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0002';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '系统故障', '本地规则识别到 Redis、连接失败、timeout 等关键词，并命中 Redis 连接池排查知识。', 90, '待人工确认', '["KB-REDIS-CONN"]',
  '["检查 Redis 地址、密码和网络 ACL。","查看连接池使用率和超时配置。","确认是否需要临时扩容或切换备用节点。"]',
  '初步判断为 Redis 连接异常，请值班人员确认网络和连接池状态后处理。',
  '["不能自动修改 Redis 生产参数。","会话链路影响面需要人工评估。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0003';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '数据问题', '本地规则识别到 MySQL、慢查询、SQL、执行计划等关键词，并命中慢查询处理流程。', 84, '待人工确认', '["KB-MYSQL-SLOW"]',
  '["收集慢查询 SQL 和执行计划。","核对 renewal_status、updated_at 索引命中。","由 DBA 评估新增索引或 SQL 改写。"]',
  '建议先由 DBA 查看执行计划和索引命中情况，再确认优化方案。',
  '["新增索引会影响写入性能。","SQL 改写需回归核心筛选场景。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0004';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '系统故障', '本地规则识别到接口 500、traceId、NullPointerException 等关键词，并命中接口 500 排查手册。', 92, '待人工确认', '["KB-API-500"]',
  '["使用 traceId 关联网关和应用日志。","确认 PricePolicyService 空指针输入来源。","人工确认修复、降级或回滚策略。"]',
  '报价接口 500 已进入人工排查，请保留失败报价单和 traceId，处理动作确认后同步。',
  '["报价链路影响销售收入。","降级或回滚必须人工确认。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0005';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '权限问题', '本地规则识别到 403、权限、共享盘、审批等关键词，并命中 RBAC 核验流程。', 86, '待人工确认', '["KB-IAM-ROLE"]',
  '["核验审批记录和直属主管确认。","检查用户是否在 finance-share-rw 组。","人工确认后执行授权并记录审计。"]',
  '该问题疑似权限组未同步，需人工核验审批记录后处理。',
  '["涉及财务目录，不能自动授权。","必须保留审批与审计记录。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0006';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '系统故障', '本地规则识别到部署、环境变量、Missing required environment variable 等关键词，并命中部署配置知识。', 89, '待人工确认', '["KB-DEPLOY-ENV"]',
  '["比对 UAT 与生产环境变量差异。","检查 ConfigMap、Secret 和启动参数。","人工确认补齐变量并重启验证。"]',
  '建议由发布负责人确认 PAYMENT_GATEWAY_URL 来源后补齐 UAT 配置。',
  '["部署配置变更需人工确认。","避免把生产密钥误写入 UAT。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0007';

INSERT INTO ticket_ai_analysis
SELECT NULL, id, '流程咨询', '本地规则识别到重复咨询、流程、FAQ、知识库等关键词，并命中知识沉淀规范。', 82, '待人工确认', '["KB-FAQ-KNOWLEDGE"]',
  '["归纳重复问题和标准答复。","生成知识草稿并指定知识负责人审核。","人工确认后发布并关联原工单。"]',
  '建议将采购审批流程整理为知识库 FAQ，由知识负责人确认后发布。',
  '["知识发布前必须人工审核。","流程条款需要业务负责人确认。"]', 'RULE_TEMPLATE', NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0008';

INSERT INTO knowledge_article
  (article_no, title, category, keywords, content, owner, status, source_ticket_id, last_verified_at, created_at, updated_at)
SELECT 'KB-DRAFT-DEMO-0008', '采购系统审批流程 FAQ', '流程咨询', '采购,审批,FAQ,流程,材料',
  '来源 DEMO-0008。该条目汇总采购审批材料、审批节点和常见退回原因，已由知识负责人人工确认。',
  'Knowledge Reviewer', 'PUBLISHED', id, NOW(), NOW(), NOW()
FROM support_ticket WHERE ticket_no = 'DEMO-0008';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, actor, note, occurred_at)
SELECT id, NULL, 'PENDING_CLASSIFICATION', 'System', '工单提交，等待本地规则分类。', created_at FROM support_ticket WHERE ticket_no LIKE 'DEMO-%';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, actor, note, occurred_at)
SELECT id, 'PENDING_CLASSIFICATION', 'PENDING_PROCESS', 'Rule Engine', '已生成分类、知识命中和建议草案，等待人工确认。', created_at + INTERVAL 5 MINUTE FROM support_ticket WHERE ticket_no LIKE 'DEMO-%';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, actor, note, occurred_at)
SELECT id, 'PENDING_PROCESS', 'IN_PROGRESS', 'Support Desk', '支持人员已人工接手处理。', created_at + INTERVAL 20 MINUTE FROM support_ticket WHERE status IN ('IN_PROGRESS', 'RESOLVED', 'KNOWLEDGE_BASED') AND ticket_no LIKE 'DEMO-%';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, actor, note, occurred_at)
SELECT id, 'IN_PROGRESS', 'RESOLVED', 'Support Desk', '问题已由人工确认解决，等待知识沉淀。', updated_at - INTERVAL 10 MINUTE FROM support_ticket WHERE status IN ('RESOLVED', 'KNOWLEDGE_BASED') AND ticket_no LIKE 'DEMO-%';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, actor, note, occurred_at)
SELECT id, 'RESOLVED', 'KNOWLEDGE_BASED', 'Knowledge Reviewer', '知识草稿已人工确认并发布。', updated_at FROM support_ticket WHERE status = 'KNOWLEDGE_BASED' AND ticket_no LIKE 'DEMO-%';

INSERT INTO generation_record (business_type, business_id, source_type, input_summary, output_summary, latency_ms, status, created_at)
SELECT 'DEMO_SEED', id, 'RULE_ENGINE', title, CONCAT('category=', category, ', confidence=', ai_confidence), 12, 'SUCCESS', created_at + INTERVAL 1 MINUTE
FROM support_ticket WHERE ticket_no LIKE 'DEMO-%';
