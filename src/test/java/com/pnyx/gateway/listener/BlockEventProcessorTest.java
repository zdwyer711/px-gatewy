package com.pnyx.gateway.listener;

import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.LedgerClientService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BlockEventProcessorTest {

    @Mock
    private LedgerClientService ledgerClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BlockEventProcessor blockEventProcessor;

    // Helper to simulate the real JSON event from the node
    private static final String JSON_EVENT_PAYLOAD = "{\"index\": 1465, \"hash\": \"00abc...\", \"txCount\": 1}";

    @Test
    public void testInit_ProcessesBlockMinedEvent() {
        // --- 1. Prepare Data ---
        String recipientWallet = "wallet-123";
        String txId = "tx-abc";
        
        User user = new User();
        user.setUsername("testUser");
        user.setWalletId(recipientWallet);

        // Transaction uses String timestamp
        TransactionDto tx = new TransactionDto(txId, "sender", recipientWallet, 50, 0L,  "2025-12-20T22:09:49.740Z", "sig");
        // FIX: Updated Constructor to match 8-parameter signature
        // (hash, index, timestamp, nonce, previousHash, minerAddress, data, transactions)
        BlockDto block = new BlockDto(
            "hash", 
            1L, 
            1766369234110L,  // Block uses Long timestamp
            0L,              // nonce
            "prevHash",      // previousHash
            "minerAddr",     // minerAddress
            "someData",      // data
            List.of(tx)
        );

        // --- 2. Mock Interactions ---
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just(JSON_EVENT_PAYLOAD));
        when(ledgerClient.getLatestBlock()).thenReturn(block);
        
        // Mock Recipient Found
        when(userRepository.findByWalletId(recipientWallet)).thenReturn(Optional.of(user));
        // Mock Sender Not Found (to prevent NPE if logic checks sender)
        when(userRepository.findByWalletId("sender")).thenReturn(Optional.empty()); 

        // --- 3. Execute ---
        blockEventProcessor.init();

        // --- 4. Verify with Awaitility ---
        // Wait for background thread to complete
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
             verify(ledgerClient, times(1)).getLatestBlock();
        });

        // Verify we looked up the user
        verify(userRepository).findByWalletId(recipientWallet);
        
        // Verify the message was sent
        verify(messagingTemplate).convertAndSend(
            eq("/topic/wallets/wallet-123"), 
            contains("Received 50 coins")
        );
    }

    @Test
    public void testInit_IgnoresIrrelevantEvents() {
        // Stream emits a plain heartbeat string (does not contain "hash" or "index")
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just("HEARTBEAT"));

        blockEventProcessor.init();

        // Ensure we did NOT try to fetch a block
        verify(ledgerClient, never()).getLatestBlock();
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    public void testProcessNewBlock_NoUserFound() {
        // 1. Prepare Data
        String unknownWallet = "ghost-wallet";
        TransactionDto tx = new TransactionDto("tx1", "sender",  unknownWallet, 100, 0L, "2025-12-21T10:00:00Z", "signature");
        
        // FIX: Updated Constructor here as well
        BlockDto block = new BlockDto(
            "hash", 
            1L, 
            1766369234110L, 
            0L, 
            "prevHash", 
            "minerAddr", 
            "someData", 
            List.of(tx)
        );

        // 2. Mock Interactions
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just(JSON_EVENT_PAYLOAD));
        when(ledgerClient.getLatestBlock()).thenReturn(block);
        
        // Mock that neither recipient nor sender exists
        when(userRepository.findByWalletId(unknownWallet)).thenReturn(Optional.empty());
        when(userRepository.findByWalletId("sender")).thenReturn(Optional.empty());

        // 3. Execute
        blockEventProcessor.init();

        // 4. Verify with Awaitility
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
             verify(ledgerClient, times(1)).getLatestBlock();
        });

        // Verify NO message was sent
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }
}