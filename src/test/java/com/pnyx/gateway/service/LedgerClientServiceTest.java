package com.pnyx.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.dto.LockRequest;
import com.pnyx.gateway.dto.LockResponse;
import com.pnyx.gateway.dto.NodeStatusDto;
import com.pnyx.gateway.dto.TransactionDto;
import com.pnyx.gateway.dto.WalletDto;
import com.pnyx.gateway.dto.WalletHistoryItem;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@RestClientTest(value = LedgerClientService.class, properties = "px-ledger.url=http://localhost:8081")
public class LedgerClientServiceTest {

    @Autowired
    private LedgerClientService ledgerClientService;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBasicAuthHeader() {
        String auth = "admin:admin";
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    // ... Existing Tests (Lock, History, Balance) ...

    @Test
    public void testRequestLockProxy_SendsAuthHeader() throws Exception {
        LockRequest request = new LockRequest("tx1", "w1", "rec1", "req1", 100L, 0,"sig");
        LockResponse expectedResponse = new LockResponse("tx1", true, null);

        mockServer.expect(requestTo("http://localhost:8081/v1/api/lock"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andExpect(content().json(objectMapper.writeValueAsString(request)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedResponse), MediaType.APPLICATION_JSON));

        ledgerClientService.requestLockProxy(request);
        mockServer.verify();
    }
    
    @Test
    public void testGetWalletHistoryProxy_SendsAuthHeader_AndParams() throws Exception {
        String walletId = "w123";
        int page = 2;
        int size = 10;
        List<WalletHistoryItem> expectedList = List.of();

        // Expect URL with query params
        mockServer.expect(requestTo("http://localhost:8081/v1/api/wallets/" + walletId + "/history?page=2&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

        ledgerClientService.getWalletHistoryProxy(walletId, page, size);
        mockServer.verify();
    }
    
    @Test
    public void testGetWalletBalanceProxy_Success() throws Exception {
        String walletId = "w123";
        Long expectedBalance = 5000L;

        mockServer.expect(requestTo("http://localhost:8081/v1/api/wallets/" + walletId + "/balance"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(expectedBalance.toString(), MediaType.APPLICATION_JSON));

        Long actualBalance = ledgerClientService.getWalletBalanceProxy(walletId);

        assertEquals(expectedBalance, actualBalance);
        mockServer.verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubscribeToNodeEvents_Success() {
        // 1. Create Mocks for the Builders (Local to this test)
        WebClient.Builder mockWebClientBuilder = mock(WebClient.Builder.class);
        org.springframework.web.client.RestClient.Builder mockRestClientBuilder = mock(org.springframework.web.client.RestClient.Builder.class);
        
        WebClient mockWebClient = mock(WebClient.class);
        org.springframework.web.client.RestClient mockRestClient = mock(org.springframework.web.client.RestClient.class);

        // 2. Configure the WebClient Builder Mock Chain
        // We must mock the chain because the Constructor calls these methods
        when(mockWebClientBuilder.baseUrl(anyString())).thenReturn(mockWebClientBuilder);
        when(mockWebClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(mockWebClientBuilder);
        when(mockWebClientBuilder.build()).thenReturn(mockWebClient);

        // 3. Configure the RestClient Builder Mock Chain
        // The constructor ALSO calls this, so we must mock it to prevent NullPointerException
        when(mockRestClientBuilder.baseUrl(anyString())).thenReturn(mockRestClientBuilder);
        when(mockRestClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(mockRestClientBuilder);
        when(mockRestClientBuilder.build()).thenReturn(mockRestClient);

        // 4. Configure the WebClient behavior for the actual call
        WebClient.RequestHeadersUriSpec requestHeadersUriSpecMock = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(mockWebClient.get()).thenReturn(requestHeadersUriSpecMock);
        when(requestHeadersUriSpecMock.uri("/v1/api/events/subscribe")).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.accept(MediaType.TEXT_EVENT_STREAM)).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
        when(responseSpecMock.bodyToFlux(String.class)).thenReturn(Flux.just("BLOCK_MINED", "HEARTBEAT"));

        // 5. Manually Instantiate the Service
        // We pass our controlled mocks directly into the constructor
        LedgerClientService manualService = new LedgerClientService(
            mockRestClientBuilder, 
            mockWebClientBuilder, 
            "http://localhost:8081"
        );

        // 6. Verify
        Flux<String> eventStream = manualService.subscribeToNodeEvents();

        StepVerifier.create(eventStream)
                .expectNext("BLOCK_MINED")
                .expectNext("HEARTBEAT")
                .verifyComplete();
    }
    
    @Test
    public void testGetWalletNonceProxy_Success() {
        String walletId = "w123";
        Long expectedNonce = 5L;

        mockServer.expect(requestTo("http://localhost:8081/v1/api/wallets/" + walletId + "/nonce"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader())) // Verifies Admin Auth to Node
                .andRespond(withSuccess(expectedNonce.toString(), MediaType.APPLICATION_JSON));

        Long actualNonce = ledgerClientService.getWalletNonceProxy(walletId);

        assertEquals(expectedNonce, actualNonce);
        mockServer.verify();
    }
    
    @Test
    public void testGetLatestBlock_Success() throws Exception {
        // Prepare Data
        TransactionDto tx1 = new TransactionDto("tx1", "sender", "recipient", 100, 0L, "2025-12-21T10:00:00Z", "signature");
        
        // FIX: Update constructor to match new BlockDto signature
        // Signature: (hash, index, timestamp, nonce, previousHash, minerAddress, data, transactions)
        BlockDto expectedBlock = new BlockDto(
            "hash123", 
            10L, 
            123456L, 
            0L,             // nonce
            "prevHash",     // previousHash
            "minerAddr",    // minerAddress
            "someData",     // data
            List.of(tx1)
        );

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/blocks/latest"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(List.of(expectedBlock)), MediaType.APPLICATION_JSON));

        // Execution
        BlockDto actualBlock = ledgerClientService.getLatestBlock();

        // Verification
        assertEquals("hash123", actualBlock.hash());
        assertEquals(1, actualBlock.transactions().size());
        mockServer.verify();
    }

    @Test
    public void testGetRecentBlocks_Success() throws Exception {
        int depth = 5;
        
        // Prepare Response Data
        TransactionDto tx = new TransactionDto("tx1", "sender", "recipient", 100, 0L, "2025-12-21T10:00:00Z", "signature");
        
        
        // FIX: Update constructor for block1
        BlockDto block1 = new BlockDto(
            "hash1", 
            100L, 
            100L, 
            0L, 
            "prev1", 
            "miner1", 
            "data1", 
            List.of(tx)
        );
        
        // FIX: Update constructor for block2
        BlockDto block2 = new BlockDto(
            "hash2", 
            99L, 
            100L, 
            0L, 
            "prev2", 
            "miner2", 
            "data2", 
            List.of()
        );
        
        List<BlockDto> expectedList = List.of(block1, block2);

        // Expectation: Verify URI contains "?depth=5"
        mockServer.expect(requestTo("http://localhost:8081/v1/api/blocks/latest?depth=" + depth))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

        // Execution
        List<BlockDto> actualList = ledgerClientService.getRecentBlocks(depth);

        // Verification
        assertEquals(2, actualList.size());
        assertEquals("hash1", actualList.get(0).hash());
        mockServer.verify();
    }
    
    @Test
    public void testGetBlockByHashOrIndex_Success() throws Exception {
        String blockId = "2380"; // Can be index or hash
        
        // Prepare Data matching your JSON
        TransactionDto tx = new TransactionDto("tx1", "SYSTEM", "SYSTEM", 50, 0L, "2025-12-22T02:07:10.002Z", "sig");
        
        BlockDto expectedBlock = new BlockDto(
            "000014d3cf9f...", 
            2380L, 
            1766369234110L, 
            61820L, 
            "00005ee2...", 
            "miner123", 
            "{\"some\":\"data\"}", 
            List.of(tx)
        );

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/blocks/" + blockId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedBlock), MediaType.APPLICATION_JSON));

        // Execution
        BlockDto actualBlock = ledgerClientService.getBlockByHashOrIndex(blockId);

        // Verification
        assertEquals(2380L, actualBlock.index());
        assertEquals("miner123", actualBlock.minerAddress());
        mockServer.verify();
    }
    
    @Test
    public void testGetTransaction_Success() throws Exception {
        String txId = "de1c1cd7-85a5-4260-a222-9d9b7fc3560a";
        
        // Prepare Data matching your JSON example
        TransactionDto expectedTx = new TransactionDto(
            txId, 
            "sender_addr", 
            "recipient_addr", 
            4L, 
            1L, // Fee
            "2025-12-20T22:33:08.264Z", 
            "sig_123" // Signature
        );

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/transactions/" + txId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedTx), MediaType.APPLICATION_JSON));

        // Execution
        TransactionDto actualTx = ledgerClientService.getTransaction(txId);

        // Verification
        assertEquals(txId, actualTx.txid());
        assertEquals(1L, actualTx.fee());
        assertEquals("sig_123", actualTx.signature());
        mockServer.verify();
    }
    
    @Test
    public void testGetNetworkHashrate_Success() {
        double expectedHashrate = 15.242348125407014;

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/node/network/hashrate"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(String.valueOf(expectedHashrate), MediaType.APPLICATION_JSON));

        // Execution
        Double actualHashrate = ledgerClientService.getNetworkHashrate();

        // Verification
        assertEquals(expectedHashrate, actualHashrate);
        mockServer.verify();
    }
    
    @Test
    public void testGetTopWallets_Success() throws Exception {
        int page = 0;
        int size = 5;

        // Prepare Data
        WalletDto w1 = new WalletDto("wallet_A", "pub_A", 5000L, 1L);
        WalletDto w2 = new WalletDto("wallet_B", "pub_B", 3000L, 2L);
        List<WalletDto> expectedList = List.of(w1, w2);

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/wallets/top?page=0&size=5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

        // Execution
        List<WalletDto> actualList = ledgerClientService.getTopWallets(page, size);

        // Verification
        assertEquals(2, actualList.size());
        assertEquals(5000L, actualList.get(0).balance());
        mockServer.verify();
    }
    
    @Test
    public void testGetMempoolTransactions_Success() throws Exception {
        // Prepare Data
        TransactionDto tx1 = new TransactionDto("tx1", "sender", "recipient", 100, 1, "2025-12-20T22:33:08.264Z", "sig1");
        TransactionDto tx2 = new TransactionDto("tx2", "sender2", "recipient2", 50, 1, "2025-12-20T22:34:00.000Z", "sig2");
        List<TransactionDto> expectedList = List.of(tx1, tx2);

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/node/mempool"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

        // Execution
        List<TransactionDto> actualList = ledgerClientService.getMempoolTransactions();

        // Verification
        assertEquals(2, actualList.size());
        assertEquals("tx1", actualList.get(0).txid());
        mockServer.verify();
    }
    
    @Test
    public void testGetPeers_Success() throws Exception {
        // Prepare Data
        List<String> expectedPeers = List.of("192.168.1.5:8080", "10.0.0.4:8080");

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/node/peers"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedPeers), MediaType.APPLICATION_JSON));

        // Execution
        List<String> actualPeers = ledgerClientService.getPeers();

        // Verification
        assertEquals(2, actualPeers.size());
        assertEquals("192.168.1.5:8080", actualPeers.get(0));
        mockServer.verify();
    }
    
    @Test
    public void testGetNodeStatus_Success() throws Exception {
        // Prepare Data matching your JSON
        NodeStatusDto expectedStatus = new NodeStatusDto(
            "node_123", 
            166L, 
            "hash_abc", 
            0, 
            0, 
            1767412697586L
        );

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/node/status"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedStatus), MediaType.APPLICATION_JSON));

        // Execution
        NodeStatusDto actualStatus = ledgerClientService.getNodeStatus();

        // Verification
        assertEquals("node_123", actualStatus.nodeId());
        assertEquals(166L, actualStatus.currentBlockHeight());
        mockServer.verify();
    }
}