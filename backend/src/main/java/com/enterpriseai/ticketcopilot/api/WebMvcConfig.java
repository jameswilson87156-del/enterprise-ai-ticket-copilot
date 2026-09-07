package com.enterpriseai.ticketcopilot.api;

import com.enterpriseai.ticketcopilot.auth.AuthInterceptor;
import com.enterpriseai.ticketcopilot.auth.AiRunRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AiRunRateLimitInterceptor aiRunRateLimitInterceptor;

    public WebMvcConfig(AuthInterceptor authInterceptor, AiRunRateLimitInterceptor aiRunRateLimitInterceptor) {
        this.authInterceptor = authInterceptor;
        this.aiRunRateLimitInterceptor = aiRunRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns("/api/**");
        registry.addInterceptor(aiRunRateLimitInterceptor)
            .addPathPatterns("/api/tickets/*/run-copilot");
    }
}
