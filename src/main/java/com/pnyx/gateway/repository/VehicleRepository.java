package com.pnyx.gateway.repository;

import com.pnyx.gateway.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    Optional<Vehicle> findByVin(String vin);
    List<Vehicle> findByOwnerUsername(String ownerUsername);
    boolean existsByVin(String vin);
}
