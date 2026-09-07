package com.enterpriseai.ticketcopilot.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Refuses unsafe staging/production bootstraps before the web server is exposed. */
@Component
public class DeploymentSafetyValidator {

    private static final Set<String> EXTERNAL_PROVIDERS = Set.of("openai", "deepseek", "openai-compatible");

    private final Environment environment;

    public DeploymentSafetyValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateWhenDeploying() {
        if (!isDeploymentProfile()) {
            return;
        }

        requireEquals("ticket.auth.mode", "OIDC");
        requireNonBlank("ticket.auth.issuer-uri");
        requireEquals("ticket.ai.fallback-to-local", "false");
        requireNonBlank("spring.datasource.username");
        requireNonBlank("spring.datasource.password");

        String provider = value("ticket.ai.provider").toLowerCase(Locale.ROOT);
        if (!EXTERNAL_PROVIDERS.contains(provider)) {
            throw new IllegalStateException("ticket.ai.provider must select an external provider in staging/production.");
        }
        requireNonBlank("ticket.ai.base-url");
        requireNonBlank("ticket.ai.model");
        requireNonBlank("ticket.ai.api-key");
    }

    private boolean isDeploymentProfile() {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> "staging".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private void requireEquals(String key, String expected) {
        if (!expected.equalsIgnoreCase(value(key))) {
            throw new IllegalStateException(key + " must be " + expected + " in staging/production.");
        }
    }

    private void requireNonBlank(String key) {
        if (value(key).isBlank()) {
            throw new IllegalStateException(key + " must be configured in staging/production.");
        }
    }

    private String value(String key) {
        return environment.getProperty(key, "").trim();
    }
}
