package com.enterpriseai.ticketcopilot.api;

import java.time.OffsetDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api", produces = "application/json;charset=UTF-8")
@Tag(name = "Health", description = "服务健康检查接口")
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
