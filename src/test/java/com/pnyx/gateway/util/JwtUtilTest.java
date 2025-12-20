package com.pnyx.gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    public void setup() {
        jwtUtil = new JwtUtil();
    }

    @Test
    public void testGenerateToken_ReturnsNonEmptyString() {
        String token = jwtUtil.generateToken("testUser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    public void testExtractUsername_ReturnsCorrectUsername() {
        String username = "pnyxAdmin";
        String token = jwtUtil.generateToken(username);
        
        String extracted = jwtUtil.extractUsername(token);
        assertEquals(username, extracted);
    }

    @Test
    public void testIsTokenValid_TrueForCorrectUser() {
        String username = "validUser";
        String token = jwtUtil.generateToken(username);

        assertTrue(jwtUtil.isTokenValid(token, username));
    }

    @Test
    public void testIsTokenValid_FalseForWrongUser() {
        String token = jwtUtil.generateToken("userA");
        assertFalse(jwtUtil.isTokenValid(token, "userB"));
    }
}