package com.pnyx.gateway.config;

import com.pnyx.gateway.exception.AuthEntryPoint;
import com.pnyx.gateway.exception.CustomAccessDeniedHandler;
import com.pnyx.gateway.filter.JwtAuthFilter;
import com.pnyx.gateway.filter.RequestLoggingFilter;
import com.pnyx.gateway.service.PnyxUserDetailsService;
import com.pnyx.gateway.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private PnyxUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ==========================================
    // BEAN WIRING TESTS
    // ==========================================

    @Test
    public void testNewSecurityBeans_AreLoaded() {
        // Verify our new components are in the context
        assertNotNull(context.getBean(AuthEntryPoint.class));
        assertNotNull(context.getBean(CustomAccessDeniedHandler.class));
        assertNotNull(context.getBean(RequestLoggingFilter.class));
    }

    // ==========================================
    // ENDPOINT SECURITY TESTS
    // ==========================================

    @Test
    public void testPublicEndpoints_AreAccessible() throws Exception {
        // Should be 400 (Bad Request) due to missing body, NOT 401/403
        mockMvc.perform(post("/api/auth/register"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUnauthenticatedAccess_ReturnsJson401() throws Exception {
        // Try to access a protected URL without a token
        mockMvc.perform(get("/api/proxy/wallets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    public void testCorsPreflight_IsAllowed() throws Exception {
        // Verify OPTIONS request works
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .options("/api/auth/login")
                    .header("Access-Control-Request-Method", "POST")
                    .header("Origin", "http://localhost:5173")
            )
            .andExpect(status().isOk()); // Should be 200 OK now
    }
}