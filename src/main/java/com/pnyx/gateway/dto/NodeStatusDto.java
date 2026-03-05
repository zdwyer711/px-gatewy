package com.pnyx.gateway.dto;

public record NodeStatusDto(
    String nodeId,
    long currentBlockHeight,
    String lastBlockHash,
    int pendingTxCount,
    int peerCount,
    long timestamp
) {}