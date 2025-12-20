package com.pnyx.gateway.controller;

import com.pnyx.gateway.service.PnyxUserDetailsService;
import com.pnyx.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = StatusController.class,
    excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false) // Bypass security filters for logic testing
public class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // We mock these because WebMvcTest still tries to load SecurityConfig, 
    // which requires these beans to exist.
    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PnyxUserDetailsService userDetailsService;

    @Test
    public void testGetAppStatus_ReturnsUp() throws Exception {
        mockMvc.perform(get("/api/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}