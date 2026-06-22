package com.enterpriseai.ticketcopilot.api;

import java.time.OffsetDateTime;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatusCode;

public record ApiErrorResponse(
    int code,
    String message,
    String path,
    String timestamp
) {
    public static ApiErrorResponse of(HttpStatusCode status, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
            status.value(),
            message,
            request.getRequestURI(),
            OffsetDateTime.now().toString()
        );
    }
}
