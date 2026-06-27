package com.enterpriseai.ticketcopilot.api;

import com.enterpriseai.ticketcopilot.auth.AuthContext;
import com.enterpriseai.ticketcopilot.auth.AuthService;
import com.enterpriseai.ticketcopilot.auth.AuthUser;
import com.enterpriseai.ticketcopilot.dto.AuthResponse;
import com.enterpriseai.ticketcopilot.dto.LoginRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/auth", produces = "application/json;charset=UTF-8")
@Tag(name = "Auth", description = "JWT + RBAC demo 登录和当前用户接口")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthUser me() {
        return AuthContext.requireUser();
    }
}
