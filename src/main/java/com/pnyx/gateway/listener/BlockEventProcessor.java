package com.pnyx.gateway.listener;

import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.LedgerClientService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy; // Import this
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable; // Import this

import java.util.Optional;

@Component
@ConditionalOnProperty(
	    name = "px-gateway.listener.enabled", 
	    havingValue = "true", 
	    matchIfMissing = true
	)
public class BlockEventProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BlockEventProcessor.class);

    private final LedgerClientService ledgerClient;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Hold a reference to the active subscription
    private Disposable subscription; 

    public BlockEventProcessor(LedgerClientService ledgerClient, 
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.ledgerClient = ledgerClient;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        logger.info("Starting SSE Subscription to Ledger Node...");
        
        // Capture the Disposable
        this.subscription = ledgerClient.subscribeToNodeEvents()
                .doOnNext(this::handleEvent)
                .doOnError(e -> logger.error("SSE Error: ", e))
                .retry(Long.MAX_VALUE) 
                .subscribe();
    }

    // --- NEW: Cleanup Method ---
    @PreDestroy
    public void cleanup() {
        if (this.subscription != null && !this.subscription.isDisposed()) {
            logger.info("Closing SSE Subscription...");
            this.subscription.dispose();
        }
    }

    private void handleEvent(String eventName) {
        // ... same logic as before ...
        logger.info("Received Node Event: {}", eventName);

        if ("BLOCK_MINED".equals(eventName.trim())) {
            processNewBlock();
        }
    }

    private void processNewBlock() {
        // ... same logic as before ...
        try {
            BlockDto block = ledgerClient.getLatestBlock();
            logger.info("Processing Block #{} with {} transactions", block.index(), block.transactions().size());

            for (TransactionDto tx : block.transactions()) {
                Optional<User> recipientUser = userRepository.findByWalletId(tx.recipient());
                
                recipientUser.ifPresent(user -> {
                    logger.info("Alerting User {} of incoming transaction {}", user.getUsername(), tx.txid());
                    String destination = "/topic/wallets/" + user.getWalletId();
                    String message = "Received " + tx.amount() + " coins!";
                    messagingTemplate.convertAndSend(destination, message);
                });
            }
        } catch (Exception e) {
            logger.error("Error processing block", e);
        }
    }
}