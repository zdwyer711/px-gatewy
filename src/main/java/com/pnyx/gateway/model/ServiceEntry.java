package com.pnyx.gateway.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "service_entries")
public class ServiceEntry {

    @Id
    private String id;

    private String vehicleId;

    @Indexed
    private String vin;

    private String submitterUsername;
    private String submitterWalletId;
    private String serviceType;
    private String description;
    private int odometerReading;
    private double cost;
    private List<String> imageIds;
    private String dataHash;
    private String blockchainTxid;
    private String status = "PENDING"; // PENDING | CONFIRMED | FAILED
    private String serviceDate;
    private Instant createdAt;

    public ServiceEntry() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getSubmitterUsername() { return submitterUsername; }
    public void setSubmitterUsername(String submitterUsername) { this.submitterUsername = submitterUsername; }

    public String getSubmitterWalletId() { return submitterWalletId; }
    public void setSubmitterWalletId(String submitterWalletId) { this.submitterWalletId = submitterWalletId; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOdometerReading() { return odometerReading; }
    public void setOdometerReading(int odometerReading) { this.odometerReading = odometerReading; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }

    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }

    public String getBlockchainTxid() { return blockchainTxid; }
    public void setBlockchainTxid(String blockchainTxid) { this.blockchainTxid = blockchainTxid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
