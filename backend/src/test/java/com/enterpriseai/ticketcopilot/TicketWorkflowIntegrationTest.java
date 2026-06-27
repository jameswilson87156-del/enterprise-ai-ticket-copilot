package com.enterpriseai.ticketcopilot;

import java.nio.charset.StandardCharsets;
import java.util.Map;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "ticket.ai.provider=openai-compatible")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
            .andExpect(jsonPath("$.aiAnalysis.modelName").value("gpt-4o-mini"))
            .andExpect(jsonPath("$.aiAnalysis.fallbackUsed").value(true))
            .andExpect(jsonPath("$.aiAnalysis.fallbackReason").value("API_KEY_MISSING"))
            .andExpect(jsonPath("$.aiAnalysis.status").value("FALLBACK"))
            .andExpect(jsonPath("$.generationRecords[5].businessType").value("AI_PROVIDER"))
            .andExpect(jsonPath("$.generationRecords[5].providerName").value("openai-compatible"))
            .andExpect(jsonPath("$.generationRecords[5].fallbackReason").value("API_KEY_MISSING"));
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
