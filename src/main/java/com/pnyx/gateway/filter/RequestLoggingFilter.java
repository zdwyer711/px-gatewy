package com.pnyx.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // 1. Wrap the response to cache the body
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 2. Proceed with the chain using the wrapper
            filterChain.doFilter(request, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 3. Read the cached body
            byte[] responseBody = responseWrapper.getContentAsByteArray();
            String responseBodyString = new String(responseBody, StandardCharsets.UTF_8);

            // 4. Log Request and Response
            // (Be careful logging large bodies in production!)
            logger.info(
                "{} {} - Status: {} ({}ms) Response: {}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration,
                responseBodyString
            );

            // 5. CRITICAL: Copy the cached body back to the actual response stream
            // If you forget this, the client receives a blank screen!
            responseWrapper.copyBodyToResponse();
        }
    }
}