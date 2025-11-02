package com.oreo.insight_factory.auth.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private long expiresIn;
    private String role;
    private String branch;
}
