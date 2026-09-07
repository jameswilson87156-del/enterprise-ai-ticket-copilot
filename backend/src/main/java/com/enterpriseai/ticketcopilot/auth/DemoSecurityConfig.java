package com.enterpriseai.ticketcopilot.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Keeps the existing interceptor-based Demo JWT flow unchanged in local/test mode. */
@Configuration
@ConditionalOnProperty(prefix = "ticket.auth", name = "mode", havingValue = "DEMO", matchIfMissing = true)
public class DemoSecurityConfig {
    @Bean
    SecurityFilterChain demoSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .build();
    }
}
