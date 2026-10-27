package com.cafe.backend.service.storage;

import com.cafe.backend.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/*
 * Checks an uploaded photo and hands it to the active ImageStorage.
 *
 * Safety rules:
 *  - Only JPG, PNG, WEBP. The type is detected from the file's real bytes,
 *    never trusted from the file name or the browser.
 *  - Maximum 5 MB.
 *  - The stored file always gets a new random name, so an upload can never
 *    overwrite another file or use a dangerous name.
 *  - Only known folders ("menu", "category").
 */
@Service
public class ImageUploadService {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_FOLDERS = Set.of("menu", "category");

    private final ImageStorage imageStorage;

    public ImageUploadService(ImageStorage imageStorage) {
        this.imageStorage = imageStorage;
    }

    public String upload(MultipartFile file, String folder) {

        if (folder == null || !ALLOWED_FOLDERS.contains(folder)) {
            throw new BadRequestException("INVALID_FOLDER", "Invalid upload folder");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("IMAGE_REQUIRED", "Please choose a photo");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("IMAGE_TOO_LARGE", "Photo is too large. Maximum size is 5 MB.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("IMAGE_UNREADABLE", "Could not read the photo. Please try again.");
        }

        ImageType type = ImageType.detect(bytes);

        if (type == null) {
            throw new BadRequestException(
                    "IMAGE_TYPE_NOT_ALLOWED",
                    "Only JPG, PNG or WEBP photos are allowed."
            );
        }

        String objectKey = folder + "/" + UUID.randomUUID() + "." + type.extension;

        return imageStorage.store(bytes, type.contentType, objectKey);
    }

    // Recognises images by their first bytes ("magic numbers")
    enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageType detect(byte[] b) {
            if (b == null || b.length < 12) {
                return null;
            }
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
                return PNG;
            }
            if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }
    }
}
