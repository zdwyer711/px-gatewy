package com.pnyx.gateway.repository;

import com.pnyx.gateway.model.RefreshToken;
import com.pnyx.gateway.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);
    int deleteByUser(User user); // Returns count of deleted items
}