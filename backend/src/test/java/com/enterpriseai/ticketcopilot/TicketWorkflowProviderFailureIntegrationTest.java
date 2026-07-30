package com.enterpriseai.ticketcopilot;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enterpriseai.ticketcopilot.entity.CopilotRun;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "ticket.ai.provider=openai-compatible",
    "ticket.ai.protocol=responses",
    "ticket.ai.base-url=http://127.0.0.1:1",
    "ticket.ai.model=test-model",
    "ticket.ai.api-key=placeholder-present",
    "ticket.ai.fallback-to-local=false"
})
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class TicketWorkflowProviderFailureIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SupportTicketMapper supportTicketMapper;
    @Autowired private CopilotRunMapper copilotRunMapper;

    @Test
    void fallbackDisabledProviderFailureDoesNotCreateSuccessfulAbstentionOrNetworkCall() throws Exception {
        String token = token("agent", "agent123");
        JsonNode created = createTicket(token);
        String ticketId = created.path("id").asText();

        mockMvc.perform(post("/api/tickets/{id}/run-copilot", ticketId).header("Authorization", token))
            .andExpect(status().isOk());

        SupportTicket ticket = supportTicketMapper.selectOne(new LambdaQueryWrapper<SupportTicket>()
            .eq(SupportTicket::getTicketNo, ticketId));
        CopilotRun run = copilotRunMapper.selectOne(new LambdaQueryWrapper<CopilotRun>()
            .eq(CopilotRun::getTicketId, ticket.getId())
            .orderByDesc(CopilotRun::getStartedAt)
            .last("limit 1"));

        assertThat(run.getRunStatus()).isEqualTo("FAILED");
        assertThat(run.getFallbackUsed()).isFalse();
        assertThat(run.getOutputProduced()).isFalse();
        assertThat(run.getErrorCategory()).isEqualTo("UNSUPPORTED_PROTOCOL");
    }

    private JsonNode createTicket(String token) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "title", "payment-service returns 500",
            "description", "checkout requests return HTTP 500 and timeout",
            "systemName", "payment-service",
            "errorLog", "HTTP 500 timeout",
            "urgency", "P1",
            "requester", "Agent Tester",
            "requesterDepartment", "QA"
        ));
        return objectMapper.readTree(mockMvc.perform(post("/api/tickets")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8));
    }

    private String token(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
        return "Bearer " + objectMapper.readTree(response).path("token").asText();
    }
}
