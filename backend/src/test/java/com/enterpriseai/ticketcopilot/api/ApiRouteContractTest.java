package com.enterpriseai.ticketcopilot.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Freezes the legacy routes while /api/v1 is designed and migrated separately.
 */
class ApiRouteContractTest {

    @Test
    void ticketRoutesRemainStableDuringVersionedApiMigration() {
        assertThat(basePath(TicketController.class)).isEqualTo("/api/tickets");
        assertThat(routes(TicketController.class, true)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "listTickets", "/api/tickets",
            "metrics", "/api/tickets/metrics",
            "getTicket", "/api/tickets/{id}",
            "getAiAnalysis", "/api/tickets/{id}/ai-analysis",
            "getTraceEvidence", "/api/tickets/{id}/trace-evidence"
        ));
        assertThat(routes(TicketController.class, false)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "createTicket", "/api/tickets",
            "runCopilot", "/api/tickets/{id}/run-copilot",
            "approveReview", "/api/tickets/{id}/review/approve",
            "requestReviewChanges", "/api/tickets/{id}/review/request-changes",
            "rejectReview", "/api/tickets/{id}/review/reject",
            "updateStatus", "/api/tickets/{id}/status",
            "createKnowledgeDraft", "/api/tickets/{id}/knowledge-draft",
            "confirmKnowledgeDraft", "/api/tickets/knowledge/{articleNo}/confirm"
        ));
    }

    @Test
    void authAndHealthRoutesRemainStable() {
        assertThat(basePath(AuthController.class)).isEqualTo("/api/auth");
        assertThat(routes(AuthController.class, false)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "login", "/api/auth/login"
        ));
        assertThat(routes(AuthController.class, true)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "me", "/api/auth/me"
        ));
        assertThat(basePath(HealthController.class)).isEqualTo("/api");
        assertThat(routes(HealthController.class, true)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "health", "/api/health"
        ));
    }

    private String basePath(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        assertThat(mapping).as("missing class mapping on %s", controller.getSimpleName()).isNotNull();
        assertThat(mapping.value()).as("class mapping on %s", controller.getSimpleName()).hasSize(1);
        return mapping.value()[0];
    }

    private Map<String, String> routes(Class<?> controller, boolean get) {
        String base = basePath(controller);
        Map<String, String> result = new HashMap<>();
        for (Method method : controller.getDeclaredMethods()) {
            String[] suffixes;
            if (get && method.isAnnotationPresent(GetMapping.class)) {
                suffixes = method.getAnnotation(GetMapping.class).value();
            } else if (!get && method.isAnnotationPresent(PostMapping.class)) {
                suffixes = method.getAnnotation(PostMapping.class).value();
            } else {
                continue;
            }
            if (suffixes.length == 0) {
                suffixes = new String[] { "" };
            }
            assertThat(suffixes).as("one route per method: %s", method.getName()).hasSize(1);
            result.put(method.getName(), base + suffixes[0]);
        }
        return result;
    }
}
