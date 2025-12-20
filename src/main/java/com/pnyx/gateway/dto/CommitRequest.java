package com.pnyx.gateway.dto;

public record CommitRequest(
    String txid,
    String walletId
) {}