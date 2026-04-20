package com.pnyx.gateway.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "vehicles")
public class Vehicle {

    @Id
    private String id;

    @Indexed(unique = true)
    private String vin;

    private String make;
    private String model;
    private int year;
    private String ownerUsername;
    private String ownerWalletId;
    private String registrationTxid;
    private int currentOdometer;
    private Instant createdAt;
    private Instant updatedAt;

    public Vehicle() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOwnerWalletId() { return ownerWalletId; }
    public void setOwnerWalletId(String ownerWalletId) { this.ownerWalletId = ownerWalletId; }

    public String getRegistrationTxid() { return registrationTxid; }
    public void setRegistrationTxid(String registrationTxid) { this.registrationTxid = registrationTxid; }

    public int getCurrentOdometer() { return currentOdometer; }
    public void setCurrentOdometer(int currentOdometer) { this.currentOdometer = currentOdometer; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
