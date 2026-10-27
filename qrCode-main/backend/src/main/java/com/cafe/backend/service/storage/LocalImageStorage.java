package com.cafe.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * DEVELOPMENT default (STORAGE_PROVIDER=local or not set).
 * Saves photos in a folder on this server and serves them at /uploads/...
 * Use s3 or cloudinary in production: many cloud servers delete local
 * files on every restart/deploy.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalImageStorage implements ImageStorage {

    private final Path rootDirectory;
    private final String publicBackendUrl;

    public LocalImageStorage(
            @Value("${app.storage.local-dir:uploads}") String localDirectory,
            @Value("${app.public-backend-url:http://localhost:8080}") String publicBackendUrl
    ) {
        this.rootDirectory = Path.of(localDirectory).toAbsolutePath().normalize();
        this.publicBackendUrl = publicBackendUrl.replaceAll("/+$", "");
    }

    @Override
    public String store(byte[] bytes, String contentType, String objectKey) {
        try {
            Path target = rootDirectory.resolve(objectKey).normalize();

            // Defence in depth: never write outside the uploads folder
            if (!target.startsWith(rootDirectory)) {
                throw new IllegalStateException("Invalid storage path");
            }

            Files.createDirectories(target.getParent());
            Files.write(target, bytes);

            return publicBackendUrl + "/uploads/" + objectKey;

        } catch (IOException e) {
            throw new IllegalStateException("Could not save the photo", e);
        }
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }
}
