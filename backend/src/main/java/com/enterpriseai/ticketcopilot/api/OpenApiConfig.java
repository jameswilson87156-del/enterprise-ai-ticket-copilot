package com.enterpriseai.ticketcopilot.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI enterpriseAiTicketCopilotOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Enterprise AI Ticket Copilot API")
                .version("v1")
                .description("""
                    企业工单工作流系统 API，包含工单录入、规则引擎辅助分类、知识库匹配、状态流转和测试证据说明。
                    当前项目使用规则引擎辅助分类、关键词知识匹配和模板化建议草稿，不依赖真实 LLM API。
                    """));
    }
}
