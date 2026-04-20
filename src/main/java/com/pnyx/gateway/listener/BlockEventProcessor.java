package com.pnyx.gateway.listener;

import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.model.ServiceEntry;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.ServiceEntryRepository;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.LedgerClientService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

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
    private final ServiceEntryRepository serviceEntryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private Disposable subscription;

    public BlockEventProcessor(LedgerClientService ledgerClient,
                               UserRepository userRepository,
                               ServiceEntryRepository serviceEntryRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.ledgerClient = ledgerClient;
        this.userRepository = userRepository;
        this.serviceEntryRepository = serviceEntryRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostConstruct
    public void init() {
        logger.info("Starting SSE Subscription to Ledger Node...");
        
        this.subscription = ledgerClient.subscribeToNodeEvents()
                .publishOn(Schedulers.boundedElastic()) 
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

    private void handleEvent(String eventPayload) {
        // ... same logic as before ...
        logger.info("Received Node Event: {}", eventPayload);

        if (eventPayload.contains("\"hash\"") && eventPayload.contains("\"index\"")) {
            processNewBlock();
        }
    }

    private void processNewBlock() {
        // ... same logic as before ...
        try {
            BlockDto block = ledgerClient.getLatestBlock();
            logger.info("Processing Block #{} with {} transactions", block.index(), block.transactions().size());

            for (TransactionDto tx : block.transactions()) {
            	logger.info("Processing sender #{} and recipient {} ", tx.sender(), tx.recipient());
                // --- 1. Alert Recipient (Existing) ---
                userRepository.findByWalletId(tx.recipient()).ifPresent(user -> {
                    logger.info("Alerting Recipient {} of incoming transaction {}", user.getUsername(), tx.txid());
                    String destination = "/topic/wallets/" + user.getWalletId();
                    String message = "Received " + tx.amount() + " coins!";
                    messagingTemplate.convertAndSend(destination, message);
                });

                // --- 2. Alert Sender (NEW) ---
                userRepository.findByWalletId(tx.sender()).ifPresent(user -> {
                    logger.info("Alerting Sender {} of outgoing transaction {}", user.getUsername(), tx.txid());
                    String destination = "/topic/wallets/" + user.getWalletId();
                    String message = "Sent " + tx.amount() + " coins!";
                    messagingTemplate.convertAndSend(destination, message);
                });

                // --- 3. Confirm SERVICE_ENTRY ---
                if ("SERVICE_ENTRY".equals(tx.type())) {
                    serviceEntryRepository.findByBlockchainTxid(tx.txid()).ifPresent(entry -> {
                        logger.info("Confirming SERVICE_ENTRY {} for vin {}", tx.txid(), entry.getVin());
                        entry.setStatus("CONFIRMED");
                        serviceEntryRepository.save(entry);
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error processing block", e);
        }
    }
}