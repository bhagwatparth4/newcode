package com.cafe.backend.service.storage;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

/*
 * Cloudinary (STORAGE_PROVIDER=cloudinary).
 *
 * Needs: CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.
 * Uses Cloudinary's documented "signed upload" REST call directly
 * (no extra library). The API secret stays on the server.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
public class CloudinaryImageStorage implements ImageStorage {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String rootFolder;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CloudinaryImageStorage(
            @Value("${app.storage.cloudinary.cloud-name}") String cloudName,
            @Value("${app.storage.cloudinary.api-key}") String apiKey,
            @Value("${app.storage.cloudinary.api-secret}") String apiSecret,
            @Value("${app.storage.cloudinary.folder:rushhour}") String rootFolder
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.rootFolder = rootFolder;
    }

    @Override
    public String store(byte[] bytes, String contentType, String objectKey) {

        // objectKey = "menu/<uuid>.jpg" -> folder "rushhour/menu", public id "<uuid>"
        int slash = objectKey.lastIndexOf('/');
        String folder = rootFolder + "/" + objectKey.substring(0, slash);
        String fileName = objectKey.substring(slash + 1);
        String publicId = fileName.substring(0, fileName.lastIndexOf('.'));
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // Signature = SHA-1 of the sorted parameters + API secret (Cloudinary docs)
        String toSign = "folder=" + folder + "&public_id=" + publicId + "&timestamp=" + timestamp;
        String signature = sha1Hex(toSign + apiSecret);

        String boundary = "----RushHourUpload" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "api_key", apiKey);
        writeField(body, boundary, "timestamp", timestamp);
        writeField(body, boundary, "folder", folder);
        writeField(body, boundary, "public_id", publicId);
        writeField(body, boundary, "signature", signature);
        writeFile(body, boundary, fileName, contentType, bytes);
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Cloudinary upload failed with HTTP " + response.statusCode()
                );
            }

            String url = new JSONObject(response.body()).optString("secure_url", "");
            if (url.isBlank()) {
                throw new IllegalStateException("Cloudinary did not return a URL");
            }
            return url;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Photo upload was interrupted", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not reach Cloudinary", e);
        }
    }

    private static void writeField(ByteArrayOutputStream out, String boundary, String name, String value) {
        out.writeBytes(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFile(ByteArrayOutputStream out, String boundary, String fileName,
                                  String contentType, byte[] bytes) {
        out.writeBytes(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.writeBytes(bytes);
        out.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
