package com.enterpriseai.ticketcopilot.auth;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Maps a verified OIDC JWT to the application's small, legacy-compatible user shape. */
@Component
public class ExternalAuthUserMapper {

    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "AGENT", "REVIEWER", "VIEWER");
    private final Environment environment;

    public ExternalAuthUserMapper(Environment environment) {
        this.environment = environment;
    }

    public AuthUser fromSecurityContext() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A verified OIDC token is required.");
        }
        return fromJwt(token.getToken());
    }

    public AuthUser fromJwt(Jwt jwt) {
        String principalClaim = environment.getProperty("ticket.auth.principal-claim", "sub");
        String username = firstText(jwt, principalClaim, "preferred_username", "email", "sub");
        String displayName = firstText(jwt, "name", "preferred_username", "email", "sub");
        Set<String> roles = extractRoles(jwt);
        String primaryRole = ROLE_PRIORITY.stream().filter(roles::contains).findFirst()
            .orElse(roles.stream().findFirst().orElse(""));
        return new AuthUser(username, displayName, primaryRole, roles);
    }

    private Set<String> extractRoles(Jwt jwt) {
        String rolesClaim = environment.getProperty("ticket.auth.roles-claim", "roles");
        Set<String> roles = new LinkedHashSet<>();
        addClaimValues(roles, jwt.getClaims().get(rolesClaim));
        addClaimValues(roles, jwt.getClaims().get("groups"));
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map) addClaimValues(roles, map.get("roles"));
        addClaimValues(roles, jwt.getClaims().get("scope"));
        return roles;
    }

    private void addClaimValues(Set<String> target, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> addClaimValues(target, item));
            return;
        }
        if (value instanceof String string) {
            for (String item : string.split("[ ,]")) {
                String normalized = normalizeRole(item);
                if (!normalized.isBlank()) target.add(normalized);
            }
        }
    }

    private String normalizeRole(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private String firstText(Jwt jwt, String... claims) {
        for (String claim : claims) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) return value;
        }
        return "external-user";
    }
}
