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

    public List<WalletHistoryItem> getWalletHistoryProxy(String walletId, int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/wallets/{walletId}/history")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(walletId))
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
    	List<BlockDto> blocks = restClient.get()
                .uri("/v1/api/blocks/latest")
                .retrieve()
                .body(new ParameterizedTypeReference<List<BlockDto>>() {});

        // Return the first block from the list, or handle empty case
        if (blocks != null && !blocks.isEmpty()) {
            return blocks.get(0);
        }
        
        throw new RuntimeException("Ledger Node returned no blocks from /latest endpoint");
    }
    
    public Long getWalletNonceProxy(String walletId) {
        return restClient.get()
                .uri("/v1/api/wallets/{walletId}/nonce", walletId)
                .retrieve()
                .body(Long.class);
    }
    
    public List<BlockDto> getRecentBlocks(int depth) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/blocks/latest")
                        .queryParam("depth", depth)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<BlockDto>>() {});
    }
    
    public BlockDto getBlockByHashOrIndex(String hashOrIndex) {
        return restClient.get()
                .uri("/v1/api/blocks/{id}", hashOrIndex)
                .retrieve()
                .body(BlockDto.class);
    }
    
    public TransactionDto getTransaction(String txid) {
        return restClient.get()
                .uri("/v1/api/transactions/{txid}", txid)
                .retrieve()
                .body(TransactionDto.class);
    }
    
    public Double getNetworkHashrate() {
        return restClient.get()
                .uri("/v1/api/node/network/hashrate")
                .retrieve()
                .body(Double.class);
    }
    
    public List<WalletDto> getTopWallets(int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/wallets/top")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<WalletDto>>() {});
    }
    
    public List<TransactionDto> getMempoolTransactions() {
        return restClient.get()
                .uri("/v1/api/node/mempool")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransactionDto>>() {});
    }
    
    public List<String> getPeers() {
        return restClient.get()
                .uri("/v1/api/node/peers")
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});
    }
    
    public NodeStatusDto getNodeStatus() {
        return restClient.get()
                .uri("/v1/api/node/status")
                .retrieve()
                .body(NodeStatusDto.class);
    }
}