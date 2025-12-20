package com.pnyx.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthEntryPointTest {

    private final AuthEntryPoint authEntryPoint = new AuthEntryPoint();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCommence_ReturnsJson401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        // Trigger the Entry Point
        authEntryPoint.commence(request, response, new InsufficientAuthenticationException("Token missing"));

        // Verify Status and Content Type
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("application/json", response.getContentType());

        // Verify JSON Body
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertEquals(401, body.get("status"));
        assertEquals("Unauthorized", body.get("error"));
        assertEquals("Token missing", body.get("message"));
        assertEquals("/api/test", body.get("path"));
    }
}