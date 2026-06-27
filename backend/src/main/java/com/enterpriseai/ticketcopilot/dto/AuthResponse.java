package com.enterpriseai.ticketcopilot.dto;

import com.enterpriseai.ticketcopilot.auth.AuthUser;

public record AuthResponse(String token, AuthUser user) {
}
