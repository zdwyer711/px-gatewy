package com.pnyx.gateway.dto;

import java.util.List;

public record BlockDto(
	    String hash,
	    long index,
	    long timestamp, // Block timestamp is Long (epoch)
	    long nonce,
	    String previousHash,
	    String minerAddress,
	    String data, // The stringified JSON content
	    List<TransactionDto> transactions
	) {}