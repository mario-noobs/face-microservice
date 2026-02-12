package com.mario.backend.face.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:face-images}")
    private String bucketName;

    public String uploadImage(Long userId, String base64Image) {
        try {
            ensureBucketExists();

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            String objectName = String.format("%d/%s.jpg", userId, UUID.randomUUID());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                            .contentType("image/jpeg")
                            .build()
            );

            log.info("Image uploaded successfully: {}/{}", bucketName, objectName);
            return objectName;

        } catch (Exception e) {
            log.error("Failed to upload image to MinIO", e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    public byte[] downloadImage(String objectName) {
        try {
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return response.readAllBytes();

        } catch (Exception e) {
            log.error("Failed to download image from MinIO", e);
            throw new RuntimeException("Failed to download image", e);
        }
    }

    public void deleteImage(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("Image deleted successfully: {}/{}", bucketName, objectName);

        } catch (Exception e) {
            log.error("Failed to delete image from MinIO", e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    private void ensureBucketExists() throws ServerException, InsufficientDataException, ErrorResponseException,
            IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Bucket created: {}", bucketName);
        }
    }

    public String getBucketName() {
        return bucketName;
    }
}
