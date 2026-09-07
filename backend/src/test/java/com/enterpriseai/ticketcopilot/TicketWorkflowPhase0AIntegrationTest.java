package com.enterpriseai.ticketcopilot;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.contract.TicketStatusContract;
import com.enterpriseai.ticketcopilot.entity.CopilotRun;
import com.enterpriseai.ticketcopilot.entity.GenerationRecord;
import com.enterpriseai.ticketcopilot.entity.KnowledgeArticle;
import com.enterpriseai.ticketcopilot.entity.ReviewRecord;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.entity.TicketAiAnalysisEntity;
import com.enterpriseai.ticketcopilot.entity.TicketStatusHistory;
import com.enterpriseai.ticketcopilot.mapper.CopilotRunMapper;
import com.enterpriseai.ticketcopilot.mapper.GenerationRecordMapper;
import com.enterpriseai.ticketcopilot.mapper.KnowledgeArticleMapper;
import com.enterpriseai.ticketcopilot.mapper.ReviewRecordMapper;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.mapper.TicketAiAnalysisMapper;
import com.enterpriseai.ticketcopilot.mapper.TicketStatusHistoryMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP and persistence-level acceptance tests for Phase 0-A.
 *
 * <p>Every parameterized invocation starts from the H2 schema, so denied
 * requests can be compared with a precise before/after database snapshot.
 * The tests deliberately exercise the Demo Auth roles through the real
 * interceptor and controller rather than setting AuthContext directly.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "ticket.ai.provider=openai-compatible",
    "ticket.ai.protocol=chat-completions",
    "ticket.ai.base-url=",
    "ticket.ai.model=test-model",
    "ticket.ai.api-key=",
    "ticket.ai.fallback-to-local=true"
})
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowPhase0AIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SupportTicketMapper supportTicketMapper;

    @Autowired
    private TicketStatusHistoryMapper statusHistoryMapper;

    @Autowired
    private ReviewRecordMapper reviewRecordMapper;

    @Autowired
    private CopilotRunMapper copilotRunMapper;

    @Autowired
    private KnowledgeArticleMapper knowledgeArticleMapper;

    @Autowired
    private GenerationRecordMapper generationRecordMapper;

    @Autowired
    private TicketAiAnalysisMapper analysisMapper;

    @ParameterizedTest(name = "create ticket authorization: {0}")
    @MethodSource("roles")
    void createTicketEnforcesRoleMatrixAndWritesOnlyWhenAuthorized(RoleCase role) throws Exception {
        DbSnapshot before = snapshot(null);

        MvcResult result = perform(post("/api/tickets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(ticketPayload())), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "AGENT"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(expected == 200 ? readJson(result).path("id").asText() : null);
        if (expected == 200) {
            assertThat(after.tickets()).isEqualTo(before.tickets() + 1);
            assertThat(after.histories()).isEqualTo(before.histories() + 2);
            assertThat(after.analyses()).isEqualTo(before.analyses() + 1);
            assertThat(after.generations()).isEqualTo(before.generations() + 3);
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.PENDING_PROCESS);
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "manual status authorization: {0}")
    @MethodSource("roles")
    void updateStatusEnforcesRoleMatrixAndProtectsHistoryAndReviewSideEffects(RoleCase role) throws Exception {
        String ticketNo = createTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/status", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "status", TicketStatusContract.IN_PROGRESS,
                "actor", "Support Desk",
                "note", "Phase 0-A status matrix test",
                "resolvedSummary", ""
            ))), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "AGENT"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.IN_PROGRESS);
            assertThat(after.histories()).isEqualTo(before.histories() + 1);
            assertThat(after.reviews()).isEqualTo(before.reviews() + 1);
            assertThat(after.tickets()).isEqualTo(before.tickets());
            assertThat(after.runs()).isEqualTo(before.runs());
            assertThat(after.articles()).isEqualTo(before.articles());
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "run Copilot authorization: {0}")
    @MethodSource("roles")
    void runCopilotEnforcesRoleMatrixBeforeCreatingRunOrAnalysis(RoleCase role) throws Exception {
        String ticketNo = createTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/run-copilot", ticketNo), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "AGENT"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.REVIEW_REQUIRED);
            assertThat(after.runs()).isEqualTo(before.runs() + 1);
            assertThat(after.histories()).isEqualTo(before.histories() + 1);
            assertThat(after.analyses()).isEqualTo(before.analyses() + 1);
            assertThat(after.generations()).isEqualTo(before.generations() + 3);
            assertThat(after.reviews()).isEqualTo(before.reviews());
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "review {0} authorization: {1}")
    @MethodSource("reviewMatrix")
    void reviewEndpointsEnforceRoleMatrixAndPersistOnlyAuthorizedDecisions(
        ReviewAction action,
        RoleCase role
    ) throws Exception {
        String ticketNo = createTicketAsAgent();
        runCopilotAsAgent(ticketNo);
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}" + action.path(), ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "Phase 0-A review matrix test"))), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "REVIEWER"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(after.ticketStatus()).isEqualTo(action.targetStatus());
            assertThat(after.histories()).isEqualTo(before.histories() + 1);
            assertThat(after.reviews()).isEqualTo(before.reviews() + 1);
            assertThat(after.runs()).isEqualTo(before.runs());
            assertThat(after.articles()).isEqualTo(before.articles());
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "draft creation authorization: {0}")
    @MethodSource("roles")
    void createKnowledgeDraftEnforcesRoleMatrixAndKeepsResolvedTicketState(RoleCase role) throws Exception {
        String ticketNo = createResolvedTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/knowledge-draft", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "title", "Phase 0-A test draft",
                "content", "A controlled knowledge draft for authorization testing.",
                "owner", "Knowledge Owner",
                "confirm", false
            ))), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "AGENT"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(readJson(result).path("status").asText()).isEqualTo("DRAFT");
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.RESOLVED);
            assertThat(after.articles()).isEqualTo(before.articles() + 1);
            assertThat(after.generations()).isEqualTo(before.generations() + 1);
            assertThat(after.histories()).isEqualTo(before.histories());
            assertThat(after.reviews()).isEqualTo(before.reviews());
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "draft confirmation authorization: {0}")
    @MethodSource("roles")
    void confirmKnowledgeDraftEnforcesRoleMatrixAndPublishesOnlyForReviewerOrAdmin(RoleCase role) throws Exception {
        String ticketNo = createResolvedTicketAsAgent();
        String articleNo = createDraftAsAgent(ticketNo);
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/knowledge/{articleNo}/confirm", articleNo), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "REVIEWER"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(readJson(result).path("status").asText()).isEqualTo("PUBLISHED");
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.KNOWLEDGE_BASED);
            assertThat(after.articleStatus()).isEqualTo("PUBLISHED");
            assertThat(after.articles()).isEqualTo(before.articles());
            assertThat(after.histories()).isEqualTo(before.histories() + 1);
            assertThat(after.reviews()).isEqualTo(before.reviews());
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "inline publish authorization: {0}")
    @MethodSource("roles")
    void confirmFlagUsesPublishPermissionAndDoesNotLetAgentPublishKnowledge(RoleCase role) throws Exception {
        String ticketNo = createResolvedTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/knowledge-draft", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "title", "Phase 0-A inline publish test",
                "content", "A controlled inline publication for authorization testing.",
                "owner", "Knowledge Owner",
                "confirm", true
            ))), role);
        int expected = expectedAuthorization(role, Set.of("ADMIN", "REVIEWER"));
        assertHttpResult(result, expected);

        DbSnapshot after = snapshot(ticketNo);
        if (expected == 200) {
            assertThat(readJson(result).path("status").asText()).isEqualTo("PUBLISHED");
            assertThat(after.ticketStatus()).isEqualTo(TicketStatusContract.KNOWLEDGE_BASED);
            assertThat(after.articleStatus()).isEqualTo("PUBLISHED");
            assertThat(after.articles()).isEqualTo(before.articles() + 1);
            assertThat(after.histories()).isEqualTo(before.histories() + 1);
            assertThat(after.generations()).isEqualTo(before.generations() + 1);
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    @ParameterizedTest(name = "illegal manual target from PENDING_PROCESS: {0}")
    @MethodSource("illegalPendingManualTargets")
    void illegalManualTransitionsReturnConflictWithoutHistoryOrReviewSideEffects(String target) throws Exception {
        String ticketNo = createTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/status", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "status", target,
                "actor", "Support Desk",
                "note", "should be rejected",
                "resolvedSummary", ""
            ))), role("agent"));
        assertHttpResult(result, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
    }

    @Test
    void reviewWithoutCopilotRunReturnsConflictWithoutWritingAnything() throws Exception {
        String ticketNo = createTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/review/approve", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "must have a run"))), role("reviewer"));
        assertHttpResult(result, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
    }

    @ParameterizedTest(name = "terminal repeated review: {0}")
    @MethodSource("terminalReviewActions")
    void terminalRepeatedApproveOrRejectReturnsConflictWithoutFakeHistory(ReviewAction action) throws Exception {
        String ticketNo = createTicketAsAgent();
        runCopilotAsAgent(ticketNo);
        MvcResult first = perform(post("/api/tickets/{id}" + action.path(), ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "first decision"))), role("reviewer"));
        assertHttpResult(first, 200);

        DbSnapshot before = snapshot(ticketNo);
        MvcResult repeated = perform(post("/api/tickets/{id}" + action.path(), ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "repeated decision"))), role("reviewer"));
        assertHttpResult(repeated, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
        assertThat(before.ticketStatus()).isEqualTo(action.targetStatus());
    }

    @Test
    void requestChangesCannotBeRepeatedForTheSameCopilotRun() throws Exception {
        String ticketNo = createTicketAsAgent();
        runCopilotAsAgent(ticketNo);
        String path = "/api/tickets/{id}/review/request-changes";
        MvcResult first = perform(post(path, ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "please add impact scope"))), role("reviewer"));
        assertHttpResult(first, 200);

        DbSnapshot before = snapshot(ticketNo);
        MvcResult repeated = perform(post(path, ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "duplicate request"))), role("reviewer"));
        assertHttpResult(repeated, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
        assertThat(before.ticketStatus()).isEqualTo(TicketStatusContract.REVIEW_REQUIRED);
    }

    @Test
    void knowledgeDraftCannotBeCreatedBeforeResolved() throws Exception {
        String ticketNo = createTicketAsAgent();
        DbSnapshot before = snapshot(ticketNo);

        MvcResult result = perform(post("/api/tickets/{id}/knowledge-draft", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "title", "invalid draft",
                "content", "invalid draft",
                "owner", "Knowledge Owner",
                "confirm", false
            ))), role("agent"));
        assertHttpResult(result, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
    }

    @Test
    void confirmDraftRequiresResolvedSourceAndLeavesDraftUnpublishedOnConflict() throws Exception {
        String ticketNo = createTicketAsAgent();
        SupportTicket ticket = findTicket(ticketNo);
        KnowledgeArticle draft = new KnowledgeArticle();
        draft.setArticleNo("KB-DRAFT-PHASE-0-A");
        draft.setTitle("Invalid source draft");
        draft.setCategory(ticket.getCategory());
        draft.setKeywords("phase-0-a");
        draft.setContent("This draft is intentionally attached to an unresolved ticket.");
        draft.setOwner("Knowledge Owner");
        draft.setStatus("DRAFT");
        draft.setSourceTicketId(ticket.getId());
        draft.setCreatedAt(java.time.LocalDateTime.now());
        draft.setUpdatedAt(java.time.LocalDateTime.now());
        knowledgeArticleMapper.insert(draft);

        DbSnapshot before = snapshot(ticketNo);
        MvcResult result = perform(post("/api/tickets/knowledge/{articleNo}/confirm", draft.getArticleNo()), role("reviewer"));
        assertHttpResult(result, 409);

        DbSnapshot after = snapshot(ticketNo);
        assertThat(after).isEqualTo(before);
        assertThat(knowledgeArticleMapper.selectById(draft.getId()).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    void terminalTicketCannotRunCopilotAgain() throws Exception {
        String ticketNo = createTicketAsAgent();
        runCopilotAsAgent(ticketNo);
        MvcResult rejected = perform(post("/api/tickets/{id}/review/reject", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("comment", "stop current recommendation"))), role("reviewer"));
        assertHttpResult(rejected, 200);

        DbSnapshot before = snapshot(ticketNo);
        MvcResult result = perform(post("/api/tickets/{id}/run-copilot", ticketNo), role("agent"));
        assertHttpResult(result, 409);

        assertThat(snapshot(ticketNo)).isEqualTo(before);
        assertThat(before.ticketStatus()).isEqualTo(TicketStatusContract.REJECTED);
    }

    private static Stream<RoleCase> roles() {
        return Stream.of(
            new RoleCase("anonymous", null, null),
            new RoleCase("VIEWER", "viewer", "viewer123"),
            new RoleCase("AGENT", "agent", "agent123"),
            new RoleCase("REVIEWER", "reviewer", "reviewer123"),
            new RoleCase("ADMIN", "admin", "admin123")
        );
    }

    private static Stream<Arguments> reviewMatrix() {
        return Stream.of(ReviewAction.values())
            .flatMap(action -> roles().map(role -> Arguments.of(action, role)));
    }

    private static Stream<Arguments> terminalReviewActions() {
        return Stream.of(
            Arguments.of(ReviewAction.APPROVE),
            Arguments.of(ReviewAction.REJECT)
        );
    }

    private static Stream<String> illegalPendingManualTargets() {
        return TicketStatusContract.allStatuses().stream()
            .filter(target -> !TicketStatusContract.IN_PROGRESS.equals(target))
            .filter(target -> !TicketStatusContract.RESOLVED.equals(target));
    }

    private int expectedAuthorization(RoleCase role, Set<String> allowedRoles) {
        if (role.anonymous()) {
            return 401;
        }
        return allowedRoles.contains(role.role()) ? 200 : 403;
    }

    private RoleCase role(String role) {
        return roles().filter(candidate -> role.equalsIgnoreCase(candidate.name())).findFirst().orElseThrow();
    }

    private MvcResult perform(MockHttpServletRequestBuilder request, RoleCase role) throws Exception {
        if (!role.anonymous()) {
            request.header("Authorization", token(role.username(), role.password()));
        }
        return mockMvc.perform(request).andReturn();
    }

    private String createTicketAsAgent() throws Exception {
        MvcResult result = perform(post("/api/tickets")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(ticketPayload())), role("agent"));
        assertHttpResult(result, 200);
        return readJson(result).path("id").asText();
    }

    private String createResolvedTicketAsAgent() throws Exception {
        String ticketNo = createTicketAsAgent();
        MvcResult result = perform(post("/api/tickets/{id}/status", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "status", TicketStatusContract.RESOLVED,
                "actor", "Support Desk",
                "note", "prepare resolved fixture",
                "resolvedSummary", "The support desk confirmed the fix."
            ))), role("agent"));
        assertHttpResult(result, 200);
        assertThat(readJson(result).path("status").asText()).isEqualTo(TicketStatusContract.RESOLVED);
        return ticketNo;
    }

    private void runCopilotAsAgent(String ticketNo) throws Exception {
        MvcResult result = perform(post("/api/tickets/{id}/run-copilot", ticketNo), role("agent"));
        assertHttpResult(result, 200);
        assertThat(readJson(result).path("status").asText()).isEqualTo(TicketStatusContract.REVIEW_REQUIRED);
    }

    private String createDraftAsAgent(String ticketNo) throws Exception {
        MvcResult result = perform(post("/api/tickets/{id}/knowledge-draft", ticketNo)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of(
                "title", "Phase 0-A draft fixture",
                "content", "Resolved ticket draft fixture.",
                "owner", "Knowledge Owner",
                "confirm", false
            ))), role("agent"));
        assertHttpResult(result, 200);
        return readJson(result).path("articleNo").asText();
    }

    private Map<String, Object> ticketPayload() {
        return Map.of(
            "title", "payment-service returns 500",
            "description", "Payment service fails after release and returns HTTP 500.",
            "systemName", "payment-service",
            "errorLog", "HTTP 500 error timeout",
            "urgency", "P1",
            "requester", "Phase 0-A tester",
            "requesterDepartment", "支付研发"
        );
    }

    private DbSnapshot snapshot(String ticketNo) {
        SupportTicket ticket = ticketNo == null ? null : findTicket(ticketNo);
        KnowledgeArticle article = ticketNo == null ? null : knowledgeArticleMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getSourceTicketId, ticket == null ? -1L : ticket.getId())
                .orderByDesc(KnowledgeArticle::getId)
                .last("limit 1")
        );
        return new DbSnapshot(
            supportTicketMapper.selectCount(new LambdaQueryWrapper<SupportTicket>()),
            statusHistoryMapper.selectCount(new LambdaQueryWrapper<TicketStatusHistory>()),
            reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()),
            copilotRunMapper.selectCount(new LambdaQueryWrapper<CopilotRun>()),
            knowledgeArticleMapper.selectCount(new LambdaQueryWrapper<KnowledgeArticle>()),
            generationRecordMapper.selectCount(new LambdaQueryWrapper<GenerationRecord>()),
            analysisMapper.selectCount(new LambdaQueryWrapper<TicketAiAnalysisEntity>()),
            ticket == null ? null : ticket.getStatus(),
            article == null ? null : article.getStatus()
        );
    }

    private SupportTicket findTicket(String ticketNo) {
        return supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketNo));
    }

    private void assertHttpResult(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        if (expectedStatus >= 400) {
            assertThat(readJson(result).path("code").asInt()).isEqualTo(expectedStatus);
        }
    }

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + readJson(result).path("token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record RoleCase(String name, String username, String password) {
        private boolean anonymous() {
            return username == null;
        }

        private String role() {
            return name;
        }
    }

    private enum ReviewAction {
        APPROVE("/review/approve", TicketStatusContract.RESOLVED),
        REQUEST_CHANGES("/review/request-changes", TicketStatusContract.REVIEW_REQUIRED),
        REJECT("/review/reject", TicketStatusContract.REJECTED);

        private final String path;
        private final String targetStatus;

        ReviewAction(String path, String targetStatus) {
            this.path = path;
            this.targetStatus = targetStatus;
        }

        private String path() {
            return path;
        }

        private String targetStatus() {
            return targetStatus;
        }
    }

    private record DbSnapshot(
        long tickets,
        long histories,
        long reviews,
        long runs,
        long articles,
        long generations,
        long analyses,
        String ticketStatus,
        String articleStatus
    ) {
    }
}
