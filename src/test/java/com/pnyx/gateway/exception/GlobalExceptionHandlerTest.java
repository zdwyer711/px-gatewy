package com.pnyx.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnyx.gateway.controller.AuthController;
import com.pnyx.gateway.dto.RegisterRequest;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.PnyxUserDetailsService; // Import this
import com.pnyx.gateway.service.RefreshTokenService;
import com.pnyx.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService; // Import this
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class, 
    excludeAutoConfiguration = SecurityAutoConfiguration.class 
)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Mocks required for AuthController ---
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationManager authenticationManager;
    
    @MockitoBean
    private RefreshTokenService refreshTokenService;

    // --- Mocks required because JwtAuthFilter is implicitly loaded ---
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PnyxUserDetailsService userDetailsService;

    @Test
    public void testValidationException_ReturnsJson() throws Exception {
        // Create request with invalid password (too short)
        RegisterRequest request = new RegisterRequest("user", "123", "email@test.com", "ROLE_USER");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error").value("Validation Error"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value(400))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password must be at least 6 characters")));
    }
}