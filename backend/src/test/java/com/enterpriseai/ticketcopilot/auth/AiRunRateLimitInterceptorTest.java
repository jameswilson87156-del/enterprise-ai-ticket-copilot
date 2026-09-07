package com.enterpriseai.ticketcopilot.auth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRunRateLimitInterceptorTest {

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void limitsExpensiveRunsPerAuthenticatedUserAndReturnsRetryMetadata() throws Exception {
        AiRunRateLimitProperties properties = new AiRunRateLimitProperties();
        properties.setEnabled(true);
        properties.setLimit(2);
        properties.setWindowSeconds(60);
        AiRunRateLimitInterceptor interceptor = new AiRunRateLimitInterceptor(
            properties,
            Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC)
        );
        AuthContext.set(new AuthUser("agent@example.com", "Agent", "AGENT"));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/TCK-1/run-copilot");
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();
        MockHttpServletResponse rejected = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, first, new Object())).isTrue();
        assertThat(first.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
        assertThat(interceptor.preHandle(request, second, new Object())).isTrue();
        assertThat(second.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        assertThatThrownBy(() -> interceptor.preHandle(request, rejected, new Object()))
            .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
    }

    @Test
    void boundsDistinctIdentityWindowsBeforeTheirFixedWindowExpires() {
        AiRunRateLimitProperties properties = new AiRunRateLimitProperties();
        properties.setEnabled(true);
        properties.setMaximumKeys(100);
        AiRunRateLimitInterceptor interceptor = new AiRunRateLimitInterceptor(
            properties,
            Clock.fixed(Instant.parse("2026-09-04T00:00:00Z"), ZoneOffset.UTC)
        );

        for (int index = 0; index <= 100; index++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/tickets/TCK-1/run-copilot");
            request.setRemoteAddr("198.51.100." + index);
            assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        }

        assertThat(interceptor.trackedIdentityCount()).isLessThanOrEqualTo(100);
    }
}
