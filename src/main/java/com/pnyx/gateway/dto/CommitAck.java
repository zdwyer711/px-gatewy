package com.pnyx.gateway.dto;

public record CommitAck(
    String txid,
    boolean ok
) {}