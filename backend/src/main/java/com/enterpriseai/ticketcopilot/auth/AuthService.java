package com.enterpriseai.ticketcopilot.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.enterpriseai.ticketcopilot.dto.AuthResponse;
import com.enterpriseai.ticketcopilot.dto.LoginRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 8 * 60 * 60;
    private static final Map<String, DemoUser> DEMO_USERS = Map.of(
        "admin", new DemoUser("admin123", "平台管理员", "ADMIN"),
        "agent", new DemoUser("agent123", "支持专员", "AGENT"),
        "reviewer", new DemoUser("reviewer123", "审核专员", "REVIEWER"),
        "viewer", new DemoUser("viewer123", "只读观察员", "VIEWER")
    );

    private final ObjectMapper objectMapper;
    private final String signingKey;

    public AuthService(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.signingKey = environment.getProperty(
            "ticket.auth.jwt-signing-key",
            "demo-rbac-signing-key-change-before-production"
        );
    }

    public AuthResponse login(LoginRequest request) {
        DemoUser demoUser = DEMO_USERS.get(request.username());
        if (demoUser == null || !demoUser.password().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid demo username or password.");
        }
        AuthUser user = new AuthUser(request.username(), demoUser.displayName(), demoUser.role());
        return new AuthResponse(createToken(user), user);
    }

    public AuthUser parseBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bearer token is required.");
        }
        return parseToken(authorization.substring("Bearer ".length()).trim());
    }

    private String createToken(AuthUser user) {
        try {
            String header = objectMapper.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = objectMapper.writeValueAsString(Map.of(
                "sub", user.username(),
                "name", user.displayName(),
                "role", user.role(),
                "exp", Instant.now().plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond()
            ));
            String unsignedToken = base64Url(header) + "." + base64Url(payload);
            return unsignedToken + "." + sign(unsignedToken);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to create demo JWT.", exception);
        }
    }

    private AuthUser parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token format.");
        }
        String unsignedToken = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token signature.");
        }
        try {
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long expiresAt = payload.path("exp").asLong(0);
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired.");
            }
            return new AuthUser(
                payload.path("sub").asText(),
                payload.path("name").asText(),
                payload.path("role").asText()
            );
        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token payload.");
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign demo JWT.", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private record DemoUser(String password, String displayName, String role) {
    }
}
