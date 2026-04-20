package com.pnyx.gateway.service;

import com.pnyx.gateway.dto.ServiceEntryRequest;
import com.pnyx.gateway.model.ImageDocument;
import com.pnyx.gateway.model.ServiceEntry;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.model.Vehicle;
import com.pnyx.gateway.repository.ImageRepository;
import com.pnyx.gateway.repository.ServiceEntryRepository;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.repository.VehicleRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServiceEntryService {

    private final ServiceEntryRepository serviceEntryRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    public ServiceEntryService(ServiceEntryRepository serviceEntryRepository,
                               VehicleRepository vehicleRepository,
                               UserRepository userRepository,
                               ImageRepository imageRepository) {
        this.serviceEntryRepository = serviceEntryRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
    }

    public ServiceEntry createServiceEntry(String username, ServiceEntryRequest req) {
        Vehicle vehicle = vehicleRepository.findByVin(req.getVin())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found: " + req.getVin()));

        if (req.getOdometerReading() < vehicle.getCurrentOdometer()) {
            throw new IllegalArgumentException(
                    "Odometer reading " + req.getOdometerReading() +
                    " cannot be less than current odometer " + vehicle.getCurrentOdometer());
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<String> imageHashes = new ArrayList<>();
        if (req.getImageIds() != null && !req.getImageIds().isEmpty()) {
            for (String imageId : req.getImageIds()) {
                imageRepository.findById(imageId).ifPresent(img -> imageHashes.add(img.getSha256Hash()));
            }
        }

        String dataHash = computeDataHash(
                req.getVin(), req.getServiceType(), req.getOdometerReading(),
                user.getWalletId(), imageHashes);

        ServiceEntry entry = new ServiceEntry();
        entry.setVehicleId(vehicle.getId());
        entry.setVin(req.getVin());
        entry.setSubmitterUsername(username);
        entry.setSubmitterWalletId(user.getWalletId());
        entry.setServiceType(req.getServiceType());
        entry.setDescription(req.getDescription());
        entry.setOdometerReading(req.getOdometerReading());
        entry.setCost(req.getCost());
        entry.setImageIds(req.getImageIds() != null ? req.getImageIds() : List.of());
        entry.setDataHash(dataHash);
        entry.setBlockchainTxid(req.getTxid());
        entry.setServiceDate(req.getServiceDate());
        entry.setCreatedAt(Instant.now());

        ServiceEntry saved = serviceEntryRepository.save(entry);

        vehicle.setCurrentOdometer(req.getOdometerReading());
        vehicle.setUpdatedAt(Instant.now());
        vehicleRepository.save(vehicle);

        return saved;
    }

    public List<ServiceEntry> getEntriesByVin(String vin) {
        return serviceEntryRepository.findByVin(vin);
    }

    public Optional<ServiceEntry> getEntryById(String id) {
        return serviceEntryRepository.findById(id);
    }

    public List<ServiceEntry> getEntriesByUsername(String username) {
        return serviceEntryRepository.findBySubmitterUsername(username);
    }

    private String computeDataHash(String vin, String serviceType, int odometer,
                                   String walletId, List<String> imageHashes) {
        List<String> sortedHashes = new ArrayList<>(imageHashes);
        Collections.sort(sortedHashes);
        String canonical = vin + "|" + serviceType + "|" + odometer + "|" + walletId + "|" +
                String.join(",", sortedHashes);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
