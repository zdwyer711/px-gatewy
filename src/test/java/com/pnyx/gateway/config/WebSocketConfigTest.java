package com.pnyx.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebSocketConfigTest {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration registration;

    @Test
    public void testConfigureMessageBroker() {
        // Run the configuration
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // Verify we enabled the broker on "/topic"
        verify(messageBrokerRegistry).enableSimpleBroker("/topic");
        // Verify application prefix is "/app"
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    public void testRegisterStompEndpoints() {
        // Mock the chain: addEndpoint returns a Registration object
        when(stompEndpointRegistry.addEndpoint(anyString())).thenReturn(registration);
        when(registration.setAllowedOrigins(anyString())).thenReturn(registration);

        // Run the registration
        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        // Verify the specific endpoint "/ws" was registered
        verify(stompEndpointRegistry).addEndpoint("/ws");
        
        // Verify CORS was allowed for React
        verify(registration).setAllowedOrigins("http://localhost:5173");
        
        // Verify SockJS was enabled
        verify(registration).withSockJS();
    }
}