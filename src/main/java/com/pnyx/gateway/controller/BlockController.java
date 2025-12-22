package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.service.LedgerClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/proxy/blocks")
public class BlockController {

    private final LedgerClientService ledgerClientService;

    public BlockController(LedgerClientService ledgerClientService) {
        this.ledgerClientService = ledgerClientService;
    }

    /**
     * Proxy endpoint to get the latest blocks.
     * Usage: GET /api/blocks/latest?depth=10
     */
    @GetMapping("/latest")
    public ResponseEntity<List<BlockDto>> getLatestBlocks(
            @RequestParam(defaultValue = "10") int depth) {
        
        List<BlockDto> blocks = ledgerClientService.getRecentBlocks(depth);
        return ResponseEntity.ok(blocks);
    }
    

    @GetMapping("/{hashOrIndex}")
    public ResponseEntity<BlockDto> getBlock(@PathVariable String hashOrIndex) {
        try {
            BlockDto block = ledgerClientService.getBlockByHashOrIndex(hashOrIndex);
            return ResponseEntity.ok(block);
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }
}