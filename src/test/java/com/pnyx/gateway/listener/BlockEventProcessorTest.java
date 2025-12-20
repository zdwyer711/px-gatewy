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

    // --- NEW: Helper to simulate the real JSON event ---
    private static final String JSON_EVENT_PAYLOAD = "{\"index\": 1465, \"hash\": \"00abc...\", \"txCount\": 1}";

    @Test
    public void testInit_ProcessesBlockMinedEvent() {
        // --- 1. Prepare Data ---
        String recipientWallet = "wallet-123";
        String txId = "tx-abc";
        
        User user = new User();
        user.setUsername("testUser");
        user.setWalletId(recipientWallet);

        // Updated timestamps to Strings (ISO format)
        TransactionDto tx = new TransactionDto(txId, "sender", recipientWallet, 50, "2025-12-20T22:09:49.740Z");
        
        // FIX: Constructor now likely expects String timestamp due to DTO change
        BlockDto block = new BlockDto("hash", 1, 2025, List.of(tx));

        // --- 2. Mock Interactions ---
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just(JSON_EVENT_PAYLOAD));
        when(ledgerClient.getLatestBlock()).thenReturn(block);
        when(userRepository.findByWalletId(recipientWallet)).thenReturn(Optional.of(user));
        // Note: You might want to mock findByWalletId for the "sender" too if testing that flow
        when(userRepository.findByWalletId("sender")).thenReturn(Optional.empty()); 

        // --- 3. Execute ---
        blockEventProcessor.init();

        // --- 4. Verify with Awaitility ---
        // We wait up to 2 seconds for the background thread to finish the call
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
             verify(ledgerClient, times(1)).getLatestBlock();
        });

        // Now we can verify the rest safely
        verify(userRepository).findByWalletId(recipientWallet);
        
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

        verify(ledgerClient, never()).getLatestBlock();
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    public void testProcessNewBlock_NoUserFound() {
        // 1. Prepare Data
        String unknownWallet = "ghost-wallet";
        TransactionDto tx = new TransactionDto("tx1", "sender", unknownWallet, 50, "2025-12-20T22:09:49.740Z");
        
        // Ensure BlockDto constructor matches your class (Long timestamp)
        BlockDto block = new BlockDto("hash", 1, 2025L, List.of(tx));

        // 2. Mock Interactions
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just(JSON_EVENT_PAYLOAD));
        when(ledgerClient.getLatestBlock()).thenReturn(block);
        
        // Mock that neither recipient nor sender exists
        when(userRepository.findByWalletId(unknownWallet)).thenReturn(Optional.empty());
        when(userRepository.findByWalletId("sender")).thenReturn(Optional.empty());

        // 3. Execute
        blockEventProcessor.init();

        // 4. Verify with Awaitility
        // Wait until getLatestBlock is called (this proves the async thread ran)
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
             verify(ledgerClient, times(1)).getLatestBlock();
        });

        // Now verify NO message was sent
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }
}