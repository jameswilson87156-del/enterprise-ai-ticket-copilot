package com.enterpriseai.ticketcopilot;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.enterpriseai.ticketcopilot.entity.CopilotResult;
import com.enterpriseai.ticketcopilot.entity.CopilotResultCitation;
import com.enterpriseai.ticketcopilot.entity.CopilotRun;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.entity.ReviewRecord;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.CopilotRunMapper;
import com.enterpriseai.ticketcopilot.mapper.CopilotResultCitationMapper;
import com.enterpriseai.ticketcopilot.mapper.CopilotResultMapper;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import com.enterpriseai.ticketcopilot.mapper.RetrievalHitMapper;
import com.enterpriseai.ticketcopilot.mapper.ReviewRecordMapper;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "ticket.ai.provider=openai-compatible",
    "ticket.ai.protocol=chat-completions",
    "ticket.ai.base-url=",
    "ticket.ai.model=test-model",
    "ticket.ai.api-key=",
    "ticket.ai.fallback-to-local=true"
})
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupportTicketMapper supportTicketMapper;

    @Autowired
    private CopilotRunMapper copilotRunMapper;

    @Autowired
    private CopilotResultMapper copilotResultMapper;

    @Autowired
    private CopilotResultCitationMapper copilotResultCitationMapper;

    @Autowired
    private RetrievalHitMapper retrievalHitMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Test
    void createTicketPersistsThroughHttpAndCanBeQueried() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();
        String token = token("agent", "agent123");

        assertThat(ticketId).startsWith("TCK-");
        assertThat(created.path("title").asText()).isEqualTo("payment-service returns 500");
        assertThat(created.path("status").asText()).isEqualTo("PENDING_PROCESS");
        assertThat(created.path("timeline")).hasSize(2);

        mockMvc.perform(get("/api/tickets/{id}", ticketId).header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ticketId))
            .andExpect(jsonPath("$.status").value("PENDING_PROCESS"))
            .andExpect(jsonPath("$.systemContext.application").value("payment-service"));

        mockMvc.perform(get("/api/tickets").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ticketId))
            .andExpect(jsonPath("$[0].status").value("PENDING_PROCESS"));
    }

    @Test
    void createdTicketExposesRuleEngineAnalysisWithoutExternalLlm() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();

        mockMvc.perform(get("/api/tickets/{id}/ai-analysis", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketId").value(ticketId))
            .andExpect(jsonPath("$.classification").value("系统故障"))
            .andExpect(jsonPath("$.confidence", greaterThanOrEqualTo(80)))
            .andExpect(jsonPath("$.confirmationState").value("待人工确认"))
            .andExpect(jsonPath("$.knowledgeHits[0].id").value("KB-OPS-003"))
            .andExpect(jsonPath("$.troubleshootingSteps.length()", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.riskNotes.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void traceEvidenceExposesGenerationRecordsRagReferencesAndHumanReviewBoundary() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                .header("Authorization", token("agent", "agent123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "status", "IN_PROGRESS",
                    "actor", "Support Desk",
                    "note", "人工确认接手处理。",
                    "resolvedSummary", ""
                ))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketId").value(ticketId))
            .andExpect(jsonPath("$.runId").value("RUN-" + ticketId))
            .andExpect(jsonPath("$.traceId").value("TRACE-" + ticketId))
            .andExpect(jsonPath("$.traceMode").value("derived-from-ticket-records; no distributed trace/span runtime"))
            .andExpect(jsonPath("$.currentStep").value("HUMAN_PROCESSING"))
            .andExpect(jsonPath("$.reviewRequired").value(true))
            .andExpect(jsonPath("$.totalLatency", greaterThanOrEqualTo(0)))
            .andExpect(jsonPath("$.aiAnalysis.analysisId").exists())
            .andExpect(jsonPath("$.aiAnalysis.recordId").exists())
            .andExpect(jsonPath("$.aiAnalysis.provider").value("local-rule"))
            .andExpect(jsonPath("$.aiAnalysis.model").value("N/A (no LLM)"))
            .andExpect(jsonPath("$.generationRecords.length()").value(3))
            .andExpect(jsonPath("$.generationRecords[0].businessType").value("CLASSIFICATION"))
            .andExpect(jsonPath("$.generationRecords[0].provider").value("local-rule"))
            .andExpect(jsonPath("$.ragReferences[0].knowledgeTitle").exists())
            .andExpect(jsonPath("$.ragReferences[0].sourcePath").value("knowledge_article/KB-OPS-003"))
            .andExpect(jsonPath("$.ragReferences[0].usedInDraft").value(true))
            .andExpect(jsonPath("$.ragReferences[0].linkedRunId").value("RUN-" + ticketId))
            .andExpect(jsonPath("$.statusHistory.length()", greaterThanOrEqualTo(3)))
            .andExpect(jsonPath("$.humanReview.reviewStatus").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.humanReview.reviewer").value("Support Desk"))
            .andExpect(jsonPath("$.humanReview.decision").value("TAKE_OWNERSHIP"));
    }

    @Test
    void runCopilotFallsBackWithoutApiKeyAndTraceShowsProviderMetadata() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId)
                .header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"));

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("reviewer", "reviewer123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentStep").value("HUMAN_REVIEW_REQUIRED"))
            .andExpect(jsonPath("$.aiAnalysis.providerName").value("openai-compatible"))
            .andExpect(jsonPath("$.aiAnalysis.modelName").value("test-model"))
            .andExpect(jsonPath("$.aiAnalysis.fallbackUsed").value(true))
            .andExpect(jsonPath("$.aiAnalysis.fallbackReason").value("API_KEY_MISSING"))
            .andExpect(jsonPath("$.aiAnalysis.status").value("FALLBACK"))
            .andExpect(jsonPath("$.generationRecords[5].businessType").value("AI_PROVIDER"))
            .andExpect(jsonPath("$.generationRecords[5].providerName").value("openai-compatible"))
            .andExpect(jsonPath("$.generationRecords[5].fallbackReason").value("API_KEY_MISSING"));
    }

    @Test
    void runCopilotPersistsImmutableRunAndRetrievalSnapshotForTraceReplay() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();
        String agentToken = token("agent", "agent123");

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk());

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = latestRun(ticket.getId());
        assertThat(run.getRunId()).startsWith("RUN-" + ticketId + "-");
        assertThat(run.getTraceId()).startsWith("TRACE-" + ticketId + "-");
        assertThat(run.getRequestedProvider()).isEqualTo("openai-compatible");
        assertThat(run.getActualProvider()).isEqualTo("local-rule");
        assertThat(run.getRunStatus()).isEqualTo("SUCCESS_WITH_FALLBACK");
        assertThat(run.getErrorCategory()).isEqualTo("CONFIGURATION_ERROR");
        assertThat(run.getAnalysisId()).isNotNull();
        assertThat(run.getGenerationRecordId()).isNotNull();

        List<RetrievalHit> hits = retrievalHitMapper.selectList(new LambdaQueryWrapper<RetrievalHit>()
            .eq(RetrievalHit::getRunId, run.getRunId())
            .orderByAsc(RetrievalHit::getRankOrder));
        assertThat(hits).hasSize(1);
        String originalTitleSnapshot = hits.get(0).getKnowledgeTitleSnapshot();

        knowledgeArticleMapper.update(null, new LambdaUpdateWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getArticleNo, hits.get(0).getKnowledgeArticleNo())
            .set(KnowledgeArticle::getTitle, "mutated knowledge title should not appear in immutable trace"));

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evidenceSource").value("IMMUTABLE_RUN"))
            .andExpect(jsonPath("$.traceMode").value("immutable-copilot-run-trace"))
            .andExpect(jsonPath("$.runId").value(run.getRunId()))
            .andExpect(jsonPath("$.traceId").value(run.getTraceId()))
            .andExpect(jsonPath("$.copilotRun.runStatus").value("SUCCESS_WITH_FALLBACK"))
            .andExpect(jsonPath("$.copilotRun.actualProvider").value("local-rule"))
            .andExpect(jsonPath("$.copilotRun.errorCategory").value("CONFIGURATION_ERROR"))
            .andExpect(jsonPath("$.ragReferences[0].knowledgeTitle").value(originalTitleSnapshot))
            .andExpect(jsonPath("$.ragReferences[0].knowledgeTitle").value(not("mutated knowledge title should not appear in immutable trace")))
            .andExpect(jsonPath("$.ragReferences[0].linkedRunId").value(run.getRunId()));
    }

    @Test
    void runCopilotPersistsStructuredResultValidatedCitationsAndTraceReplayDoesNotRequeryKnowledge() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();
        String agentToken = token("agent", "agent123");

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"));

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = latestRun(ticket.getId());
        CopilotResult result = copilotResultMapper.selectOne(new LambdaQueryWrapper<CopilotResult>()
            .eq(CopilotResult::getRunId, run.getRunId()));
        assertThat(result).isNotNull();
        assertThat(result.getAbstained()).isFalse();
        assertThat(result.getAbstentionReasonCode()).isEqualTo("NONE");
        assertThat(result.getOutputValidationStatus()).isEqualTo("VALID");
        assertThat(result.getCitationValidationStatus()).isEqualTo("VALID");
        assertThat(result.getValidCitationCount()).isEqualTo(1);
        assertThat(result.getFinalHumanReviewRequired()).isTrue();

        List<CopilotResultCitation> citations = copilotResultCitationMapper.selectList(new LambdaQueryWrapper<CopilotResultCitation>()
            .eq(CopilotResultCitation::getResultId, result.getId()));
        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).getKnowledgeArticleId()).isEqualTo("KB-OPS-003");

        knowledgeArticleMapper.update(null, new LambdaUpdateWrapper<KnowledgeArticle>()
            .eq(KnowledgeArticle::getArticleNo, "KB-OPS-003")
            .set(KnowledgeArticle::getTitle, "mutated title should not affect validated citation"));

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.structuredOutput.abstained").value(false))
            .andExpect(jsonPath("$.structuredOutput.citationValidationStatus").value("VALID"))
            .andExpect(jsonPath("$.structuredOutput.outputValidationStatus").value("VALID"))
            .andExpect(jsonPath("$.validatedCitations[0].knowledgeArticleId").value("KB-OPS-003"))
            .andExpect(jsonPath("$.validatedCitations[0].knowledgeTitle").value(not("mutated title should not affect validated citation")))
            .andExpect(jsonPath("$.copilotRun.structuredResultId").value(result.getId()));
    }

    @Test
    void runCopilotAbstainsAndSkipsProviderWhenNoRetrievalEvidence() throws Exception {
        String agentToken = token("agent", "agent123");
        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "会议室位置咨询",
                    "description", "请告知会议室在哪里",
                    "systemName", "office-faq",
                    "errorLog", "",
                    "urgency", "P3",
                    "requester", "测试员工",
                    "requesterDepartment", "综合部"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        String ticketId = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"));

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = latestRun(ticket.getId());
        CopilotResult structured = copilotResultMapper.selectOne(new LambdaQueryWrapper<CopilotResult>()
            .eq(CopilotResult::getRunId, run.getRunId()));

        assertThat(run.getActualProvider()).isEqualTo("NONE");
        assertThat(run.getRetrievalHitCount()).isZero();
        assertThat(structured.getAbstained()).isTrue();
        assertThat(structured.getAbstentionReasonCode()).isEqualTo("NO_RETRIEVAL_EVIDENCE");
        assertThat(structured.getCitationValidationStatus()).isEqualTo("NO_RETRIEVAL_EVIDENCE");
        assertThat(copilotResultCitationMapper.selectList(new LambdaQueryWrapper<CopilotResultCitation>()
            .eq(CopilotResultCitation::getResultId, structured.getId()))).isEmpty();
    }

    @Test
    void repeatedCopilotRunsCreateDistinctImmutableRunAndTraceIds() throws Exception {
        String ticketId = createTicket().path("id").asText();
        String agentToken = token("agent", "agent123");

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk());

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        List<CopilotRun> runs = copilotRunMapper.selectList(new LambdaQueryWrapper<CopilotRun>()
            .eq(CopilotRun::getTicketId, ticket.getId())
            .orderByAsc(CopilotRun::getStartedAt));

        assertThat(runs).hasSize(2);
        assertThat(runs.get(0).getRunId()).isNotEqualTo(runs.get(1).getRunId());
        assertThat(runs.get(0).getTraceId()).isNotEqualTo(runs.get(1).getTraceId());
    }

    @Test
    void reviewRecordLinksHumanDecisionToImmutableRun() throws Exception {
        String reviewerToken = token("reviewer", "reviewer123");
        String agentToken = token("agent", "agent123");
        String ticketId = createTicket().path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk());
        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = latestRun(ticket.getId());

        mockMvc.perform(post("/api/tickets/{id}/review/approve", ticketId)
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "human approval linked to immutable run"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));

        List<ReviewRecord> records = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
            .eq(ReviewRecord::getRunId, run.getRunId()));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getDecision()).isEqualTo("APPROVED_RESOLUTION");
        assertThat(records.get(0).getPreviousStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(records.get(0).getNewStatus()).isEqualTo("RESOLVED");

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId).header("Authorization", reviewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewRecords[0].runId").value(run.getRunId()))
            .andExpect(jsonPath("$.reviewRecords[0].decision").value("APPROVED_RESOLUTION"))
            .andExpect(jsonPath("$.humanReview.decision").value("APPROVED_RESOLUTION"));
    }

    @Test
    void humanReviewEndpointsPersistApproveRequestChangesAndRejectDecisions() throws Exception {
        String reviewerToken = token("reviewer", "reviewer123");
        String agentToken = token("agent", "agent123");

        String approveTicket = createTicket().path("id").asText();
        mockMvc.perform(post("/api/tickets/{id}/run-copilot", approveTicket).header("Authorization", agentToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets/{id}/review/approve", approveTicket)
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "人工审核通过，允许进入解决态。"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"));
        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", approveTicket).header("Authorization", reviewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.humanReview.reviewStatus").value("COMPLETED"))
            .andExpect(jsonPath("$.humanReview.decision").value("APPROVED_RESOLUTION"));

        String changesTicket = createTicket().path("id").asText();
        mockMvc.perform(post("/api/tickets/{id}/run-copilot", changesTicket).header("Authorization", agentToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets/{id}/review/request-changes", changesTicket)
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "请补充业务影响范围。"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"));

        String rejectTicket = createTicket().path("id").asText();
        mockMvc.perform(post("/api/tickets/{id}/run-copilot", rejectTicket).header("Authorization", agentToken))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tickets/{id}/review/reject", rejectTicket)
                .header("Authorization", reviewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "建议与现象不符，拒绝当前草稿。"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rbacBlocksViewerFromCopilotAndReviewOperations() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();
        String viewerToken = token("viewer", "viewer123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId)
                .header("Authorization", viewerToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/tickets/{id}/review/approve", ticketId)
                .header("Authorization", viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("comment", "viewer should not approve"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void statusFlowAndKnowledgeDraftPersistThroughH2Database() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();
        String token = token("agent", "agent123");

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "status", "RESOLVED",
                    "actor", "Support Desk",
                    "note", "人工确认处理完成。",
                    "resolvedSummary", "确认 payment-service 配置已恢复。"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESOLVED"))
            .andExpect(jsonPath("$.timeline.length()", greaterThanOrEqualTo(3)));

        MvcResult draftResult = mockMvc.perform(post("/api/tickets/{id}/knowledge-draft", ticketId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "【草稿】payment-service 500 排查",
                    "content", "来源工单：" + ticketId + "\n处理摘要：人工确认配置恢复。",
                    "owner", "SRE Enablement",
                    "confirm", false
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.articleNo").exists())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();

        String articleNo = readJson(draftResult).path("articleNo").asText();

        mockMvc.perform(post("/api/tickets/knowledge/{articleNo}/confirm", articleNo).header("Authorization", token("reviewer", "reviewer123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.articleNo").value(articleNo))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/tickets/{id}", ticketId).header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("KNOWLEDGE_BASED"))
            .andExpect(jsonPath("$.knowledgeDraft.articleNo").value(articleNo))
            .andExpect(jsonPath("$.knowledgeDraft.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.timeline.length()", greaterThanOrEqualTo(4)));
    }

    private CopilotRun latestRun(Long ticketId) {
        return copilotRunMapper.selectOne(new LambdaQueryWrapper<CopilotRun>()
            .eq(CopilotRun::getTicketId, ticketId)
            .orderByDesc(CopilotRun::getStartedAt)
            .orderByDesc(CopilotRun::getRunId)
            .last("limit 1"));
    }

    private JsonNode createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", token("agent", "agent123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "title", "payment-service returns 500",
                    "description", "Payment service fails after release and returns HTTP 500.",
                    "systemName", "payment-service",
                    "errorLog", "HTTP 500 error timeout",
                    "urgency", "P1",
                    "requester", "周冉",
                    "requesterDepartment", "支付研发"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();
        return readJson(result);
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "username", username,
                    "password", password
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + readJson(result).path("token").asText();
    }

    private String json(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
