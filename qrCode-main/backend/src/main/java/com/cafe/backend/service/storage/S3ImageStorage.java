package com.cafe.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

/*
 * AWS S3 (STORAGE_PROVIDER=s3).
 *
 * Needs: S3_BUCKET, S3_REGION, S3_ACCESS_KEY, S3_SECRET_KEY.
 * Optional:
 *   S3_PUBLIC_BASE_URL -> e.g. a CloudFront URL. Default:
 *                         https://<bucket>.s3.<region>.amazonaws.com
 *   S3_ENDPOINT        -> only for S3-compatible providers (not AWS itself)
 *
 * The bucket must allow public READ of the uploaded photos (see SETUP.md).
 * Keys stay on the server; the browser never sees them.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3ImageStorage implements ImageStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3ImageStorage(
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.region}") String region,
            @Value("${app.storage.s3.access-key}") String accessKey,
            @Value("${app.storage.s3.secret-key}") String secretKey,
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.bucket = bucket;

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                );

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        this.s3Client = builder.build();

        String base = (publicBaseUrl == null || publicBaseUrl.isBlank())
                ? "https://" + bucket + ".s3." + region + ".amazonaws.com"
                : publicBaseUrl;
        this.publicBaseUrl = base.replaceAll("/+$", "");
    }

    @Override
    public String store(byte[] bytes, String contentType, String objectKey) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                // Photos never change (every upload gets a new name), so browsers may cache them for a year
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));

        return publicBaseUrl + "/" + objectKey;
    }
}
