package com.pnyx.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnyx.gateway.dto.*;
import com.pnyx.gateway.exception.GlobalExceptionHandler;
import com.pnyx.gateway.exception.TokenRefreshException;
import com.pnyx.gateway.model.RefreshToken;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.PnyxUserDetailsService;
import com.pnyx.gateway.service.RefreshTokenService;
import com.pnyx.gateway.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PnyxUserDetailsService userDetailsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        // We attach the GlobalExceptionHandler so exceptions (like 401/403) return JSON
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler()) 
                .build();
        objectMapper = new ObjectMapper();
    }

    // ==========================================
    // REGISTRATION TESTS
    // ==========================================

    @Test
    public void testRegisterUser_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123", "test@pnyx.com");

        // Mock DB: Username does NOT exist (Optional.empty())
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegisterUser_UsernameTaken() throws Exception {
        RegisterRequest request = new RegisterRequest("existingUser", "password123", "test@pnyx.com");

        // Mock DB: Username DOES exist
        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Username is already taken!"));

        verify(userRepository, never()).save(any(User.class));
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    public void testLoginUser_Success() throws Exception {
        AuthRequest loginRequest = new AuthRequest("validUser", "password123");
        String fakeAccessToken = "access.token.jwt";
        String fakeRefreshToken = "refresh-token-uuid";

        // Mocks for successful login flow
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("validUser");

        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken(fakeRefreshToken);

        when(userDetailsService.loadUserByUsername("validUser")).thenReturn(mockUserDetails);
        when(jwtUtil.generateToken(mockUserDetails.getUsername())).thenReturn(fakeAccessToken);
        when(refreshTokenService.createRefreshToken("validUser")).thenReturn(mockRefreshToken);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(fakeAccessToken))
                .andExpect(jsonPath("$.refreshToken").value(fakeRefreshToken));
    }

    @Test
    public void testLoginUser_InvalidCredentials() throws Exception {
        AuthRequest loginRequest = new AuthRequest("wrongUser", "wrongPass");

        // Simulate AuthenticationManager failing
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Note: In standalone mode without a specific exception handler for BadCredentials,
        // it might throw a nested exception. Ideally, GlobalExceptionHandler handles this, 
        // but if not, the test framework catches it. 
        // We will assume GlobalExceptionHandler maps generic Exceptions to 500 or you added a specific handler.
        // If your GlobalExceptionHandler maps Exception -> 500:
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError()) // Or 401 if you added a handler for BadCredentials
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    // ==========================================
    // REFRESH TOKEN TESTS (NEW)
    // ==========================================

    @Test
    public void testRefreshToken_Success() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest("valid-refresh-token");
        String newAccessToken = "new.access.token";

        // Mock Data
        User user = new User();
        user.setUsername("testUser");

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("valid-refresh-token");
        storedToken.setUser(user);

        // 1. Service finds token
        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(storedToken);
        // 2. Service verifies it (returns valid token)
        when(refreshTokenService.verifyExpiration(storedToken)).thenReturn(storedToken);
        // 3. Generate new JWT
        when(jwtUtil.generateToken("testUser")).thenReturn(newAccessToken);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));
    }

    @Test
    public void testRefreshToken_NotFound_Returns403() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest("unknown-token");

        // Service throws exception because token isn't in DB
        when(refreshTokenService.findByToken("unknown-token"))
                .thenThrow(new TokenRefreshException("unknown-token", "Refresh token is not in database!"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()) // The Exception Handler maps this to 403
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Failed for [unknown-token]: Refresh token is not in database!"));
    }

    @Test
    public void testRefreshToken_Expired_Returns403() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest("expired-token");
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken("expired-token");

        // Service finds it, but verifyExpiration throws exception
        when(refreshTokenService.findByToken("expired-token")).thenReturn(expiredToken);
        when(refreshTokenService.verifyExpiration(expiredToken))
                .thenThrow(new TokenRefreshException("expired-token", "Refresh token was expired"));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Refresh token was expired")));
    }
}