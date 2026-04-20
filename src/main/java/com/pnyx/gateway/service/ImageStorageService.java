package com.pnyx.gateway.service;

import com.pnyx.gateway.model.ImageDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

public interface ImageStorageService {
    ImageDocument store(MultipartFile file, String uploadedBy) throws IOException;
    Optional<ImageDocument> retrieve(String id);
}
