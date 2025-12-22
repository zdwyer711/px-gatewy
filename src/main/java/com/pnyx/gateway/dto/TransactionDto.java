package com.pnyx.gateway.dto;

public record TransactionDto(
    String txid,
    String sender,
    String recipient,
    long amount,
    long fee,         // NEW
    String timestamp, // String (ISO-8601)
    String signature  // NEW
) {}