package com.enterpriseai.ticketcopilot.eval;

import java.time.LocalDateTime;
import java.util.List;

import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import org.springframework.jdbc.core.JdbcTemplate;

final class RetrievalEvaluationCorpusFixture {

    private static final List<ArticleFixture> CORPUS = List.of(
        new ArticleFixture("KB-ACCOUNT-001", "账号登录失败与 MFA 重置处理流程", "账号,登录,密码,MFA,SSO,认证,锁定"),
        new ArticleFixture("KB-IAM-002", "权限申请与最小权限核验清单", "权限,授权,角色,RBAC,审批,访问,共享盘,报表"),
        new ArticleFixture("KB-OPS-003", "系统故障超时与 5xx 排查手册", "502,500,timeout,超时,异常,报错,服务,网关,error"),
        new ArticleFixture("KB-DATA-004", "数据同步异常与报表口径排查", "数据,同步,报表,不一致,缺失,导入,导出,指标"),
        new ArticleFixture("KB-PROCESS-005", "内部流程咨询分派与知识沉淀规范", "流程,如何,怎么,申请,规范,指引,步骤,咨询"),
        new ArticleFixture("KB-JAVA-PORT", "Java 服务端口占用排查手册", "Java,端口,占用,BindException,Tomcat,8080"),
        new ArticleFixture("KB-SPRING-BEAN", "Spring Boot BeanCreationException 处理清单", "Spring Boot,BeanCreationException,bean,依赖注入,启动失败"),
        new ArticleFixture("KB-REDIS-CONN", "Redis 连接失败与连接池排查", "Redis,连接失败,timeout,Lettuce,Jedis,连接池"),
        new ArticleFixture("KB-MYSQL-SLOW", "MySQL 慢查询定位与索引评估流程", "MySQL,慢查询,索引,SQL,执行计划,锁等待"),
        new ArticleFixture("KB-API-500", "接口 500 错误分层排查手册", "接口,500,error,traceId,异常,网关"),
        new ArticleFixture("KB-IAM-ROLE", "权限配置问题与 RBAC 核验流程", "权限,RBAC,角色,403,Forbidden,访问控制,审批"),
        new ArticleFixture(
            "KB-DEPLOY-ENV",
            "部署环境变量缺失排查清单",
            "部署,环境变量,配置,profile,container,Kubernetes,ConfigMap,Secret,PAYMENT_GATEWAY_URL,启动参数"
        ),
        new ArticleFixture("KB-FAQ-KNOWLEDGE", "重复咨询转知识库沉淀规范", "重复咨询,知识库,FAQ,流程,沉淀,审核"),
        new ArticleFixture("KB-DRAFT-DEMO-0008", "采购系统审批流程 FAQ", "采购,审批,FAQ,流程,材料")
    );

    private RetrievalEvaluationCorpusFixture() {
    }

    static void seedV2(JdbcTemplate jdbcTemplate, KnowledgeArticleMapper knowledgeArticleMapper) {
        jdbcTemplate.update("DELETE FROM knowledge_article");
        LocalDateTime now = LocalDateTime.of(2026, 10, 22, 0, 0);
        for (ArticleFixture fixture : CORPUS) {
            KnowledgeArticle article = new KnowledgeArticle();
            article.setArticleNo(fixture.articleNo());
            article.setTitle(fixture.title());
            article.setCategory("integration-test");
            article.setKeywords(fixture.keywords());
            article.setContent("Keyword retrieval integration fixture");
            article.setOwner("test");
            article.setStatus("PUBLISHED");
            article.setCreatedAt(now);
            article.setUpdatedAt(now);
            article.setLastVerifiedAt(now);
            if (knowledgeArticleMapper.insert(article) != 1) {
                throw new AssertionError("Failed to seed retrieval article " + fixture.articleNo());
            }
        }
    }

    static void seedV3(JdbcTemplate jdbcTemplate, KnowledgeArticleMapper knowledgeArticleMapper) {
        seedV2(jdbcTemplate, knowledgeArticleMapper);
        jdbcTemplate.update(
            "UPDATE knowledge_article SET keywords = ? WHERE article_no = ?",
            "采购,流程,FAQ,材料",
            "KB-DRAFT-DEMO-0008"
        );
    }

    private record ArticleFixture(String articleNo, String title, String keywords) {
    }
}
