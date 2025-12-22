package com.pnyx.gateway.controller;

import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.service.LedgerClientService;
import com.pnyx.gateway.util.JwtUtil; // Import your JwtUtil
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService; // Import UserDetailsService
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables filter execution, but beans are still created
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerClientService ledgerClientService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;
    // -----------------------------------------------------------------------

    @Test
    public void testGetTransaction_Success() throws Exception {
        String txId = "de1c1cd7-85a5-4260-a222-9d9b7fc3560a";
        
        TransactionDto mockTx = new TransactionDto(
                txId,
                "sender_123",
                "recipient_456",
                100L,       
                1L,         
                "2025-12-20T22:33:08.264Z",
                "sig_abc"   
        );

        when(ledgerClientService.getTransaction(txId)).thenReturn(mockTx);

        mockMvc.perform(get("/api/proxy//transactions/" + txId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txid").value(txId))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.fee").value(1));
    }

    @Test
    public void testGetTransaction_NotFound() throws Exception {
        String invalidTxId = "invalid-tx-id";

        // FIX: Use .create() to generate the specific 'HttpClientErrorException.NotFound' subclass
        // instead of 'new HttpClientErrorException(...)'.
        when(ledgerClientService.getTransaction(invalidTxId))
                .thenThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, 
                    "Not Found", 
                    HttpHeaders.EMPTY, 
                    null, 
                    null
                ));

        // Execute & Verify
        mockMvc.perform(get("/api/proxy//transactions/" + invalidTxId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}