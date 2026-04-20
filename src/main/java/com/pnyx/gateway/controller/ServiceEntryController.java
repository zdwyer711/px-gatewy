package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.ServiceEntryRequest;
import com.pnyx.gateway.model.ServiceEntry;
import com.pnyx.gateway.service.ServiceEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceEntryController {

    private final ServiceEntryService serviceEntryService;

    public ServiceEntryController(ServiceEntryService serviceEntryService) {
        this.serviceEntryService = serviceEntryService;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ServiceEntryRequest request, Principal principal) {
        try {
            ServiceEntry entry = serviceEntryService.createServiceEntry(principal.getName(), request);
            return ResponseEntity.ok(entry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/vehicle/{vin}")
    public ResponseEntity<List<ServiceEntry>> getByVin(@PathVariable String vin) {
        return ResponseEntity.ok(serviceEntryService.getEntriesByVin(vin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return serviceEntryService.getEntryById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ServiceEntry>> getMine(Principal principal) {
        return ResponseEntity.ok(serviceEntryService.getEntriesByUsername(principal.getName()));
    }
}
