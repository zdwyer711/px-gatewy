package com.pnyx.gateway.dto;

public record WalletDto(
    String walletId,
    String publicKey,
    long balance,
    long nonce
) {}