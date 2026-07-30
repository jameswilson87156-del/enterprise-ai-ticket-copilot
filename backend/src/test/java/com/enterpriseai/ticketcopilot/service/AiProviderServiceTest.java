package com.enterpriseai.ticketcopilot.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.enterpriseai.ticketcopilot.entity.RetrievalHit;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class AiProviderServiceTest {

    private HttpServer server;
    private AtomicInteger requestCount;

    @BeforeEach
    void setUp() throws IOException {
        requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", this::handleChatCompletion);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatCompletionsProtocolUsesCurrentAdapterAgainstLocalStubOnly(CapturedOutput output) {
        AiProviderResult result = service("openai-compatible", "chat-completions", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.sourceType()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.actualProvider()).isEqualTo("openai-compatible");
        assertThat(result.actualProtocol()).isEqualTo("chat-completions");
        assertThat(result.errorCategory()).isEqualTo("NONE");
        assertThat(result.content()).contains("OK");
        assertThat(requestCount).hasValue(1);
        assertThat(output).doesNotContain("Authorization").doesNotContain("placeholder-token");
    }

    @Test
    void bothProtocolUsesChatCompletionsAdapterAgainstLocalStubOnly() {
        AiProviderResult result = service("openai-compatible", "both", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.sourceType()).isEqualTo("OPENAI_COMPATIBLE");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void localRuleNeverCallsProviderEvenWhenConnectionSettingsExist() {
        AiProviderResult result = service("local-rule", "chat-completions", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("FALLBACK");
        assertThat(result.fallbackReason()).isEqualTo("PROVIDER_DISABLED");
        assertThat(result.actualProvider()).isEqualTo("local-rule");
        assertThat(result.errorCategory()).isEqualTo("CONFIGURATION_ERROR");
        assertThat(result.sourceType()).isEqualTo("LOCAL_RULE_FALLBACK");
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void responsesProtocolFailsClosedWithoutNetworkAndFallsBackLocally() {
        AiProviderResult result = service("openai-compatible", "responses", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("FALLBACK");
        assertThat(result.fallbackReason()).isEqualTo("PROTOCOL_NOT_SUPPORTED_BY_CURRENT_ADAPTER");
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void unknownProtocolFailsClosedWithoutNetworkAndFallsBackLocally() {
        AiProviderResult result = service("openai-compatible", "unknown-protocol", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("FALLBACK");
        assertThat(result.fallbackReason()).isEqualTo("UNSUPPORTED_PROTOCOL_CONFIGURATION");
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void unknownProviderFailsClosedWithoutNetworkAndFallsBackLocally() {
        AiProviderResult result = service("unexpected-provider", "chat-completions", true).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("FALLBACK");
        assertThat(result.fallbackReason()).isEqualTo("UNSUPPORTED_PROVIDER_CONFIGURATION");
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void fallbackDisabledReturnsSanitizedErrorWithoutLocalFallbackOrNetworkForUnsupportedProtocol() {
        AiProviderResult result = service("openai-compatible", "responses", false).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.fallbackUsed()).isFalse();
        assertThat(result.fallbackReason()).isEqualTo("PROTOCOL_NOT_SUPPORTED_BY_CURRENT_ADAPTER");
        assertThat(result.errorMessage()).isNull();
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void providerErrorSummaryDoesNotExposeCredentials(CapturedOutput output) {
        server.removeContext("/v1/chat/completions");
        server.createContext("/v1/chat/completions", exchange -> send(exchange, 500, "{\"error\":\"failed\"}"));

        AiProviderResult result = service("openai-compatible", "chat-completions", false).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.errorMessage()).isEqualTo("Provider returned HTTP 500");
        assertThat(result.actualProvider()).isEqualTo("openai-compatible");
        assertThat(result.errorCategory()).isEqualTo("UPSTREAM_5XX");
        assertThat(result.errorMessage()).doesNotContain("placeholder-token");
        assertThat(output).doesNotContain("Authorization").doesNotContain("placeholder-token");
    }

    @Test
    void providerHttpErrorsAreClassifiedWithoutResponseBodyLeakage() {
        server.removeContext("/v1/chat/completions");
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            send(exchange, 401, "{\"error\":\"secret upstream detail\"}");
        });

        AiProviderResult result = service("openai-compatible", "chat-completions", false).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.errorCategory()).isEqualTo("AUTHENTICATION_ERROR");
        assertThat(result.errorMessage()).isEqualTo("Provider returned HTTP 401");
        assertThat(result.errorMessage()).doesNotContain("secret upstream detail");
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void invalidJsonProviderResponseIsClassifiedAndSanitized() {
        server.removeContext("/v1/chat/completions");
        server.createContext("/v1/chat/completions", exchange -> {
            requestCount.incrementAndGet();
            send(exchange, 200, "not-json");
        });

        AiProviderResult result = service("openai-compatible", "chat-completions", false).complete(
            ticket(), "SYSTEM_FAILURE", retrievalHits(), draft()
        );

        assertThat(result.status()).isEqualTo("ERROR");
        assertThat(result.errorCategory()).isEqualTo("INVALID_JSON");
        assertThat(result.errorMessage()).isEqualTo("provider response JSON is invalid.");
        assertThat(requestCount).hasValue(1);
    }

    private AiProviderService service(String provider, String protocol, boolean fallbackToLocal) {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("ticket.ai.provider", provider)
            .withProperty("ticket.ai.protocol", protocol)
            .withProperty("ticket.ai.base-url", "http://127.0.0.1:" + server.getAddress().getPort())
            .withProperty("ticket.ai.model", "unit-test-model")
            .withProperty("ticket.ai.api-key", "placeholder-token")
            .withProperty("ticket.ai.fallback-to-local", Boolean.toString(fallbackToLocal));
        return new AiProviderService(environment, new ObjectMapper());
    }

    private void handleChatCompletion(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        send(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}");
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream response = exchange.getResponseBody()) {
            response.write(bytes);
        }
    }

    private SupportTicket ticket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNo("TCK-UNIT-1");
        ticket.setTitle("synthetic ticket");
        ticket.setDescription("synthetic description");
        ticket.setSystemName("synthetic-system");
        ticket.setUrgency("P2");
        return ticket;
    }

    private List<RetrievalHit> retrievalHits() {
        RetrievalHit hit = new RetrievalHit();
        hit.setKnowledgeArticleNo("KB-UNIT-1");
        hit.setKnowledgeTitleSnapshot("synthetic knowledge");
        hit.setKnowledgeCategorySnapshot("SYSTEM_FAILURE");
        hit.setExcerptSnapshot("synthetic excerpt");
        hit.setScore(99);
        return List.of(hit);
    }

    private RecommendationDraft draft() {
        return new RecommendationDraft(List.of("synthetic step"), "synthetic local draft", List.of("synthetic risk"));
    }
}
