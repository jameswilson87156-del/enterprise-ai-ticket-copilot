package com.enterpriseai.ticketcopilot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.CopilotResult;
import com.enterpriseai.ticketcopilot.entity.CopilotResultCitation;
import com.enterpriseai.ticketcopilot.entity.CopilotRun;
import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.entity.TicketAiAnalysisEntity;
import com.enterpriseai.ticketcopilot.mapper.CopilotResultCitationMapper;
import com.enterpriseai.ticketcopilot.mapper.CopilotResultMapper;
import com.enterpriseai.ticketcopilot.mapper.CopilotRunMapper;
import com.enterpriseai.ticketcopilot.mapper.RetrievalHitMapper;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.mapper.TicketAiAnalysisMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowStructuredHardeningIntegrationTest {

    private static final ObjectMapper STATIC_OBJECT_MAPPER = new ObjectMapper();
    private static final AtomicReference<String> PROVIDER_CONTENT = new AtomicReference<>("{}");
    private static final AtomicInteger PROVIDER_STATUS = new AtomicInteger(200);
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    private static final HttpServer SERVER = startServer();

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SupportTicketMapper supportTicketMapper;
    @Autowired private TicketAiAnalysisMapper analysisMapper;
    @Autowired private CopilotRunMapper copilotRunMapper;
    @Autowired private CopilotResultMapper copilotResultMapper;
    @Autowired private CopilotResultCitationMapper copilotResultCitationMapper;
    @Autowired private RetrievalHitMapper retrievalHitMapper;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("ticket.ai.provider", () -> "openai-compatible");
        registry.add("ticket.ai.protocol", () -> "chat-completions");
        registry.add("ticket.ai.base-url", () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("ticket.ai.model", () -> "test-model");
        registry.add("ticket.ai.api-key", () -> "placeholder-present");
        registry.add("ticket.ai.fallback-to-local", () -> "false");
    }

    @BeforeEach
    void resetProvider() {
        REQUEST_COUNT.set(0);
        PROVIDER_STATUS.set(200);
        PROVIDER_CONTENT.set(validProviderOutput(
            "Use the cited run snapshot.",
            "KB-OPS-003",
            "model claim",
            "model excerpt"
        ));
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void providerDeclaredAbstentionUsesSystemAnswerAndDoesNotPersistProviderAdvice() throws Exception {
        PROVIDER_CONTENT.set("{"
            + "\"answer\":\"Restart payment-service, roll back deployment, and expand capacity immediately.\","
            + "\"citations\":[],"
            + "\"riskLevel\":\"LOW\","
            + "\"humanReviewRequired\":true,"
            + "\"missingInformation\":[],"
            + "\"abstained\":true,"
            + "\"abstentionReasonCode\":\"UNSUPPORTED_PROVIDER\""
            + "}");
        String ticketId = createTicket("payment-service returns 500", "Payment service fails after release and returns HTTP 500.").path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotResult result = latestResult(ticket.getId());
        TicketAiAnalysisEntity analysis = latestAnalysis(ticket.getId());
        String traceJson = mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("reviewer", "reviewer123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(result.getAbstained()).isTrue();
        assertThat(result.getAbstentionReasonCode()).isEqualTo("OUTPUT_POLICY_REJECTED");
        assertThat(result.getFinalHumanReviewRequired()).isTrue();
        assertThat(result.getAnswer()).doesNotContain("Restart payment-service", "roll back", "expand capacity");
        assertThat(analysis.getReplySuggestion()).doesNotContain("Restart payment-service", "roll back", "expand capacity");
        assertThat(traceJson).doesNotContain("Restart payment-service", "roll back", "expand capacity");
    }

    @Test
    void invalidProviderCitationPreservesModelReviewRecommendationSeparateFromFinalReview() throws Exception {
        PROVIDER_CONTENT.set(validProviderOutput(
            "Use a citation that is not in this run.",
            "KB-FAKE",
            "model claim",
            "model excerpt"
        ));
        String ticketId = createTicket("payment-service returns 500", "Payment service fails after release and returns HTTP 500.").path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotResult result = latestResult(ticket.getId());

        assertThat(result.getModelHumanReviewRequired()).isFalse();
        assertThat(result.getFinalHumanReviewRequired()).isTrue();
        assertThat(result.getCitationValidationStatus()).isEqualTo("INVALID_CITATION");
        assertThat(result.getAbstentionReasonCode()).isEqualTo("INVALID_CITATION");
    }

    @Test
    void validatedCitationEvidenceUsesRetrievalSnapshotAndDropsModelSupportedClaim() throws Exception {
        PROVIDER_CONTENT.set(validProviderOutput(
            "Use the cited run snapshot.",
            "KB-OPS-003",
            "fabricated supported claim",
            "fabricated evidence excerpt"
        ));
        String ticketId = createTicket("payment-service returns 500", "Payment service fails after release and returns HTTP 500.").path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotResult result = latestResult(ticket.getId());
        List<CopilotResultCitation> citations = copilotResultCitationMapper.selectList(new LambdaQueryWrapper<CopilotResultCitation>()
            .eq(CopilotResultCitation::getResultId, result.getId()));
        List<RetrievalHit> hits = retrievalHitMapper.selectList(new LambdaQueryWrapper<RetrievalHit>()
            .eq(RetrievalHit::getRunId, result.getRunId()));
        String traceJson = mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);

        assertThat(citations).hasSize(1);
        assertThat(hits).hasSize(1);
        assertThat(citations.get(0).getEvidenceExcerpt()).isEqualTo(hits.get(0).getExcerptSnapshot());
        assertThat(citations.get(0).getSupportedClaim()).isNull();
        assertThat(traceJson).doesNotContain("fabricated evidence excerpt", "fabricated supported claim");
    }

    @Test
    void noRetrievalEvidenceSkipsProviderAndRemovesActionableTroubleshootingAdvice() throws Exception {
        String ticketId = createTicketWithoutRetrievalEvidence().path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotRun run = latestRun(ticket.getId());
        CopilotResult result = latestResult(ticket.getId());
        TicketAiAnalysisEntity analysis = latestAnalysis(ticket.getId());
        JsonNode trace = objectMapper.readTree(mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));

        assertThat(REQUEST_COUNT).hasValue(0);
        assertThat(run.getActualProvider()).isEqualTo("NONE");
        assertThat(trace.path("structuredOutput").path("modelHumanReviewRequired").isNull()).isTrue();
        assertThat(result.getAbstained()).isTrue();
        assertThat(result.getFinalHumanReviewRequired()).isTrue();
        assertThat(analysis.getTroubleshootingSteps()).doesNotContain("重启", "回滚", "扩容", "修改配置", "restart", "rollback", "scale");
    }

    @Test
    void malformedStructuredOutputPersistsSafeAbstentionAndImmutableReplay() throws Exception {
        PROVIDER_CONTENT.set("{\"answer\":\"missing required structured fields\"}");
        String ticketId = createTicket("payment-service returns 500", "Payment service fails after release and returns HTTP 500.").path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotResult result = latestResult(ticket.getId());
        JsonNode trace = objectMapper.readTree(mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("reviewer", "reviewer123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));

        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(result.getOutputValidationStatus()).isNotEqualTo("VALID");
        assertThat(result.getAbstained()).isTrue();
        assertThat(result.getFinalHumanReviewRequired()).isTrue();
        assertThat(trace.path("evidenceSource").asText()).isEqualTo("IMMUTABLE_RUN");
        assertThat(trace.path("structuredOutput").path("abstained").asBoolean()).isTrue();
        assertThat(REQUEST_COUNT).hasValue(1);
    }

    @Test
    void providerHttpFailurePersistsFailedRunAndReplayDoesNotRetryProvider() throws Exception {
        PROVIDER_STATUS.set(503);
        String ticketId = createTicket("payment-service returns 500", "Payment service fails after release and returns HTTP 500.").path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token("agent", "agent123")))
            .andExpect(status().isOk());

        SupportTicket ticket = ticket(ticketId);
        CopilotRun run = latestRun(ticket.getId());
        assertThat(REQUEST_COUNT).hasValue(1);
        assertThat(run.getRunStatus()).isEqualTo("FAILED");
        assertThat(run.getOutputProduced()).isFalse();

        JsonNode trace = objectMapper.readTree(mockMvc.perform(get("/api/tickets/{id}/trace-evidence", ticketId)
                .header("Authorization", token("reviewer", "reviewer123")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));

        assertThat(trace.path("evidenceSource").asText()).isEqualTo("IMMUTABLE_RUN");
        assertThat(trace.path("copilotRun").path("runStatus").asText()).isEqualTo("FAILED");
        assertThat(trace.path("copilotRun").path("outputProduced").asBoolean()).isFalse();
        assertThat(REQUEST_COUNT).hasValue(1);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/chat/completions", TicketWorkflowStructuredHardeningIntegrationTest::handleProvider);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void handleProvider(HttpExchange exchange) throws IOException {
        REQUEST_COUNT.incrementAndGet();
        int statusCode = PROVIDER_STATUS.get();
        String content = STATIC_OBJECT_MAPPER.writeValueAsString(PROVIDER_CONTENT.get());
        byte[] bytes = statusCode == 200
            ? ("{\"choices\":[{\"message\":{\"content\":" + content + "}}]}").getBytes(StandardCharsets.UTF_8)
            : "{\"error\":\"synthetic stub failure\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }

    private String validProviderOutput(String answer, String citationId, String supportedClaim, String evidenceExcerpt) {
        return "{"
            + "\"answer\":" + jsonString(answer) + ","
            + "\"citations\":[{\"knowledgeArticleId\":" + jsonString(citationId)
            + ",\"supportedClaim\":" + jsonString(supportedClaim)
            + ",\"evidenceExcerpt\":" + jsonString(evidenceExcerpt) + "}],"
            + "\"riskLevel\":\"LOW\","
            + "\"humanReviewRequired\":false,"
            + "\"missingInformation\":[],"
            + "\"abstained\":false,"
            + "\"abstentionReasonCode\":\"NONE\""
            + "}";
    }

    private JsonNode createTicket(String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", token("agent", "agent123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "description", description,
                    "systemName", "payment-service",
                    "errorLog", "HTTP 500 timeout",
                    "urgency", "P1",
                    "requester", "Agent Tester",
                    "requesterDepartment", "QA"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode createTicketWithoutRetrievalEvidence() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", token("agent", "agent123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "meeting room location question",
                    "description", "Where is the meeting room?",
                    "systemName", "office-faq",
                    "errorLog", "",
                    "urgency", "P3",
                    "requester", "Agent Tester",
                    "requesterDepartment", "QA"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("token").asText();
    }

    private SupportTicket ticket(String ticketNo) {
        return supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>().eq(SupportTicket::getTicketNo, ticketNo));
    }

    private CopilotRun latestRun(Long ticketId) {
        return copilotRunMapper.selectOne(new LambdaQueryWrapper<CopilotRun>()
            .eq(CopilotRun::getTicketId, ticketId)
            .orderByDesc(CopilotRun::getStartedAt)
            .last("limit 1"));
    }

    private CopilotResult latestResult(Long ticketId) {
        return copilotResultMapper.selectOne(new LambdaQueryWrapper<CopilotResult>()
            .inSql(CopilotResult::getRunId, "select run_id from copilot_run where ticket_id = " + ticketId)
            .orderByDesc(CopilotResult::getCreatedAt)
            .last("limit 1"));
    }

    private TicketAiAnalysisEntity latestAnalysis(Long ticketId) {
        return analysisMapper.selectOne(new LambdaQueryWrapper<TicketAiAnalysisEntity>()
            .eq(TicketAiAnalysisEntity::getTicketId, ticketId)
            .orderByDesc(TicketAiAnalysisEntity::getCreatedAt)
            .last("limit 1"));
    }

    private String jsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
