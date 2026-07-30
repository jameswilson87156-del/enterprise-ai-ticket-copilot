package com.enterpriseai.ticketcopilot;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.CopilotResult;
import com.enterpriseai.ticketcopilot.entity.CopilotRun;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.CopilotResultMapper;
import com.enterpriseai.ticketcopilot.mapper.CopilotRunMapper;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "ticket.ai.provider=local-rule",
    "ticket.ai.protocol=chat-completions",
    "ticket.ai.base-url=",
    "ticket.ai.model=test-model",
    "ticket.ai.api-key=",
    "ticket.ai.fallback-to-local=true"
})
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowLocalRuleSemanticsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SupportTicketMapper supportTicketMapper;
    @Autowired private CopilotRunMapper copilotRunMapper;
    @Autowired private CopilotResultMapper copilotResultMapper;

    @Test
    void intentionalLocalRuleRunIsNormalSuccessNotFallback() throws Exception {
        String agentToken = token("agent", "agent123");
        String ticketId = createTicket(agentToken).path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", agentToken))
            .andExpect(status().isOk());

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = copilotRunMapper.selectOne(new LambdaQueryWrapper<CopilotRun>()
            .eq(CopilotRun::getTicketId, ticket.getId())
            .orderByDesc(CopilotRun::getStartedAt)
            .last("limit 1"));
        CopilotResult result = copilotResultMapper.selectOne(new LambdaQueryWrapper<CopilotResult>()
            .eq(CopilotResult::getRunId, run.getRunId()));

        assertThat(run.getRequestedProvider()).isEqualTo("local-rule");
        assertThat(run.getActualProvider()).isEqualTo("local-rule");
        assertThat(run.getActualProtocol()).isEqualTo("local-rule");
        assertThat(run.getFallbackUsed()).isFalse();
        assertThat(run.getFallbackReasonCode()).isNull();
        assertThat(run.getErrorCategory()).isEqualTo("NONE");
        assertThat(run.getRunStatus()).isEqualTo("SUCCESS");
        assertThat(result.getModelHumanReviewRequired()).isTrue();
    }

    private JsonNode createTicket(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "payment-service returns 500",
                    "description", "Payment service fails after release and returns HTTP 500.",
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

    private String token(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("token").asText();
    }
}
