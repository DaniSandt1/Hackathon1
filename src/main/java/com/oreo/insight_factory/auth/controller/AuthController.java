package com.oreo.insight_factory.auth.controller;

import com.oreo.insight_factory.auth.dto.*;
import com.oreo.insight_factory.auth.service.AuthService;
import com.oreo.insight_factory.users.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(201).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok","auth-controller");
    }


}
