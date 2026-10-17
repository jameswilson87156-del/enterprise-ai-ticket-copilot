package com.enterpriseai.ticketcopilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 判断真实 LLM 是否可用：仅当 AI_DASHSCOPE_API_KEY 已配置且非占位符时为 true。
 */
@Component
public class AiAvailability {

    private final String apiKey;

    public AiAvailability(@Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean llmConfigured() {
        return !apiKey.isEmpty() && !apiKey.startsWith("placeholder");
    }
}
