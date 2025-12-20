package com.pnyx.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWalletRequest(
    @NotBlank(message = "Public Key is required")
    String publicKey
) {}