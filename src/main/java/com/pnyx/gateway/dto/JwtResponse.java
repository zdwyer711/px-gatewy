package com.pnyx.gateway.dto;

public record JwtResponse(
    String accessToken,
    String refreshToken,
    String type
) {
    public JwtResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}