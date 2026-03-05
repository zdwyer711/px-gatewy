package com.pnyx.gateway.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pnyx.gateway.dto.NodeStatusDto;
import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.service.LedgerClientService;
import com.pnyx.gateway.util.JwtUtil;

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
    
    @Test
    public void testGetMempool_Success() throws Exception {
        // Mock Data
        TransactionDto tx = new TransactionDto("tx1", "s", "r", 10, 1, "time", "sig");
        List<TransactionDto> mockMempool = List.of(tx);

        // Mock Service
        when(ledgerClientService.getMempoolTransactions()).thenReturn(mockMempool);

        // Execute & Verify
        mockMvc.perform(get("/api/proxy/node/mempool")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].txid").value("tx1"));
    }
    
    @Test
    public void testGetPeers_Success() throws Exception {
        // Mock Data
        List<String> mockPeers = List.of("peer1", "peer2");

        // Mock Service
        when(ledgerClientService.getPeers()).thenReturn(mockPeers);

        // Execute & Verify
        mockMvc.perform(get("/api/proxy/node/peers")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("peer1"));
    }
    
    @Test
    public void testGetNodeStatus_Success() throws Exception {
        NodeStatusDto mockStatus = new NodeStatusDto("id", 10L, "hash", 5, 2, 1000L);

        when(ledgerClientService.getNodeStatus()).thenReturn(mockStatus);

        mockMvc.perform(get("/api/proxy/node/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value("id"))
                .andExpect(jsonPath("$.currentBlockHeight").value(10));
    }
}