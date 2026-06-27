package com.enterpriseai.ticketcopilot.auth;

import java.util.Arrays;

public record AuthUser(String username, String displayName, String role) {

    public boolean hasAnyRole(String... allowedRoles) {
        return Arrays.stream(allowedRoles).anyMatch(role::equals);
    }
}
