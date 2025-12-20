package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.*;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.LedgerClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/proxy")
public class WalletProxyController {

    private final LedgerClientService ledgerClientService;
    private final UserRepository userRepository;

    public WalletProxyController(LedgerClientService ledgerClientService, UserRepository userRepository) {
        this.ledgerClientService = ledgerClientService;
        this.userRepository = userRepository;
    }

    @PostMapping("/wallets")
    public ResponseEntity<String> createWallet(
            @Valid @RequestBody CreateWalletRequest request, 
            Principal principal // Spring injects the logged-in user here
    ) {
        try {
            // 1. Call Ledger Service
            String walletId = ledgerClientService.createWalletProxy(request);

            // 2. If successful, find the local user
            // principal.getName() returns the username from the JWT
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // 3. Update and Save
            user.setWalletId(walletId);
            userRepository.save(user);

            // 4. Return result
            return ResponseEntity.ok(walletId);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred connecting to the Ledger.");
        }
    }
    
    // --- NEW: Lock Proxy ---
    @PostMapping("/lock")
    public ResponseEntity<LockResponse> requestLock(@RequestBody LockRequest request) {
        try {
            LockResponse response = ledgerClientService.requestLockProxy(request);
            // If the ledger returned a LockResponse successfully (even if it's "false"), 
            // the service returns it. If the Ledger threw a 409, it's caught below.
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            // Ledger returned 409 or 400. We try to map the body back to LockResponse if possible,
            // or just re-throw the status.
            try {
                // If Ledger sent a JSON body with the error (e.g. on 409)
                return ResponseEntity.status(e.getStatusCode())
                        .body(e.getResponseBodyAs(LockResponse.class));
            } catch (Exception parseEx) {
                // If body wasn't JSON, return error structure
                return ResponseEntity.status(e.getStatusCode())
                        .body(new LockResponse(request.txid(), false, e.getStatusText()));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new LockResponse(request != null ? request.txid() : null, false, "Gateway Error: " + e.getMessage()));
        }
    }

    // --- NEW: Commit Proxy ---
    @PostMapping("/commit")
    public ResponseEntity<CommitAck> commitTransaction(@RequestBody CommitRequest request) {
        try {
            CommitAck ack = ledgerClientService.commitTransactionProxy(request);
            return ResponseEntity.ok(ack);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
             // Try to parse the specific Ack from error body if available
            try {
                return ResponseEntity.status(e.getStatusCode())
                        .body(e.getResponseBodyAs(CommitAck.class));
            } catch (Exception parseEx) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(new CommitAck(request.txid(), false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new CommitAck(request != null ? request.txid() : null, false));
        }
    }
    
    // --- NEW: Balance Endpoint ---
    @GetMapping("/wallets/{walletId}/balance")
    public ResponseEntity<?> getWalletBalance(
            @PathVariable String walletId,
            Principal principal
    ) {
        try {
            // 1. Security Check: Verify ownership
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (user.getWalletId() == null || !user.getWalletId().equals(walletId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to view the balance of this wallet.");
            }

            // 2. Call Ledger
            Long balance = ledgerClientService.getWalletBalanceProxy(walletId);
            return ResponseEntity.ok(balance);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Handles 404 (Not Found) if wallet doesn't exist on ledger
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching balance: " + e.getMessage());
        }
    }
    
    // --- NEW: History Endpoint ---
    @GetMapping("/wallets/{walletId}/history")
    public ResponseEntity<?> getWalletHistory(
            @PathVariable String walletId,
            Principal principal
    ) {
        try {
            // Optional Security Check: Ensure the logged-in user actually owns this wallet
            // If you want to allow anyone to view any history (like a block explorer), remove this block.
            User user = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            if (user.getWalletId() == null || !user.getWalletId().equals(walletId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You are not authorized to view history for this wallet.");
            }

            // Call Ledger
            List<WalletHistoryItem> history = ledgerClientService.getWalletHistoryProxy(walletId);
            return ResponseEntity.ok(history);

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching history: " + e.getMessage());
        }
    }
}