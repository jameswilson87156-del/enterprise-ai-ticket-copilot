package com.enterpriseai.ticketcopilot.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class AiProviderService {

    private static final String PROVIDER_LOCAL_RULE = "local-rule";
    private static final String PROVIDER_OPENAI_COMPATIBLE = "openai-compatible";
    private static final String PROTOCOL_CHAT_COMPLETIONS = "chat-completions";
    private static final String PROTOCOL_BOTH = "both";
    private static final String PROTOCOL_RESPONSES = "responses";
    private static final String MODEL_NONE = "N/A (no LLM)";
    private static final int SUMMARY_LIMIT = 280;

    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiProviderService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    }

    public AiProviderResult complete(SupportTicket ticket, String classification, List<KnowledgeMatch> matches, RecommendationDraft localDraft) {
        long started = System.currentTimeMillis();
        ProviderSettings settings = settings();
        String promptSummary = promptSummary(ticket, classification, matches, localDraft);
        String localResponse = localDraft.replySuggestion();
        if (PROVIDER_LOCAL_RULE.equalsIgnoreCase(settings.providerName())) {
            return fallback(settings, started, promptSummary, localResponse, "PROVIDER_DISABLED", null);
        }
        if (!PROVIDER_OPENAI_COMPATIBLE.equalsIgnoreCase(settings.providerName())) {
            return providerFailure(settings, started, promptSummary, localResponse, "UNSUPPORTED_PROVIDER_CONFIGURATION", null);
        }
        if (PROTOCOL_RESPONSES.equalsIgnoreCase(settings.protocol())) {
            return providerFailure(settings, started, promptSummary, localResponse, "PROTOCOL_NOT_SUPPORTED_BY_CURRENT_ADAPTER", null);
        }
        if (!PROTOCOL_CHAT_COMPLETIONS.equalsIgnoreCase(settings.protocol()) && !PROTOCOL_BOTH.equalsIgnoreCase(settings.protocol())) {
            return providerFailure(settings, started, promptSummary, localResponse, "UNSUPPORTED_PROTOCOL_CONFIGURATION", null);
        }
        if (settings.apiKey().isBlank()) {
            return providerFailure(settings, started, promptSummary, localResponse, "API_KEY_MISSING", null);
        }
        if (settings.baseUrl().isBlank()) {
            return providerFailure(settings, started, promptSummary, localResponse, "BASE_URL_MISSING", null);
        }
        try {
            String content = requestProvider(settings, ticket, classification, matches, localDraft);
            return new AiProviderResult(
                settings.providerName(),
                settings.modelName(),
                false,
                null,
                elapsed(started),
                "SUCCESS",
                null,
                summarize(promptSummary),
                summarize(content),
                content,
                "OPENAI_COMPATIBLE"
            );
        } catch (HttpTimeoutException exception) {
            return providerFailure(settings, started, promptSummary, localResponse, "TIMEOUT", "Provider request timed out.");
        } catch (IllegalArgumentException exception) {
            return providerFailure(settings, started, promptSummary, localResponse, "PARSE_ERROR", safeProviderError(exception));
        } catch (IOException exception) {
            return providerFailure(settings, started, promptSummary, localResponse, "PROVIDER_ERROR", safeProviderError(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return providerFailure(settings, started, promptSummary, localResponse, "TIMEOUT", "Provider request was interrupted.");
        } catch (RuntimeException exception) {
            return providerFailure(settings, started, promptSummary, localResponse, "PROVIDER_ERROR", safeProviderError(exception));
        }
    }

    private AiProviderResult providerFailure(
        ProviderSettings settings,
        long started,
        String promptSummary,
        String localResponse,
        String fallbackReason,
        String errorMessage
    ) {
        if (settings.fallbackToLocal()) {
            return fallback(settings, started, promptSummary, localResponse, fallbackReason, errorMessage);
        }
        return new AiProviderResult(
            settings.providerName(),
            settings.modelName(),
            false,
            fallbackReason,
            elapsed(started),
            "ERROR",
            summarize(errorMessage),
            summarize(promptSummary),
            "",
            "",
            PROVIDER_OPENAI_COMPATIBLE.equalsIgnoreCase(settings.providerName()) ? "OPENAI_COMPATIBLE" : "LOCAL_RULE_FALLBACK"
        );
    }

    private AiProviderResult fallback(
        ProviderSettings settings,
        long started,
        String promptSummary,
        String localResponse,
        String fallbackReason,
        String errorMessage
    ) {
        return new AiProviderResult(
            settings.providerName(),
            providerModelName(settings),
            true,
            fallbackReason,
            elapsed(started),
            "FALLBACK",
            summarize(errorMessage),
            summarize(promptSummary),
            summarize(localResponse),
            localResponse,
            "LOCAL_RULE_FALLBACK"
        );
    }

    private String requestProvider(
        ProviderSettings settings,
        SupportTicket ticket,
        String classification,
        List<KnowledgeMatch> matches,
        RecommendationDraft localDraft
    ) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
            "model", settings.modelName(),
            "temperature", 0.2,
            "messages", List.of(
                Map.of(
                    "role", "system",
                    "content", "You are an internal IT support copilot. Return a concise Chinese review draft. Do not claim an action was executed."
                ),
                Map.of(
                    "role", "user",
                    "content", promptBody(ticket, classification, matches, localDraft)
                )
            )
        );
        HttpRequest request = HttpRequest.newBuilder(chatCompletionsUri(settings.baseUrl()))
            .timeout(Duration.ofSeconds(18))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + settings.apiKey())
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Provider returned HTTP " + response.statusCode());
        }
        JsonNode content = objectMapper.readTree(response.body())
            .path("choices")
            .path(0)
            .path("message")
            .path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IllegalArgumentException("choices[0].message.content is empty.");
        }
        return content.asText().trim();
    }

    private URI chatCompletionsUri(String baseUrl) {
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return URI.create(normalized + "/chat/completions");
    }

    private ProviderSettings settings() {
        String providerName = property("ticket.ai.provider", PROVIDER_LOCAL_RULE);
        String modelName = property("ticket.ai.model", "gpt-5.5");
        String protocol = property("ticket.ai.protocol", PROTOCOL_CHAT_COMPLETIONS);
        return new ProviderSettings(
            providerName.isBlank() ? PROVIDER_LOCAL_RULE : providerName,
            property("ticket.ai.base-url", ""),
            modelName.isBlank() ? "gpt-5.5" : modelName,
            property("ticket.ai.api-key", ""),
            protocol.isBlank() ? PROTOCOL_CHAT_COMPLETIONS : protocol,
            Boolean.parseBoolean(property("ticket.ai.fallback-to-local", "true"))
        );
    }

    private String property(String key, String fallback) {
        String value = environment.getProperty(key);
        return value == null ? fallback : value.trim();
    }

    private String providerModelName(ProviderSettings settings) {
        return PROVIDER_LOCAL_RULE.equalsIgnoreCase(settings.providerName()) ? MODEL_NONE : settings.modelName();
    }

    private String safeProviderError(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "Provider request failed.";
        }
        String message = exception.getMessage();
        if (message.contains("choices[0].message.content")) {
            return message;
        }
        if (message.startsWith("Provider returned HTTP ")) {
            return message;
        }
        return "Provider request failed.";
    }

    private String promptSummary(SupportTicket ticket, String classification, List<KnowledgeMatch> matches, RecommendationDraft localDraft) {
        return "ticket=" + ticket.getTicketNo()
            + "; classification=" + classification
            + "; priority=" + ticket.getUrgency()
            + "; knowledgeHits=" + matches.stream().map(match -> match.article().getArticleNo()).toList()
            + "; localDraft=" + localDraft.replySuggestion();
    }

    private String promptBody(SupportTicket ticket, String classification, List<KnowledgeMatch> matches, RecommendationDraft localDraft) {
        return """
            Ticket: %s
            Title: %s
            Description: %s
            System: %s
            Priority: %s
            Classification: %s
            Knowledge references: %s
            Local draft: %s
            Required boundary: human review is required before status changes or customer reply.
            """.formatted(
            ticket.getTicketNo(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getSystemName(),
            ticket.getUrgency(),
            classification,
            matches.stream().map(match -> match.article().getTitle()).toList(),
            localDraft.replySuggestion()
        );
    }

    private long elapsed(long started) {
        return Math.max(0, System.currentTimeMillis() - started);
    }

    private String summarize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > SUMMARY_LIMIT ? normalized.substring(0, SUMMARY_LIMIT) : normalized;
    }

    private record ProviderSettings(
        String providerName,
        String baseUrl,
        String modelName,
        String apiKey,
        String protocol,
        boolean fallbackToLocal
    ) {
    }
}
