package com.enterpriseai.ticketcopilot.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class SharedProviderConfigMappingTest {

    @Test
    void ticketAiVariablesTakePriorityOverPortfolioSharedVariables() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(Map.ofEntries(
            Map.entry("TICKET_AI_PROVIDER", "ticket-provider"),
            Map.entry("TICKET_AI_BASE_URL", "ticket-base-present"),
            Map.entry("TICKET_AI_MODEL", "ticket-model"),
            Map.entry("TICKET_AI_API_KEY", "ticket-key-present"),
            Map.entry("TICKET_AI_PROTOCOL", "chat-completions"),
            Map.entry("TICKET_AI_FALLBACK_TO_LOCAL", "false"),
            Map.entry("PORTFOLIO_AI_PROVIDER", "portfolio-provider"),
            Map.entry("PORTFOLIO_AI_BASE_URL", "portfolio-base-present"),
            Map.entry("PORTFOLIO_AI_MODEL", "portfolio-model"),
            Map.entry("PORTFOLIO_AI_API_KEY", "portfolio-key-present"),
            Map.entry("PORTFOLIO_AI_PROTOCOL", "both"),
            Map.entry("PORTFOLIO_AI_FALLBACK_ENABLED", "true")
        ));

        assertThat(resolver.getProperty("ticket.ai.provider")).isEqualTo("ticket-provider");
        assertThat(resolver.getProperty("ticket.ai.base-url")).isEqualTo("ticket-base-present");
        assertThat(resolver.getProperty("ticket.ai.model")).isEqualTo("ticket-model");
        assertThat(resolver.getProperty("ticket.ai.api-key")).isEqualTo("ticket-key-present");
        assertThat(resolver.getProperty("ticket.ai.protocol")).isEqualTo("chat-completions");
        assertThat(resolver.getProperty("ticket.ai.fallback-to-local")).isEqualTo("false");
    }

    @Test
    void portfolioSharedVariablesAreFallbackWhenTicketVariablesAreMissing() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(Map.of(
            "PORTFOLIO_AI_PROVIDER", "openai-compatible",
            "PORTFOLIO_AI_BASE_URL", "portfolio-base-present",
            "PORTFOLIO_AI_MODEL", "portfolio-model",
            "PORTFOLIO_AI_API_KEY", "portfolio-key-present",
            "PORTFOLIO_AI_PROTOCOL", "both",
            "PORTFOLIO_AI_FALLBACK_ENABLED", "true"
        ));

        assertThat(resolver.getProperty("ticket.ai.provider")).isEqualTo("openai-compatible");
        assertThat(resolver.getProperty("ticket.ai.base-url")).isEqualTo("portfolio-base-present");
        assertThat(resolver.getProperty("ticket.ai.model")).isEqualTo("portfolio-model");
        assertThat(resolver.getProperty("ticket.ai.api-key")).isEqualTo("portfolio-key-present");
        assertThat(resolver.getProperty("ticket.ai.protocol")).isEqualTo("both");
        assertThat(resolver.getProperty("ticket.ai.fallback-to-local")).isEqualTo("true");
    }

    @Test
    void defaultsRemainSafeWhenTicketAndPortfolioVariablesAreMissing() throws IOException {
        PropertySourcesPropertyResolver resolver = resolver(Map.of());

        assertThat(resolver.getProperty("ticket.ai.provider")).isEqualTo("local-rule");
        assertThat(resolver.getProperty("ticket.ai.base-url")).isEmpty();
        assertThat(resolver.getProperty("ticket.ai.model")).isEqualTo("gpt-5.5");
        assertThat(resolver.getProperty("ticket.ai.api-key")).isEmpty();
        assertThat(resolver.getProperty("ticket.ai.protocol")).isEqualTo("chat-completions");
        assertThat(resolver.getProperty("ticket.ai.fallback-to-local")).isEqualTo("true");
    }

    @Test
    void testProfilePinsLocalRuleAndDoesNotDependOnHostSharedProvider() throws IOException {
        PropertySourcesPropertyResolver resolver = resolverFromYaml("application-test.yml", Map.of(
            "PORTFOLIO_AI_PROVIDER", "openai-compatible",
            "PORTFOLIO_AI_API_KEY", "host-key-present"
        ));

        assertThat(resolver.getProperty("ticket.ai.provider")).isEqualTo("local-rule");
        assertThat(resolver.getProperty("ticket.ai.protocol")).isEqualTo("chat-completions");
        assertThat(resolver.getProperty("ticket.ai.base-url")).isEmpty();
        assertThat(resolver.getProperty("ticket.ai.model")).isEqualTo("test-model");
        assertThat(resolver.getProperty("ticket.ai.api-key")).isEmpty();
        assertThat(resolver.getProperty("ticket.ai.fallback-to-local")).isEqualTo("true");
    }

    private PropertySourcesPropertyResolver resolver(Map<String, Object> values) throws IOException {
        return resolverFromYaml("application.yml", values);
    }

    private PropertySourcesPropertyResolver resolverFromYaml(String yaml, Map<String, Object> values) throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("synthetic-env", new HashMap<>(values)));
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load(yaml, new ClassPathResource(yaml))) {
            sources.addLast(source);
        }
        return new PropertySourcesPropertyResolver(sources);
    }
}
