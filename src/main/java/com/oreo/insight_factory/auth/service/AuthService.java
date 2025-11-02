package com.oreo.insight_factory.auth.service;
import org.springframework.context.annotation.Lazy;
import com.oreo.insight_factory.auth.dto.*;
import com.oreo.insight_factory.security.JwtService;
import com.oreo.insight_factory.users.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final @Lazy AuthenticationManager authenticationManager; // ✅ nuevo

    public User register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername()))
            throw new IllegalArgumentException("Username ya existe");
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email ya existe");
        if (req.getRole() == Role.BRANCH && (req.getBranch() == null || req.getBranch().isBlank()))
            throw new IllegalArgumentException("Branch requerido para usuarios BRANCH");

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .branch(req.getRole() == Role.CENTRAL ? null : req.getBranch())
                .build();

        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getUsername(),
                        req.getPassword()
                )
        );

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken(
                user.getUsername(),
                user.getRole().name(),
                user.getBranch()
        );

        return AuthResponse.builder()
                .token(token)
                .expiresIn(3600)
                .role(user.getRole().name())
                .branch(user.getBranch())
                .build();
    }
}
