package com.pnyx.gateway.dto;

public record JwtResponse(
    String accessToken,
    String refreshToken,
    String type,
    String role,
    String walletId
) {
    public JwtResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", "OWNER", null);
    }
}