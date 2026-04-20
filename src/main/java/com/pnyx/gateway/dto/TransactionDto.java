package com.pnyx.gateway.dto;

public record TransactionDto(
    String txid,
    String sender,
    String recipient,
    long amount,
    long fee,
    String timestamp,
    String signature,
    String type,
    String metadata
) {}