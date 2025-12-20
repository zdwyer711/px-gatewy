package com.pnyx.gateway.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.pnyx.gateway.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByWalletId(String walletId);
}