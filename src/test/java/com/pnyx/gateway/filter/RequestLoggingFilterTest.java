package com.pnyx.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingResponseWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    public void testDoFilterInternal_LogsAndCopiesResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // Simulate the Filter Chain writing to the response
        doAnswer(invocation -> {
            HttpServletResponse passedResponse = invocation.getArgument(1);
            passedResponse.getWriter().write("Hello World");
            passedResponse.setStatus(200);
            return null;
        }).when(filterChain).doFilter(any(), any(ContentCachingResponseWrapper.class));

        // Execute
        filter.doFilterInternal(request, response, filterChain);

        // Verify that the wrapper copied the content back to the real response
        // If this fails, it means we forgot 'copyBodyToResponse()'
        assertEquals("Hello World", response.getContentAsString());
    }
}