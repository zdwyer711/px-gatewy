package com.pnyx.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pnyx.gateway.dto.AuthRequest;
import com.pnyx.gateway.dto.JwtResponse;
import com.pnyx.gateway.dto.RegisterRequest;
import com.pnyx.gateway.dto.TokenRefreshRequest;
import com.pnyx.gateway.dto.TokenRefreshResponse;
import com.pnyx.gateway.model.RefreshToken;
import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import com.pnyx.gateway.service.PnyxUserDetailsService; // Your custom service
import com.pnyx.gateway.service.RefreshTokenService;
import com.pnyx.gateway.util.JwtUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final PnyxUserDetailsService userDetailsService; // Use the concrete class
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          PnyxUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.username()).isPresent()) {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }

        User user = new User();
        user.setUsername(registerRequest.username());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setEmail(registerRequest.email());
        user.setActive(true);
        user.setRole(registerRequest.role() != null ? registerRequest.role() : "OWNER");

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        // 1. Authenticate using Spring Security Manager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password())
        );

        // 2. Load User and generate tokens
        User user = userRepository.findByUsername(authRequest.username())
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        final String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(authRequest.username());

        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken.getToken(), "Bearer", user.getRole(), user.getWalletId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.refreshToken();

        // 1. Get the token directly (Service will throw exception if missing)
        RefreshToken token = refreshTokenService.findByToken(requestRefreshToken);

        // 2. Verify expiration (Service will throw exception if expired)
        token = refreshTokenService.verifyExpiration(token);

        // 3. Get the User from the token
        User user = token.getUser();

        // 4. Generate a new Access Token
        // Since you chose the alternative fix, we pass the username String
        String newAccessToken = jwtUtil.generateToken(user.getUsername());

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, requestRefreshToken));
    }
}