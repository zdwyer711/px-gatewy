package com.pnyx.gateway.dto;

public record TransactionDto(
    String txid,
    String sender,
    String recipient,
    long amount,
    String timestamp
) {}