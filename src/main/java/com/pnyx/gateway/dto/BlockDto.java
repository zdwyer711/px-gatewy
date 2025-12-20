package com.pnyx.gateway.dto;

import java.util.List;

public record BlockDto(
    String hash,
    long index,
    long timestamp,
    List<TransactionDto> transactions
) {}