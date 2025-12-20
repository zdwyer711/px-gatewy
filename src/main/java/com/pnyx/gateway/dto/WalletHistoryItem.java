package com.pnyx.gateway.dto;

public record WalletHistoryItem(
    String txid,
    long timestamp,
    String type,        // "SENT" or "RECEIVED"
    String counterparty,
    long amount
) {}