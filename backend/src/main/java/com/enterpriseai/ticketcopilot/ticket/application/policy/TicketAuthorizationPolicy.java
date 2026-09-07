package com.enterpriseai.ticketcopilot.ticket.application.policy;

import java.util.Map;
import java.util.Set;

import com.enterpriseai.ticketcopilot.auth.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Central authorization matrix for ticket write operations.
 *
 * <p>The matrix deliberately uses only the four roles already present in the
 * Demo Auth contract. It is an application authorization boundary, not a
 * production identity provider or tenant-aware RBAC implementation.</p>
 */
@Component
public final class TicketAuthorizationPolicy {

    public enum Action {
        CREATE_TICKET,
        UPDATE_STATUS,
        RUN_COPILOT,
        APPROVE_REVIEW,
        REQUEST_REVIEW_CHANGES,
        REJECT_REVIEW,
        CREATE_KNOWLEDGE_DRAFT,
        CONFIRM_KNOWLEDGE_DRAFT,
        PUBLISH_KNOWLEDGE_DRAFT
    }

    private static final Map<Action, Set<String>> ALLOWED_ROLES = Map.of(
        Action.CREATE_TICKET, Set.of("ADMIN", "AGENT"),
        Action.UPDATE_STATUS, Set.of("ADMIN", "AGENT"),
        Action.RUN_COPILOT, Set.of("ADMIN", "AGENT"),
        Action.APPROVE_REVIEW, Set.of("ADMIN", "REVIEWER"),
        Action.REQUEST_REVIEW_CHANGES, Set.of("ADMIN", "REVIEWER"),
        Action.REJECT_REVIEW, Set.of("ADMIN", "REVIEWER"),
        Action.CREATE_KNOWLEDGE_DRAFT, Set.of("ADMIN", "AGENT"),
        Action.CONFIRM_KNOWLEDGE_DRAFT, Set.of("ADMIN", "REVIEWER"),
        Action.PUBLISH_KNOWLEDGE_DRAFT, Set.of("ADMIN", "REVIEWER")
    );

    public void require(AuthUser user, Action action) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication token is required.");
        }
        Set<String> allowedRoles = ALLOWED_ROLES.get(action);
        if (allowedRoles == null) {
            throw new IllegalArgumentException("Unsupported ticket authorization action: " + action);
        }
        if (!user.hasAnyRole(allowedRoles.toArray(String[]::new))) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Insufficient role for ticket operation: " + action.name()
            );
        }
    }

    public void requireKnowledgeDraftWrite(AuthUser user, boolean confirm) {
        require(user, confirm ? Action.PUBLISH_KNOWLEDGE_DRAFT : Action.CREATE_KNOWLEDGE_DRAFT);
    }

    public Set<String> allowedRoles(Action action) {
        return ALLOWED_ROLES.getOrDefault(action, Set.of());
    }
}
