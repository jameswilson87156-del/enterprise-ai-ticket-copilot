package com.enterpriseai.ticketcopilot.ticket.application.port.out;

import com.enterpriseai.ticketcopilot.model.CitationValidationStatus;
import com.enterpriseai.ticketcopilot.model.OutputValidationStatus;
import com.enterpriseai.ticketcopilot.model.StructuredCopilotOutput;

/**
 * Application port for the final human-review decision.
 *
 * <p>The workflow supplies validated output facts and policy inputs. The
 * port deliberately does not expose persistence entities, mappers, provider
 * services, or the legacy {@code service} package.</p>
 */
public interface ReviewPolicy {

    boolean requiresHumanReview(
        StructuredCopilotOutput output,
        boolean fallbackUsed,
        CitationValidationStatus citationValidationStatus,
        OutputValidationStatus outputValidationStatus,
        boolean businessRuleRequiresReview
    );
}
