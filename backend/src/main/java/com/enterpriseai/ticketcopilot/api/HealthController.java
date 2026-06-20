package com.enterpriseai.ticketcopilot.api;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = "application/json;charset=UTF-8")
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "enterprise-ai-ticket-copilot",
            "phase", "2",
            "timestamp", OffsetDateTime.now().toString()
        );
    }
}
