package com.enterpriseai.ticketcopilot.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Bounded per-identity protection for costly Copilot-run requests. */
@Component
@ConfigurationProperties(prefix = "ticket.ai.rate-limit")
public class AiRunRateLimitProperties {

    private boolean enabled;
    private int limit = 6;
    private int windowSeconds = 60;
    private int maximumKeys = 10_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLimit() {
        return Math.max(1, Math.min(limit, 1_000));
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getWindowSeconds() {
        return Math.max(1, Math.min(windowSeconds, 3_600));
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getMaximumKeys() {
        return Math.max(100, Math.min(maximumKeys, 100_000));
    }

    public void setMaximumKeys(int maximumKeys) {
        this.maximumKeys = maximumKeys;
    }
}
