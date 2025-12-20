package com.pnyx.gateway.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TokenRefreshExceptionTest {

    @Test
    public void testExceptionMessageFormatting() {
        String token = "sample-token-123";
        String message = "Token is invalid";

        TokenRefreshException exception = new TokenRefreshException(token, message);

        // Verify the formatted message matches the expected pattern
        // Expected: "Failed for [sample-token-123]: Token is invalid"
        String expectedMessage = String.format("Failed for [%s]: %s", token, message);
        
        assertEquals(expectedMessage, exception.getMessage());
    }
}