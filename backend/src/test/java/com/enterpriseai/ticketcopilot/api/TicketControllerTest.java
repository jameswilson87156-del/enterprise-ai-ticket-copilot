package com.enterpriseai.ticketcopilot.api;

import java.util.List;

import com.enterpriseai.ticketcopilot.model.SystemContext;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.enterpriseai.ticketcopilot.service.TicketWorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private TicketWorkflowService ticketWorkflowService;

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TicketController(ticketWorkflowService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void rejectsMissingTitleBeforeCallingWorkflow() throws Exception {
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "description": "Redis 连接失败",
                      "systemName": "employee-portal",
                      "urgency": "P2"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("title is required"))
            .andExpect(jsonPath("$.path").value("/api/tickets"))
            .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(ticketWorkflowService);
    }

    @Test
    void rejectsUnsupportedPriorityBeforeCallingWorkflow() throws Exception {
        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Redis 连接失败",
                      "description": "员工门户会话超时",
                      "systemName": "employee-portal",
                      "urgency": "P0"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("urgency must be P1, P2 or P3"))
            .andExpect(jsonPath("$.path").value("/api/tickets"))
            .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(ticketWorkflowService);
    }

    @Test
    void responseStatusExceptionUsesUnifiedErrorResponse() throws Exception {
        when(ticketWorkflowService.getTicket("NOTEXIST"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: NOTEXIST"));

        mockMvc.perform(get("/api/tickets/NOTEXIST"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("Ticket not found: NOTEXIST"))
            .andExpect(jsonPath("$.path").value("/api/tickets/NOTEXIST"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void acceptsValidTicketAndReturnsWorkflowResult() throws Exception {
        when(ticketWorkflowService.createTicket(any())).thenReturn(new TicketDetail(
            "TCK-1",
            "Redis 连接失败",
            "员工自助提交",
            "内部支持",
            TicketWorkflowService.STATUS_PENDING_PROCESS,
            "P2",
            "系统故障",
            "员工门户会话超时",
            new SystemContext("employee-portal", "internal", "corp", "-", "局部团队受影响"),
            List.of("Redis timeout"),
            List.of("等待人工确认"),
            List.of(),
            null
        ));

        mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Redis 连接失败",
                      "description": "员工门户会话超时",
                      "systemName": "employee-portal",
                      "urgency": "P2"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("TCK-1"))
            .andExpect(jsonPath("$.status").value("PENDING_PROCESS"));
    }
}
