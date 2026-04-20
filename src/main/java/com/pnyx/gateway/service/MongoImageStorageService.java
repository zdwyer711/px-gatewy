package com.pnyx.gateway.service;

import com.pnyx.gateway.model.ImageDocument;
import com.pnyx.gateway.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class MongoImageStorageService implements ImageStorageService {

    private final ImageRepository imageRepository;

    public MongoImageStorageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public ImageDocument store(MultipartFile file, String uploadedBy) throws IOException {
        byte[] data = file.getBytes();

        ImageDocument doc = new ImageDocument();
        doc.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
        doc.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setData(data);
        doc.setSha256Hash(sha256Hex(data));
        doc.setUploadedBy(uploadedBy);
        doc.setStorageType("MONGODB");
        doc.setUploadedAt(Instant.now());

        return imageRepository.save(doc);
    }

    @Override
    public Optional<ImageDocument> retrieve(String id) {
        return imageRepository.findById(id);
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
