package com.enterpriseai.ticketcopilot.ticket.application;

import java.util.List;

import com.enterpriseai.ticketcopilot.dto.CreateKnowledgeDraftRequest;
import com.enterpriseai.ticketcopilot.dto.CreateTicketRequest;
import com.enterpriseai.ticketcopilot.dto.ReviewDecisionRequest;
import com.enterpriseai.ticketcopilot.dto.UpdateTicketStatusRequest;
import com.enterpriseai.ticketcopilot.dto.WorkbenchMetrics;
import com.enterpriseai.ticketcopilot.model.AiAnalysis;
import com.enterpriseai.ticketcopilot.model.KnowledgeDraft;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.enterpriseai.ticketcopilot.model.TicketSummary;
import com.enterpriseai.ticketcopilot.model.TraceEvidence;
import com.enterpriseai.ticketcopilot.service.TicketWorkflowService;
import com.enterpriseai.ticketcopilot.ticket.application.port.in.TicketWorkflowUseCase;
import org.springframework.stereotype.Service;

/**
 * Transitional adapter that keeps the existing workflow implementation intact
 * while the ticket module is migrated behind an application port.
 */
@Service
public class LegacyTicketWorkflowFacade implements TicketWorkflowUseCase {

    private final TicketWorkflowService delegate;

    public LegacyTicketWorkflowFacade(TicketWorkflowService delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<TicketSummary> listTickets() {
        return delegate.listTickets();
    }

    @Override
    public TicketDetail createTicket(CreateTicketRequest request) {
        return delegate.createTicket(request);
    }

    @Override
    public WorkbenchMetrics metrics() {
        return delegate.metrics();
    }

    @Override
    public TicketDetail getTicket(String ticketNo) {
        return delegate.getTicket(ticketNo);
    }

    @Override
    public AiAnalysis getAiAnalysis(String ticketNo) {
        return delegate.getAiAnalysis(ticketNo);
    }

    @Override
    public TraceEvidence getTraceEvidence(String ticketNo) {
        return delegate.getTraceEvidence(ticketNo);
    }

    @Override
    public TicketDetail runCopilot(String ticketNo, String actor) {
        return delegate.runCopilot(ticketNo, actor);
    }

    @Override
    public TicketDetail approveReview(String ticketNo, String actor, String comment) {
        return delegate.approveReview(ticketNo, actor, comment);
    }

    @Override
    public TicketDetail requestReviewChanges(String ticketNo, String actor, String comment) {
        return delegate.requestReviewChanges(ticketNo, actor, comment);
    }

    @Override
    public TicketDetail rejectReview(String ticketNo, String actor, String comment) {
        return delegate.rejectReview(ticketNo, actor, comment);
    }

    @Override
    public TicketDetail updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        return delegate.updateStatus(ticketNo, request);
    }

    @Override
    public KnowledgeDraft createKnowledgeDraft(String ticketNo, CreateKnowledgeDraftRequest request) {
        return delegate.createKnowledgeDraft(ticketNo, request);
    }

    @Override
    public KnowledgeDraft confirmKnowledgeDraft(String articleNo) {
        return delegate.confirmKnowledgeDraft(articleNo);
    }
}
