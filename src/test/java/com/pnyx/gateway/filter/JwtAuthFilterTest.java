package com.pnyx.gateway.filter;

import com.pnyx.gateway.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @AfterEach
    public void tearDown() {
        // CRITICAL: Clean up security context after every test
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testDoFilterInternal_NoHeader_ContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // No Authorization header set

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Verify chain continued
        verify(filterChain).doFilter(request, response);
        // Verify no authentication was set
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testDoFilterInternal_ValidToken_AuthenticatesUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "valid.jwt.token";
        String username = "testUser";
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = new User(username, "pass", new ArrayList<>());

        // Mocks
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.isTokenValid(token, username)).thenReturn(true);

        // Execute
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternal_InvalidToken_DoesNotAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String token = "invalid.token";
        String username = "testUser";
        request.addHeader("Authorization", "Bearer " + token);

        // Mocks
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(new User(username, "pass", new ArrayList<>()));
        // Token is NOT valid
        when(jwtUtil.isTokenValid(token, username)).thenReturn(false);

        // Execute
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}