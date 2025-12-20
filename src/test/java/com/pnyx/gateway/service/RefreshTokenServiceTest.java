package com.pnyx.gateway.service;

import com.pnyx.gateway.exception.TokenRefreshException;
import com.pnyx.gateway.model.RefreshToken;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.RefreshTokenRepository;
import com.pnyx.gateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    public void setUp() {
        // Manually inject the @Value("${jwt.refreshExpiration}") property
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 600000L); // 10 minutes
    }

    @Test
    public void testFindByToken_Success() {
        RefreshToken token = new RefreshToken();
        token.setToken("uuid-123");

        when(refreshTokenRepository.findByToken("uuid-123")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.findByToken("uuid-123");
        assertNotNull(result);
        assertEquals("uuid-123", result.getToken());
    }

    @Test
    public void testFindByToken_NotFound_ThrowsException() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.findByToken("unknown");
        });
    }

    @Test
    public void testCreateRefreshToken_Success() {
        String username = "testuser";
        User user = new User();
        user.setUsername(username);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        // Mock the save to return the object passed to it
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArguments()[0]);

        RefreshToken result = refreshTokenService.createRefreshToken(username);

        assertNotNull(result);
        assertNotNull(result.getToken()); // UUID should be generated
        assertNotNull(result.getExpiryDate());
        assertEquals(user, result.getUser());
        
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    public void testVerifyExpiration_ValidToken() {
        RefreshToken token = new RefreshToken();
        // Set expiry to future
        token.setExpiryDate(Instant.now().plusMillis(10000));
        token.setToken("valid-token");

        RefreshToken result = refreshTokenService.verifyExpiration(token);
        
        // Should return the same token object
        assertEquals(token, result);
        // Verify delete was NOT called
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    public void testVerifyExpiration_ExpiredToken() {
        RefreshToken token = new RefreshToken();
        // Set expiry to past
        token.setExpiryDate(Instant.now().minusMillis(10000));
        token.setToken("expired-token");

        // Expect exception
        TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> {
            refreshTokenService.verifyExpiration(token);
        });

        assertTrue(exception.getMessage().contains("Refresh token was expired"));
        
        // Verify token was deleted from DB
        verify(refreshTokenRepository, times(1)).delete(token);
    }
}