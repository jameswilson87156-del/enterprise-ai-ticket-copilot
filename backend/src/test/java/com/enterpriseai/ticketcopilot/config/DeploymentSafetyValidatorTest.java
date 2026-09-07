package com.enterpriseai.ticketcopilot.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DeploymentSafetyValidatorTest {

    @Test
    void ignoresLocalProfiles() {
        assertThatCode(() -> new DeploymentSafetyValidator(new MockEnvironment()).validateWhenDeploying())
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeFallbackInDeploymentProfiles() {
        MockEnvironment environment = validDeploymentEnvironment();
        environment.setProperty("ticket.ai.fallback-to-local", "true");

        assertThatIllegalStateException()
            .isThrownBy(() -> new DeploymentSafetyValidator(environment).validateWhenDeploying())
            .withMessageContaining("ticket.ai.fallback-to-local");
    }

    @Test
    void acceptsCompleteExternalDeploymentConfiguration() {
        assertThatCode(() -> new DeploymentSafetyValidator(validDeploymentEnvironment()).validateWhenDeploying())
            .doesNotThrowAnyException();
    }

    private MockEnvironment validDeploymentEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        return environment
            .withProperty("ticket.auth.mode", "OIDC")
            .withProperty("ticket.auth.issuer-uri", "https://id.example.test/realms/ticket")
            .withProperty("ticket.ai.fallback-to-local", "false")
            .withProperty("ticket.ai.provider", "openai-compatible")
            .withProperty("ticket.ai.base-url", "https://provider.example.test/v1")
            .withProperty("ticket.ai.model", "model")
            .withProperty("ticket.ai.api-key", "test-key")
            .withProperty("spring.datasource.username", "ticket")
            .withProperty("spring.datasource.password", "password");
    }
}
