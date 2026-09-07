package com.enterpriseai.ticketcopilot.ticket.application.port.in;

import java.util.List;

import com.enterpriseai.ticketcopilot.dto.CreateKnowledgeDraftRequest;
import com.enterpriseai.ticketcopilot.dto.CreateTicketRequest;
import com.enterpriseai.ticketcopilot.dto.UpdateTicketStatusRequest;
import com.enterpriseai.ticketcopilot.dto.WorkbenchMetrics;
import com.enterpriseai.ticketcopilot.model.AiAnalysis;
import com.enterpriseai.ticketcopilot.model.KnowledgeDraft;
import com.enterpriseai.ticketcopilot.model.TicketDetail;
import com.enterpriseai.ticketcopilot.model.TicketSummary;
import com.enterpriseai.ticketcopilot.model.TraceEvidence;

/**
 * Inbound application port for the ticket workflow.
 *
 * <p>The current REST controller uses this interface instead of the legacy
 * all-in-one service. DTOs and read models remain unchanged for compatibility;
 * persistence types and provider implementations are deliberately absent from
 * this port.</p>
 */
public interface TicketWorkflowUseCase {

    List<TicketSummary> listTickets();

    TicketDetail createTicket(CreateTicketRequest request);

    WorkbenchMetrics metrics();

    TicketDetail getTicket(String ticketNo);

    AiAnalysis getAiAnalysis(String ticketNo);

    TraceEvidence getTraceEvidence(String ticketNo);

    TicketDetail runCopilot(String ticketNo, String actor);

    TicketDetail approveReview(String ticketNo, String actor, String comment);

    TicketDetail requestReviewChanges(String ticketNo, String actor, String comment);

    TicketDetail rejectReview(String ticketNo, String actor, String comment);

    TicketDetail updateStatus(String ticketNo, UpdateTicketStatusRequest request);

    KnowledgeDraft createKnowledgeDraft(String ticketNo, CreateKnowledgeDraftRequest request);

    KnowledgeDraft confirmKnowledgeDraft(String articleNo);
}
