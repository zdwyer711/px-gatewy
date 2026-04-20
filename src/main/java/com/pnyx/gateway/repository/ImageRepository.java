package com.pnyx.gateway.repository;

import com.pnyx.gateway.model.ImageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ImageRepository extends MongoRepository<ImageDocument, String> {
    List<ImageDocument> findByAssociatedEntityId(String associatedEntityId);
}
