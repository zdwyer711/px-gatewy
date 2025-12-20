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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    @Test
    public void testInit_ProcessesBlockMinedEvent() {
        // --- 1. Prepare Data ---
        String recipientWallet = "wallet-123";
        String txId = "tx-abc";
        
        User user = new User();
        user.setUsername("testUser");
        user.setWalletId(recipientWallet);

        TransactionDto tx = new TransactionDto(txId, "sender", recipientWallet, 50, 1000L);
        BlockDto block = new BlockDto("hash", 1, 1000L, List.of(tx));

        // --- 2. Mock Interactions ---
        
        // A. Mock the SSE Stream to emit one "BLOCK_MINED" event immediately
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just("BLOCK_MINED"));

        // B. Mock fetching the block details
        when(ledgerClient.getLatestBlock()).thenReturn(block);

        // C. Mock finding the user who owns the wallet
        when(userRepository.findByWalletId(recipientWallet)).thenReturn(Optional.of(user));

        // --- 3. Execute ---
        // Manually call init() which subscribes to the flux
        blockEventProcessor.init();

        // --- 4. Verify ---
        
        // Verify we fetched the block
        verify(ledgerClient, times(1)).getLatestBlock();
        
        // Verify we looked up the user
        verify(userRepository, times(1)).findByWalletId(recipientWallet);
        
        // CRITICAL: Verify the WebSocket message was sent
        verify(messagingTemplate).convertAndSend(
            eq("/topic/wallets/wallet-123"), 
            contains("Received 50 coins")
        );
    }

    @Test
    public void testInit_IgnoresIrrelevantEvents() {
        // Stream emits HEARTBEAT, not BLOCK_MINED
        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just("HEARTBEAT"));

        blockEventProcessor.init();

        // Ensure we did NOT try to fetch a block
        verify(ledgerClient, never()).getLatestBlock();
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    public void testProcessNewBlock_NoUserFound() {
        // Transaction exists, but NO user owns this wallet
        String unknownWallet = "ghost-wallet";
        TransactionDto tx = new TransactionDto("tx1", "sender", unknownWallet, 50, 1000L);
        BlockDto block = new BlockDto("hash", 1, 1000L, List.of(tx));

        when(ledgerClient.subscribeToNodeEvents()).thenReturn(Flux.just("BLOCK_MINED"));
        when(ledgerClient.getLatestBlock()).thenReturn(block);
        when(userRepository.findByWalletId(unknownWallet)).thenReturn(Optional.empty());

        blockEventProcessor.init();

        // Verify we fetched block but sent NO message
        verify(ledgerClient).getLatestBlock();
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }
}