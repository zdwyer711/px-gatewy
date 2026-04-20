package com.pnyx.gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRegistrationRequest(
    @NotBlank String vin,
    @NotBlank String make,
    @NotBlank String model,
    @NotNull Integer year,
    @NotBlank String txid
) {}
