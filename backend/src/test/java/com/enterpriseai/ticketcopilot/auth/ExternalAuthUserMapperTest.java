package com.enterpriseai.ticketcopilot.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.jwt.Jwt;

class ExternalAuthUserMapperTest {

    @Test
    void mapsCommonOidcClaimsToTheApplicationUserShape() {
        var environment = new MockEnvironment()
                .withProperty("ticket.auth.roles-claim", "roles")
                .withProperty("ticket.auth.principal-claim", "preferred_username");
        var mapper = new ExternalAuthUserMapper(environment);
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://id.example.test/realms/enterprise")
                .subject("subject-42")
                .issuedAt(Instant.now().minusSeconds(30))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("preferred_username", "agent@example.test")
                .claim("email", "agent@example.test")
                .claim("name", "Support Agent")
                .claim("roles", List.of("ROLE_agent", "reviewer"))
                .claim("groups", List.of("ticket-support"))
                .build();

        AuthUser user = mapper.fromJwt(jwt);

        assertEquals("agent@example.test", user.username());
        assertEquals("Support Agent", user.displayName());
        assertEquals("AGENT", user.role());
        assertTrue(user.hasAnyRole("AGENT"));
        assertTrue(user.hasAnyRole("REVIEWER"));
        assertTrue(user.roles().contains("TICKET-SUPPORT"));
    }
}
