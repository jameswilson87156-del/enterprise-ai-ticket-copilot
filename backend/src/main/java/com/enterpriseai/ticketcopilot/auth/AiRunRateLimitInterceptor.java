package com.enterpriseai.ticketcopilot.auth;

import java.time.Clock;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

/**
 * Limits the expensive Copilot-run endpoint after the authentication interceptor
 * has established the request identity. The in-memory limiter is intentionally
 * local-only; a multi-instance deployment should place an additional shared
 * gateway or Redis limiter in front of it.
 */
@Component
public class AiRunRateLimitInterceptor implements HandlerInterceptor {

    private final AiRunRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public AiRunRateLimitInterceptor(AiRunRateLimitProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AiRunRateLimitInterceptor(AiRunRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!properties.isEnabled()) {
            return true;
        }

        long now = clock.instant().getEpochSecond();
        Decision decision = consume(identity(request), now);
        response.setHeader("X-RateLimit-Limit", String.valueOf(properties.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetAtEpochSecond()));

        if (decision.allowed()) {
            return true;
        }

        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Copilot 调用过于频繁，请在 " + decision.retryAfterSeconds() + " 秒后重试。"
        );
    }

    private Decision consume(String identity, long now) {
        int limit = properties.getLimit();
        int windowSeconds = properties.getWindowSeconds();
        Window window = windows.compute(identity, (ignored, current) -> {
            if (current == null || current.resetAtEpochSecond() <= now) {
                return new Window(1, now + windowSeconds);
            }
            return new Window(current.count() + 1, current.resetAtEpochSecond());
        });
        trimExpired(now);

        boolean allowed = window.count() <= limit;
        int remaining = Math.max(0, limit - window.count());
        int retryAfter = Math.max(1, (int) (window.resetAtEpochSecond() - now));
        return new Decision(allowed, remaining, window.resetAtEpochSecond(), retryAfter);
    }

    private void trimExpired(long now) {
        int maximumKeys = properties.getMaximumKeys();
        if (windows.size() <= maximumKeys) {
            return;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().resetAtEpochSecond() <= now);
        int overflow = windows.size() - maximumKeys;
        if (overflow <= 0) {
            return;
        }
        // A short flood of distinct identities must not bypass the configured
        // memory bound merely because none of its fixed windows has expired yet.
        windows.entrySet().stream()
            .sorted(Comparator.comparingLong(entry -> entry.getValue().resetAtEpochSecond()))
            .limit(overflow)
            .map(Map.Entry::getKey)
            .toList()
            .forEach(windows::remove);
    }

    int trackedIdentityCount() {
        return windows.size();
    }

    private String identity(HttpServletRequest request) {
        AuthUser user = AuthContext.currentUser();
        if (user != null && user.username() != null && !user.username().isBlank()) {
            return "user:" + user.username().trim();
        }
        String address = request.getRemoteAddr();
        return "ip:" + (address == null || address.isBlank() ? "unknown" : address.trim());
    }

    private record Window(int count, long resetAtEpochSecond) {
    }

    private record Decision(boolean allowed, int remaining, long resetAtEpochSecond, int retryAfterSeconds) {
    }
}
