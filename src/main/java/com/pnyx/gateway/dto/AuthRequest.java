package com.pnyx.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Username cannot be empty")
    String username,
    
    @NotBlank(message = "Password cannot be empty")
    String password
) {}