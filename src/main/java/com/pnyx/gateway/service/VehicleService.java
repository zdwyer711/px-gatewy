package com.pnyx.gateway.service;

import com.pnyx.gateway.dto.VehicleRegistrationRequest;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.model.Vehicle;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.repository.VehicleRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class VehicleService {

    // Standard VIN: 17 chars, A-Z excluding I/O/Q, and 0-9
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    public Vehicle registerVehicle(String username, VehicleRegistrationRequest req) {
        // 1. Validate VIN format
        if (!VIN_PATTERN.matcher(req.vin()).matches()) {
            throw new IllegalArgumentException("Invalid VIN format. Must be 17 alphanumeric characters (no I, O, or Q).");
        }

        // 2. Check VIN uniqueness
        if (vehicleRepository.existsByVin(req.vin())) {
            throw new IllegalStateException("VIN " + req.vin() + " is already registered.");
        }

        // 3. Look up owner
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (user.getWalletId() == null) {
            throw new IllegalStateException("Wallet not set up. Please create a wallet before registering a vehicle.");
        }

        // 4. Save vehicle document with the txid from the client's lock/commit
        Instant now = Instant.now();
        Vehicle vehicle = new Vehicle();
        vehicle.setVin(req.vin());
        vehicle.setMake(req.make());
        vehicle.setModel(req.model());
        vehicle.setYear(req.year());
        vehicle.setOwnerUsername(username);
        vehicle.setOwnerWalletId(user.getWalletId());
        vehicle.setRegistrationTxid(req.txid());
        vehicle.setCurrentOdometer(0);
        vehicle.setCreatedAt(now);
        vehicle.setUpdatedAt(now);

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> getVehiclesByOwner(String username) {
        return vehicleRepository.findByOwnerUsername(username);
    }

    public Optional<Vehicle> getVehicleByVin(String vin) {
        return vehicleRepository.findByVin(vin);
    }
}
