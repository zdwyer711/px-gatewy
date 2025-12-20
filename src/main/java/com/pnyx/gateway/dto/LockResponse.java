package com.pnyx.gateway.dto;

public record LockResponse(
    String txid,
    boolean ok,
    String reason
) {}