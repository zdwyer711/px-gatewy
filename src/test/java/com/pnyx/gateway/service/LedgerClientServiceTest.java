package com.pnyx.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnyx.gateway.dto.BlockDto;
import com.pnyx.gateway.dto.LockRequest;
import com.pnyx.gateway.dto.LockResponse;
import com.pnyx.gateway.dto.TransactionDto;
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
        LockRequest request = new LockRequest("tx1", "w1", "rec1", "req1", 100L, "sig");
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
    public void testGetWalletHistoryProxy_SendsAuthHeader() throws Exception {
        String walletId = "w123";
        List<WalletHistoryItem> expectedList = List.of();

        mockServer.expect(requestTo("http://localhost:8081/v1/api/wallets/" + walletId + "/history"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));

        ledgerClientService.getWalletHistoryProxy(walletId);
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

    // --- NEW: Test for getLatestBlock (Synchronous RestClient) ---
    @Test
    public void testGetLatestBlock_Success() throws Exception {
        // Prepare Data
        TransactionDto tx1 = new TransactionDto("tx1", "sender", "recipient", 100, 123456L);
        BlockDto expectedBlock = new BlockDto("hash123", 10L, 123456L, List.of(tx1));

        // Expectation
        mockServer.expect(requestTo("http://localhost:8081/v1/api/blocks/latest"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, getBasicAuthHeader()))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedBlock), MediaType.APPLICATION_JSON));

        // Execution
        BlockDto actualBlock = ledgerClientService.getLatestBlock();

        // Verification
        assertEquals("hash123", actualBlock.hash());
        assertEquals(1, actualBlock.transactions().size());
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
}