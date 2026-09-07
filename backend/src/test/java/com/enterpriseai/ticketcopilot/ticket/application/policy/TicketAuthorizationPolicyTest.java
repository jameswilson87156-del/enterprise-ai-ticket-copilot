package com.enterpriseai.ticketcopilot.ticket.application.policy;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.enterpriseai.ticketcopilot.auth.AuthUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAuthorizationPolicyTest {

    private final TicketAuthorizationPolicy policy = new TicketAuthorizationPolicy();

    @ParameterizedTest(name = "{0} may perform {1}")
    @MethodSource("allowedActions")
    void allowsOnlyTheDocumentedRoleForEachWriteAction(String role, TicketAuthorizationPolicy.Action action) {
        policy.require(user(role), action);
    }

    @ParameterizedTest(name = "{0} cannot perform {1}")
    @MethodSource("deniedActions")
    void deniesEveryRoleOutsideTheActionMatrix(String role, TicketAuthorizationPolicy.Action action) {
        assertThatThrownBy(() -> policy.require(user(role), action))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
            );
    }

    @org.junit.jupiter.api.Test
    void anonymousWriteRequestsAreUnauthorized() {
        assertThatThrownBy(() -> policy.require(null, TicketAuthorizationPolicy.Action.CREATE_TICKET))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
            );
    }

    @org.junit.jupiter.api.Test
    void draftCreationWithConfirmUsesPublishPermission() {
        policy.requireKnowledgeDraftWrite(user("AGENT"), false);
        policy.requireKnowledgeDraftWrite(user("REVIEWER"), true);
        assertThatThrownBy(() -> policy.requireKnowledgeDraftWrite(user("AGENT"), true))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
            );
    }

    private AuthUser user(String role) {
        return new AuthUser(role.toLowerCase(), role + " user", role);
    }

    private static Stream<Arguments> allowedActions() {
        return Stream.of(
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.CREATE_TICKET),
            Arguments.of("AGENT", TicketAuthorizationPolicy.Action.CREATE_TICKET),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.UPDATE_STATUS),
            Arguments.of("AGENT", TicketAuthorizationPolicy.Action.UPDATE_STATUS),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.RUN_COPILOT),
            Arguments.of("AGENT", TicketAuthorizationPolicy.Action.RUN_COPILOT),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.APPROVE_REVIEW),
            Arguments.of("REVIEWER", TicketAuthorizationPolicy.Action.APPROVE_REVIEW),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.REQUEST_REVIEW_CHANGES),
            Arguments.of("REVIEWER", TicketAuthorizationPolicy.Action.REQUEST_REVIEW_CHANGES),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.REJECT_REVIEW),
            Arguments.of("REVIEWER", TicketAuthorizationPolicy.Action.REJECT_REVIEW),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.CREATE_KNOWLEDGE_DRAFT),
            Arguments.of("AGENT", TicketAuthorizationPolicy.Action.CREATE_KNOWLEDGE_DRAFT),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.CONFIRM_KNOWLEDGE_DRAFT),
            Arguments.of("REVIEWER", TicketAuthorizationPolicy.Action.CONFIRM_KNOWLEDGE_DRAFT),
            Arguments.of("ADMIN", TicketAuthorizationPolicy.Action.PUBLISH_KNOWLEDGE_DRAFT),
            Arguments.of("REVIEWER", TicketAuthorizationPolicy.Action.PUBLISH_KNOWLEDGE_DRAFT)
        );
    }

    private static Stream<Arguments> deniedActions() {
        List<String> roles = List.of("VIEWER", "AGENT", "REVIEWER", "ADMIN");
        Map<TicketAuthorizationPolicy.Action, List<String>> allowed = Map.of(
            TicketAuthorizationPolicy.Action.CREATE_TICKET, List.of("ADMIN", "AGENT"),
            TicketAuthorizationPolicy.Action.UPDATE_STATUS, List.of("ADMIN", "AGENT"),
            TicketAuthorizationPolicy.Action.RUN_COPILOT, List.of("ADMIN", "AGENT"),
            TicketAuthorizationPolicy.Action.APPROVE_REVIEW, List.of("ADMIN", "REVIEWER"),
            TicketAuthorizationPolicy.Action.REQUEST_REVIEW_CHANGES, List.of("ADMIN", "REVIEWER"),
            TicketAuthorizationPolicy.Action.REJECT_REVIEW, List.of("ADMIN", "REVIEWER"),
            TicketAuthorizationPolicy.Action.CREATE_KNOWLEDGE_DRAFT, List.of("ADMIN", "AGENT"),
            TicketAuthorizationPolicy.Action.CONFIRM_KNOWLEDGE_DRAFT, List.of("ADMIN", "REVIEWER"),
            TicketAuthorizationPolicy.Action.PUBLISH_KNOWLEDGE_DRAFT, List.of("ADMIN", "REVIEWER")
        );
        return Stream.of(TicketAuthorizationPolicy.Action.values())
            .flatMap(action -> roles.stream()
                .filter(role -> !allowed.get(action).contains(role))
                .map(role -> Arguments.of(role, action)));
    }
}
