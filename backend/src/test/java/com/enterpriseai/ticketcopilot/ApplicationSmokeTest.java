package com.enterpriseai.ticketcopilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;

import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;

/**
 * 冒烟测试：不配置模型 API Key（使用占位符）时应用必须能正常启动，
 * 原有 REST 接口不回归，MCP SSE 端点完成注册。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("smoke")
class ApplicationSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebMvcSseServerTransportProvider sseTransportProvider;

    @Test
    void healthEndpointStillWorks() {
        String body = restTemplate.getForObject("/api/health", String.class);
        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void ticketListEndpointStillWorks() {
        assertThat(restTemplate.getForEntity("/api/tickets", String.class).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void mcpSseEndpointIsRegistered() throws Exception {
        // SSE 为长连接：只校验响应头后立即断开，避免阻塞测试线程
        HttpURLConnection connection = (HttpURLConnection) new URI(restTemplate.getRootUri() + "/sse").toURL().openConnection();
        try {
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            assertThat(connection.getResponseCode()).isEqualTo(200);
            assertThat(connection.getContentType()).contains("text/event-stream");
        } finally {
            connection.disconnect();
            // 主动关闭 MCP SSE 传输，避免非守护线程阻塞测试 JVM 退出
            sseTransportProvider.closeGracefully().block(Duration.ofSeconds(2));
        }
    }
}
