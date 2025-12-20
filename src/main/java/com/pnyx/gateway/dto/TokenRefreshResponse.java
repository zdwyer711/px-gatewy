package com.pnyx.gateway.dto;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String type
) {
    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}