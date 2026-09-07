package com.enterpriseai.ticketcopilot.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Stateless JWT resource-server mode used by staging and production profiles. */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(prefix = "ticket.auth", name = "mode", havingValue = "OIDC")
public class OidcSecurityConfig {

    private final Environment environment;

    public OidcSecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        String issuer = required("ticket.auth.issuer-uri");
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        String audience = environment.getProperty("ticket.auth.audience", "").trim();
        if (audience.isBlank()) return decoder;
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(audience)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing.", null));
        if (!(decoder instanceof NimbusJwtDecoder nimbusDecoder)) {
            throw new IllegalStateException("OIDC decoder does not support audience validation.");
        }
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        return nimbusDecoder;
    }

    @Bean
    SecurityFilterChain oidcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/health").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(jsonEntryPoint(401, "Authentication is required."))
                .accessDeniedHandler(jsonDeniedHandler()))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter())));
        return http.build();
    }

    private AuthenticationEntryPoint jsonEntryPoint(int status, String message) {
        return (request, response, exception) -> {
            response.setStatus(status);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"" + message + "\"}");
        };
    }

    private AccessDeniedHandler jsonDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(403);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Insufficient permissions.\"}");
        };
    }

    private String required(String key) {
        String value = environment.getProperty(key, "").trim();
        if (value.isBlank()) throw new IllegalStateException(key + " must be configured when OIDC auth is enabled.");
        return value;
    }
}
