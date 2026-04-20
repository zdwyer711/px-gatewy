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
                .map(v -> ResponseEntity.ok(Map.of(
                        "vin", v.getVin(),
                        "make", v.getMake(),
                        "model", v.getModel(),
                        "year", v.getYear(),
                        "registrationTxid", v.getRegistrationTxid() != null ? v.getRegistrationTxid() : "",
                        "currentOdometer", v.getCurrentOdometer(),
                        "serviceHistory", serviceEntryService.getEntriesByVin(v.getVin())
                )))
                .<ResponseEntity<?>>map(r -> r)
                .orElse(ResponseEntity.notFound().build());
    }
}
