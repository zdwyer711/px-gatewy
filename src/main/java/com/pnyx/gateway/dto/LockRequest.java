package com.pnyx.gateway.dto;

public record LockRequest(
    String txid,
    String walletId,
    String recipient,
    String requester,
    long amount,
    String signature
) {}