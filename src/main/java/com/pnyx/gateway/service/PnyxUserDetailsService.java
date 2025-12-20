package com.pnyx.gateway.service;

import com.pnyx.gateway.model.User;
import com.pnyx.gateway.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class PnyxUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Manual Constructor Injection
    public PnyxUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Fetch user from MongoDB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // 2. Return a Spring Security User object
        // The last parameter (new ArrayList<>()) is for Authorities/Roles. 
        // We are leaving it empty for now, but you can add roles here later.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}