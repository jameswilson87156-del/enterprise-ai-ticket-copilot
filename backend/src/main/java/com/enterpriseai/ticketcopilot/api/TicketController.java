package com.enterpriseai.ticketcopilot.api;

import java.util.List;

import com.enterpriseai.ticketcopilot.dto.CreateKnowledgeDraftRequest;
import com.enterpriseai.ticketcopilot.dto.CreateTicketRequest;
import com.enterpriseai.ticketcopilot.dto.UpdateTicketStatusRequest;
import com.enterpriseai.ticketcopilot.dto.WorkbenchMetrics;
import com.enterpriseai.ticketcopilot.model.AiAnalysis;
import com.enterpriseai.ticketcopilot.model.KnowledgeDraft;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.enterpriseai.ticketcopilot.model.TicketSummary;
import com.enterpriseai.ticketcopilot.service.TicketWorkflowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/tickets", produces = "application/json;charset=UTF-8")
@Tag(name = "Tickets", description = "工单录入、规则分类、知识匹配、状态流转和知识沉淀接口")
public class TicketController {

    private final TicketWorkflowService ticketWorkflowService;

    public TicketController(TicketWorkflowService ticketWorkflowService) {
        this.ticketWorkflowService = ticketWorkflowService;
    }

    @GetMapping
    public List<TicketSummary> listTickets() {
        return ticketWorkflowService.listTickets();
    }

    @PostMapping
    public TicketDetail createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketWorkflowService.createTicket(request);
    }

    @GetMapping("/metrics")
    public WorkbenchMetrics metrics() {
        return ticketWorkflowService.metrics();
    }

    @GetMapping("/{id}")
    public TicketDetail getTicket(@PathVariable String id) {
        return ticketWorkflowService.getTicket(id);
    }

    @GetMapping("/{id}/ai-analysis")
    public AiAnalysis getAiAnalysis(@PathVariable String id) {
        return ticketWorkflowService.getAiAnalysis(id);
    }

    @PostMapping("/{id}/status")
    public TicketDetail updateStatus(@PathVariable String id, @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ticketWorkflowService.updateStatus(id, request);
    }

    @PostMapping("/{id}/knowledge-draft")
    public KnowledgeDraft createKnowledgeDraft(@PathVariable String id, @Valid @RequestBody CreateKnowledgeDraftRequest request) {
        return ticketWorkflowService.createKnowledgeDraft(id, request);
    }

    @PostMapping("/knowledge/{articleNo}/confirm")
    public KnowledgeDraft confirmKnowledgeDraft(@PathVariable String articleNo) {
        return ticketWorkflowService.confirmKnowledgeDraft(articleNo);
    }
}
