package com.pnyx.gateway.service;

import com.pnyx.gateway.dto.*;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64; // Standard Java Import
import java.util.List;

@Service
public class LedgerClientService {

    private final RestClient restClient;
    private final WebClient sseWebClient;

    public LedgerClientService(RestClient.Builder builder, 
    		WebClient.Builder webClientBuilder, @Value("${px-ledger.url}") String ledgerUrl) {
        // Standard Java Base64 Encoding
        String auth = "admin:admin";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        this.restClient = builder
                .baseUrl(ledgerUrl)
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
        
        this.sseWebClient = webClientBuilder
                .baseUrl(ledgerUrl)
                .defaultHeader("Authorization", "Basic " + encodedAuth)
                .build();
    }

    public String createWalletProxy(CreateWalletRequest request) {
        return restClient.post()
                .uri("/v1/api/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);
    }

    public LockResponse requestLockProxy(LockRequest request) {
        return restClient.post()
                .uri("/v1/api/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(LockResponse.class);
    }

    public CommitAck commitTransactionProxy(CommitRequest request) {
        return restClient.post()
                .uri("/v1/api/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CommitAck.class);
    }
    
    public Long getWalletBalanceProxy(String walletId) {
        return restClient.get()
                .uri("/v1/api/wallets/{walletId}/balance", walletId)
                .retrieve()
                .body(Long.class);
    }

    public List<WalletHistoryItem> getWalletHistoryProxy(String walletId) {
        return restClient.get()
                .uri("/v1/api/wallets/{walletId}/history", walletId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<WalletHistoryItem>>() {});
    }
    
    public Flux<String> subscribeToNodeEvents() {
        return sseWebClient.get()
                .uri("/v1/api/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class); // We expect simple strings like "BLOCK_MINED"
    }

    public BlockDto getLatestBlock() {
        return restClient.get()
                .uri("/v1/api/blocks/latest") // Assuming this endpoint exists on your Node
                .retrieve()
                .body(BlockDto.class);
    }
}