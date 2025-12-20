package com.pnyx.gateway.service;

import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PnyxUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PnyxUserDetailsService userDetailsService;

    @Test
    public void testLoadUserByUsername_Success() {
        // Prepare mock data
        User mockUser = new User();
        mockUser.setUsername("foundUser");
        mockUser.setPassword("encodedPass");

        // Define mock behavior
        when(userRepository.findByUsername("foundUser")).thenReturn(Optional.of(mockUser));

        // Execute
        UserDetails userDetails = userDetailsService.loadUserByUsername("foundUser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("foundUser", userDetails.getUsername());
        assertEquals("encodedPass", userDetails.getPassword());
    }

    @Test
    public void testLoadUserByUsername_NotFound_ThrowsException() {
        // Define mock behavior
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Assert exception
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknown");
        });
    }
}