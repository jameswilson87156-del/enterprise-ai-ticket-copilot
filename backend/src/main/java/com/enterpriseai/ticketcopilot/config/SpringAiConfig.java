package com.enterpriseai.ticketcopilot.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring AI 定制配置：OpenAI 兼容端点 + 15s 只读超时（覆盖默认无超时的 RestClient）。
 */
@Configuration
public class SpringAiConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    /** 自定义 OpenAiApi 覆盖 starter 默认 bean，统一为 chat/embedding 两个通道设置超时 */
    @Bean
    public OpenAiApi openAiApi(
        @Value("${spring.ai.openai.base-url}") String baseUrl,
        @Value("${spring.ai.openai.api-key}") String apiKey
    ) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return OpenAiApi.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
            .build();
    }
}
