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

        assertThat(ticketId).startsWith("TCK-");
        assertThat(created.path("title").asText()).isEqualTo("payment-service returns 500");
        assertThat(created.path("status").asText()).isEqualTo("PENDING_PROCESS");
        assertThat(created.path("timeline")).hasSize(2);

        mockMvc.perform(get("/api/tickets/{id}", ticketId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ticketId))
            .andExpect(jsonPath("$.status").value("PENDING_PROCESS"))
            .andExpect(jsonPath("$.systemContext.application").value("payment-service"));

        mockMvc.perform(get("/api/tickets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ticketId))
            .andExpect(jsonPath("$[0].status").value("PENDING_PROCESS"));
    }

    @Test
    void createdTicketExposesRuleEngineAnalysisWithoutExternalLlm() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();

        mockMvc.perform(get("/api/tickets/{id}/ai-analysis", ticketId))
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                    "status", "IN_PROGRESS",
                    "actor", "Support Desk",
                    "note", "人工确认接手处理。",
                    "resolvedSummary", ""
                ))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId))
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
            .andExpect(jsonPath("$.aiAnalysis.provider").value("local-rule fallback"))
            .andExpect(jsonPath("$.aiAnalysis.model").value("N/A (no LLM)"))
            .andExpect(jsonPath("$.generationRecords.length()").value(3))
            .andExpect(jsonPath("$.generationRecords[0].businessType").value("CLASSIFICATION"))
            .andExpect(jsonPath("$.generationRecords[0].provider").value("local-rule fallback"))
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
    void statusFlowAndKnowledgeDraftPersistThroughH2Database() throws Exception {
        JsonNode created = createTicket();
        String ticketId = created.path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/status", ticketId)
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

        mockMvc.perform(post("/api/tickets/knowledge/{articleNo}/confirm", articleNo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.articleNo").value(articleNo))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/tickets/{id}", ticketId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("KNOWLEDGE_BASED"))
            .andExpect(jsonPath("$.knowledgeDraft.articleNo").value(articleNo))
            .andExpect(jsonPath("$.knowledgeDraft.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.timeline.length()", greaterThanOrEqualTo(4)));
    }

    private JsonNode createTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
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

    private String json(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
