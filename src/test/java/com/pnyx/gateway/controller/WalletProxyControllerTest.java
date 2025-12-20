package com.pnyx.gateway.controller;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnyx.gateway.dto.CommitAck;
import com.pnyx.gateway.dto.CommitRequest;
import com.pnyx.gateway.dto.CreateWalletRequest;
import com.pnyx.gateway.dto.LockRequest;
import com.pnyx.gateway.dto.LockResponse;
import com.pnyx.gateway.dto.WalletHistoryItem;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.LedgerClientService;
import com.pnyx.gateway.service.PnyxUserDetailsService;
import com.pnyx.gateway.util.JwtUtil;

@WebMvcTest(
    controllers = WalletProxyController.class, 
    excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
public class WalletProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LedgerClientService ledgerClientService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private PnyxUserDetailsService userDetailsService;

    @MockitoBean
    private UserRepository userRepository; 

    // ==========================================
    // CREATE WALLET TESTS (Full Coverage)
    // ==========================================

    @Test
    public void testCreateWallet_Success_UpdatesUser() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("valid-key");
        String fakeWalletId = "wallet-uuid-123";
        String username = "testUser";
        
        User mockUser = new User();
        mockUser.setUsername(username);
        
        // 1. Mock Ledger Success
        when(ledgerClientService.createWalletProxy(any(CreateWalletRequest.class)))
                .thenReturn(fakeWalletId);
        
        // 2. Mock Database Find
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(mockUser));

        mockMvc.perform(post("/api/proxy/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                // FIX: Manually inject Principal because filters are disabled
                .principal(() -> username) 
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(fakeWalletId));

        // 3. Verify user was updated
        verify(userRepository, times(1)).save(argThat(user -> 
            user.getWalletId().equals(fakeWalletId) && user.getUsername().equals(username)
        ));
    }

    @Test
    public void testCreateWallet_UserNotFound_InternalError() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("valid-key");
        String username = "ghostUser";
        
        when(ledgerClientService.createWalletProxy(any(CreateWalletRequest.class)))
                .thenReturn("wallet-123");
        
        // Mock DB returning Empty
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/proxy/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(() -> username)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred connecting to the Ledger."));
                
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testCreateWallet_LedgerError_ReturnsErrorStatus() throws Exception {
        CreateWalletRequest request = new CreateWalletRequest("bad-key");
        String username = "testUser";

        // Mock Ledger throwing 400 Bad Request
        when(ledgerClientService.createWalletProxy(any(CreateWalletRequest.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid Key"));

        mockMvc.perform(post("/api/proxy/wallets")
                .contentType(MediaType.APPLICATION_JSON)
                .principal(() -> username)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // Expect 400 back
        
        verify(userRepository, never()).save(any());
    }

    // ==========================================
    // LOCK TESTS
    // ==========================================

    @Test
    public void testRequestLock_Success() throws Exception {
        LockRequest request = new LockRequest("tx1", "w1", "rec1", "req1", 50, "sig");
        LockResponse response = new LockResponse("tx1", true, null);

        when(ledgerClientService.requestLockProxy(any(LockRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/proxy/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    public void testRequestLock_ConflictFromLedger() throws Exception {
        LockRequest request = new LockRequest("tx1", "w1", "rec1", "req1", 50, "sig");
        LockResponse conflictResponse = new LockResponse("tx1", false, "Insufficient Funds");

        // Simulate Ledger throwing 409 Conflict containing a JSON body
        HttpClientErrorException ex = new HttpClientErrorException(
                HttpStatus.CONFLICT, "Conflict",
                objectMapper.writeValueAsBytes(conflictResponse),
                StandardCharsets.UTF_8
        );

        when(ledgerClientService.requestLockProxy(any(LockRequest.class))).thenThrow(ex);

        mockMvc.perform(post("/api/proxy/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ok").value(false));
    }

    // ==========================================
    // COMMIT TESTS
    // ==========================================

    @Test
    public void testCommitTransaction_Success() throws Exception {
        CommitRequest request = new CommitRequest("tx1", "w1");
        // Assuming CommitAck record has (txid, ok) or (txid, ok, message)
        // Adjust based on your actual DTO definition
        CommitAck ack = new CommitAck("tx1", true); 

        when(ledgerClientService.commitTransactionProxy(any(CommitRequest.class))).thenReturn(ack);

        mockMvc.perform(post("/api/proxy/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testCommitTransaction_ServerError() throws Exception {
        CommitRequest request = new CommitRequest("tx1", "w1");

        // Simulate Ledger throwing 500
        when(ledgerClientService.commitTransactionProxy(any(CommitRequest.class)))
                .thenThrow(new RuntimeException("Connection Refused"));

        mockMvc.perform(post("/api/proxy/commit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false));
    }
    
    @Test
    public void testGetHistory_Success() throws Exception {
        String walletId = "wallet-123";
        String username = "testUser";

        // Mock User owning this wallet
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setWalletId(walletId);

        // Mock Service Response
        WalletHistoryItem item = new WalletHistoryItem("tx1", 12345L, "SENT", "other", 100);
        when(ledgerClientService.getWalletHistoryProxy(walletId)).thenReturn(List.of(item));

        // Mock DB
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/proxy/wallets/" + walletId + "/history")
                .principal(() -> username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].txid").value("tx1"))
                .andExpect(jsonPath("$[0].type").value("SENT"));
    }

    @Test
    public void testGetHistory_Forbidden_NotOwner() throws Exception {
        String targetWalletId = "wallet-123";
        String username = "hacker";

        // Mock User owning a DIFFERENT wallet (or none)
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setWalletId("wallet-999"); // Different ID

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/proxy/wallets/" + targetWalletId + "/history")
                .principal(() -> username))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You are not authorized to view history for this wallet."));
        
        // Ensure we never called the ledger
        verify(ledgerClientService, never()).getWalletHistoryProxy(any());
    }
    
    @Test
    public void testGetBalance_Success() throws Exception {
        String walletId = "wallet-123";
        String username = "testUser";

        // Mock User owning this wallet
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setWalletId(walletId);

        // Mock Service Response
        when(ledgerClientService.getWalletBalanceProxy(walletId)).thenReturn(5000L);

        // Mock DB
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/proxy/wallets/" + walletId + "/balance")
                .principal(() -> username))
                .andExpect(status().isOk())
                .andExpect(content().string("5000"));
    }

    @Test
    public void testGetBalance_Forbidden_NotOwner() throws Exception {
        String targetWalletId = "wallet-123";
        String username = "hacker";

        // Mock User owning a DIFFERENT wallet
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setWalletId("wallet-999"); 

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/proxy/wallets/" + targetWalletId + "/balance")
                .principal(() -> username))
                .andExpect(status().isForbidden())
                .andExpect(content().string("You are not authorized to view the balance of this wallet."));
        
        // Verify we blocked the call to the ledger
        verify(ledgerClientService, never()).getWalletBalanceProxy(any());
    }
}