package com.enterpriseai.ticketcopilot.auth;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public record AuthUser(String username, String displayName, String role, Set<String> roles) {

    public AuthUser(String username, String displayName, String role) {
        this(username, displayName, role, Set.of(role == null ? "" : role));
    }

    public AuthUser {
        Set<String> normalized = new LinkedHashSet<>();
        if (roles != null) {
            roles.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase())
                .forEach(normalized::add);
        }
        if (role != null && !role.isBlank()) {
            normalized.add(role.trim().toUpperCase());
            role = role.trim().toUpperCase();
        }
        roles = Set.copyOf(normalized);
    }

    public boolean hasAnyRole(String... allowedRoles) {
        return Arrays.stream(allowedRoles)
            .map(value -> value == null ? "" : value.trim().toUpperCase())
            .anyMatch(roles::contains);
    }
}
