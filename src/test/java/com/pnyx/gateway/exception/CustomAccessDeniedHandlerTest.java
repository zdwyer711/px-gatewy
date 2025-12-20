package com.pnyx.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testHandle_ReturnsJson403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Trigger the Handler
        accessDeniedHandler.handle(request, response, new AccessDeniedException("Not allowed"));

        // Verify Status and Content Type
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertEquals("application/json", response.getContentType());

        // Verify JSON Body
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        assertEquals(403, body.get("status"));
        assertEquals("Forbidden", body.get("error"));
        assertEquals("You do not have permission to access this resource.", body.get("message"));
        assertEquals("/api/admin", body.get("path"));
    }
}