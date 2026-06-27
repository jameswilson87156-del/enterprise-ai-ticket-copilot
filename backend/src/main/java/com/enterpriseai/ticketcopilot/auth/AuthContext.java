package com.enterpriseai.ticketcopilot.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AuthContext {

    private static final ThreadLocal<AuthUser> CURRENT_USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT_USER.set(user);
    }

    public static AuthUser currentUser() {
        return CURRENT_USER.get();
    }

    public static AuthUser requireUser() {
        AuthUser user = currentUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication token is required.");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
