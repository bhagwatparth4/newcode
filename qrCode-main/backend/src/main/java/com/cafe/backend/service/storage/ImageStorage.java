package com.cafe.backend.service.storage;

/*
 * Where uploaded photos are kept. Exactly ONE implementation is active,
 * chosen with the STORAGE_PROVIDER environment variable:
 *
 *   local       (default) -> saved on this server, good for development
 *   s3          -> AWS S3 (or any S3-compatible storage)
 *   cloudinary  -> Cloudinary
 *
 * Only the returned public URL is saved in the database (menu_items.image_url /
 * categories.image_url). The photo itself never goes into Postgres.
 */
public interface ImageStorage {

    /**
     * @param bytes       validated image bytes
     * @param contentType image/jpeg, image/png or image/webp
     * @param objectKey   safe, random path such as "menu/3f2a....jpg"
     * @return public https URL of the stored photo
     */
    String store(byte[] bytes, String contentType, String objectKey);
}
