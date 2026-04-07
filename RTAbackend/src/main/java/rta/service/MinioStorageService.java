package rta.service;

import io.minio.*;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinioStorageService - Handles file upload/download/delete operations using
 * MinIO object storage - Automatically creates the bucket if it doesn't exist
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        // Create bucket if it doesn't exist
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            } else {
                log.info("MinIO bucket already exists: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize MinIO storage", e);
        }
    }

    /**
     * Upload a file to MinIO
     *
     * @param fileName the object name/key in the bucket
     * @param file the MultipartFile to upload
     */
    public void uploadFile(String fileName, MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded file to MinIO: {}", fileName);
        }
    }

    /**
     * Upload byte array to MinIO
     *
     * @param fileName the object name/key in the bucket
     * @param data the byte array to upload
     * @param contentType the content type of the file
     */
    public void uploadFile(String fileName, byte[] data, String contentType) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            log.info("Uploaded file to MinIO: {}", fileName);
        }
    }

    /**
     * Download a file from MinIO as byte array
     *
     * @param fileName the object name/key in the bucket
     * @return byte array of the file content
     */
    public byte[] downloadFile(String fileName) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build())) {
            return stream.readAllBytes();
        }
    }

    /**
     * Download a file from MinIO as InputStream
     *
     * @param fileName the object name/key in the bucket
     * @return InputStream of the file content
     */
    public InputStream downloadFileAsStream(String fileName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build());
    }

    /**
     * Check if a file exists in MinIO
     *
     * @param fileName the object name/key in the bucket
     * @return true if the file exists
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            throw new RuntimeException("Error checking file existence", e);
        } catch (Exception e) {
            throw new RuntimeException("Error checking file existence", e);
        }
    }

    /**
     * Delete a file from MinIO
     *
     * @param fileName the object name/key in the bucket
     */
    public void deleteFile(String fileName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build());
        log.info("Deleted file from MinIO: {}", fileName);
    }

    /**
     * Get the bucket name
     */
    public String getBucket() {
        return bucket;
    }
}
