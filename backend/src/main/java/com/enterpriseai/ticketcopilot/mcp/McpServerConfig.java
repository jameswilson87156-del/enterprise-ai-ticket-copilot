package com.enterpriseai.ticketcopilot.mcp;

import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server 装配：把 @Tool 注解的 TicketMcpTools 注册为 MCP 工具。
 * 传输协议为 SSE，端点见 application.yml（/sse + /mcp/message）。
 */
@Configuration
public class McpServerConfig {

    @Bean
    public MethodToolCallbackProvider ticketToolCallbackProvider(TicketMcpTools ticketMcpTools) {
        return MethodToolCallbackProvider.builder().toolObjects(ticketMcpTools).build();
    }
}
