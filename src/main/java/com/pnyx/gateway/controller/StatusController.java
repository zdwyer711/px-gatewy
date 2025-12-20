package com.pnyx.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<Map<String, String>> getAppStatus() {
        return ResponseEntity.ok(Collections.singletonMap("status", "UP"));
    }
}