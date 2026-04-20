package com.pnyx.gateway.dto;

import java.util.List;

public class ServiceEntryRequest {
    private String vin;
    private String serviceType;
    private int odometerReading;
    private String description;
    private double cost;
    private List<String> imageIds;
    private String serviceDate;
    private String txid;

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public int getOdometerReading() { return odometerReading; }
    public void setOdometerReading(int odometerReading) { this.odometerReading = odometerReading; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public List<String> getImageIds() { return imageIds; }
    public void setImageIds(List<String> imageIds) { this.imageIds = imageIds; }

    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }

    public String getTxid() { return txid; }
    public void setTxid(String txid) { this.txid = txid; }
}
