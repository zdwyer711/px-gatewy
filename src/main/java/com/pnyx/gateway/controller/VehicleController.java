package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.VehicleRegistrationRequest;
import com.pnyx.gateway.model.Vehicle;
import com.pnyx.gateway.service.ServiceEntryService;
import com.pnyx.gateway.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final ServiceEntryService serviceEntryService;

    public VehicleController(VehicleService vehicleService, ServiceEntryService serviceEntryService) {
        this.vehicleService = vehicleService;
        this.serviceEntryService = serviceEntryService;
    }

    @PostMapping
    public ResponseEntity<?> registerVehicle(
            @Valid @RequestBody VehicleRegistrationRequest request,
            Principal principal) {
        try {
            Vehicle vehicle = vehicleService.registerVehicle(principal.getName(), request);
            return ResponseEntity.ok(vehicle);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<Vehicle>> listMyVehicles(Principal principal) {
        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(principal.getName()));
    }

    @GetMapping("/{vin}")
    public ResponseEntity<?> getVehicle(@PathVariable String vin) {
        return vehicleService.getVehicleByVin(vin)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{vin}/history")
    public ResponseEntity<?> getVehicleHistory(@PathVariable String vin) {
        return vehicleService.getVehicleByVin(vin)
                .map(v -> {
                    List<Map<String, Object>> history = serviceEntryService.getEntriesByVin(v.getVin())
                            .stream()
                            .map(e -> {
                                Map<String, Object> item = new java.util.LinkedHashMap<>();
                                item.put("serviceType", e.getServiceType());
                                item.put("description", e.getDescription() != null ? e.getDescription() : "");
                                item.put("odometerReading", e.getOdometerReading());
                                item.put("cost", e.getCost());
                                item.put("submitterName", e.getSubmitterUsername());
                                item.put("blockchainTxid", e.getBlockchainTxid() != null ? e.getBlockchainTxid() : "");
                                item.put("dataHash", e.getDataHash() != null ? e.getDataHash() : "");
                                item.put("status", e.getStatus());
                                item.put("serviceDate", e.getServiceDate() != null ? e.getServiceDate() : "");
                                item.put("imageIds", e.getImageIds() != null ? e.getImageIds() : List.of());
                                return item;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    Map<String, Object> response = new java.util.LinkedHashMap<>();
                    response.put("vin", v.getVin());
                    response.put("make", v.getMake());
                    response.put("model", v.getModel());
                    response.put("year", v.getYear());
                    response.put("registrationTxid", v.getRegistrationTxid() != null ? v.getRegistrationTxid() : "");
                    response.put("currentOdometer", v.getCurrentOdometer());
                    response.put("serviceHistory", history);
                    return ResponseEntity.ok(response);
                })
                .<ResponseEntity<?>>map(r -> r)
                .orElse(ResponseEntity.notFound().build());
    }
}
