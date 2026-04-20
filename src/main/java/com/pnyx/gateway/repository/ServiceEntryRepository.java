package com.pnyx.gateway.repository;

import com.pnyx.gateway.model.ServiceEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceEntryRepository extends MongoRepository<ServiceEntry, String> {
    List<ServiceEntry> findByVin(String vin);
    List<ServiceEntry> findByVehicleId(String vehicleId);
    List<ServiceEntry> findBySubmitterUsername(String submitterUsername);
    Optional<ServiceEntry> findByBlockchainTxid(String blockchainTxid);
}
