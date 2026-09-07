package com.enterpriseai.ticketcopilot.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ExternalAuthUserMapper externalAuthUserMapper;
    private final Environment environment;

    public AuthInterceptor(AuthService authService, ExternalAuthUserMapper externalAuthUserMapper, Environment environment) {
        this.authService = authService;
        this.externalAuthUserMapper = externalAuthUserMapper;
        this.environment = environment;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request.getRequestURI())) {
            return true;
        }
        AuthContext.set(isOidcMode()
            ? externalAuthUserMapper.fromSecurityContext()
            : authService.parseBearerToken(request.getHeader("Authorization")));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private boolean isPublicPath(String uri) {
        return uri.equals("/api/health")
            || uri.equals("/api/auth/login");
    }

    private boolean isOidcMode() {
        return "OIDC".equalsIgnoreCase(environment.getProperty("ticket.auth.mode", "DEMO"));
    }
}
