package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.service.LedgerClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/transactions")
public class TransactionController {

    private final LedgerClientService ledgerClientService;

    public TransactionController(LedgerClientService ledgerClientService) {
        this.ledgerClientService = ledgerClientService;
    }

    @GetMapping("/{txid}")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable String txid) {
        try {
            TransactionDto transaction = ledgerClientService.getTransaction(txid);
            return ResponseEntity.ok(transaction);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }
}