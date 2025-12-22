package com.pnyx.gateway.controller;

import com.pnyx.gateway.service.LedgerClientService;
import com.pnyx.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NodeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerClientService ledgerClientService;

    // Required mocks for Security Configuration
    @MockitoBean
    private JwtUtil jwtUtil;
    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    public void testGetNetworkHashrate_Success() throws Exception {
        Double mockHashrate = 15.242348125407014;

        when(ledgerClientService.getNetworkHashrate()).thenReturn(mockHashrate);

        mockMvc.perform(get("/api/proxy/node/network/hashrate")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(mockHashrate)));
    }
}