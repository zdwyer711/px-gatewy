package com.pnyx.gateway.controller;

import com.pnyx.gateway.service.LedgerClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proxy/node")
public class NodeController {

    private final LedgerClientService ledgerClientService;

    public NodeController(LedgerClientService ledgerClientService) {
        this.ledgerClientService = ledgerClientService;
    }

    @GetMapping("/network/hashrate")
    public ResponseEntity<Double> getNetworkHashrate() {
        Double hashrate = ledgerClientService.getNetworkHashrate();
        return ResponseEntity.ok(hashrate);
    }
}