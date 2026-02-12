package com.mario.backend.face.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mario.backend.common.exception.ApiException;
import com.mario.backend.face.dto.FaceResponse;
import com.mario.backend.face.entity.FaceFeature;
import com.mario.backend.face.entity.FaceImage;
import com.mario.backend.face.repository.FaceFeatureRepository;
import com.mario.backend.face.repository.FaceImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceService {

    private final FaceFeatureRepository faceFeatureRepository;
    private final FaceImageRepository faceImageRepository;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    @Value("${face-recognition.service-url:http://face-recognition-service:5000}")
    private String faceRecognitionServiceUrl;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Transactional
    public FaceResponse registerFace(Long userId, String imageData) {
        try {
            String url = faceRecognitionServiceUrl + "/face/create-identity";

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(java.util.Map.of(
                            "userId", String.valueOf(userId),
                            "imageBase64", imageData,
                            "flow", "register",
                            "requestId", java.util.UUID.randomUUID().toString(),
                            "algorithmDet", "retinaface",
                            "algorithmReg", "mobilenet"
                    )),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "";
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown response";

                if ("0000".equals(code)) {
                    // Store image in MinIO
                    String objectName = minioService.uploadImage(userId, imageData);

                    FaceImage faceImage = FaceImage.builder()
                            .userId(userId)
                            .imagePath(objectName)
                            .bucketName(minioService.getBucketName())
                            .objectName(objectName)
                            .build();
                    faceImageRepository.save(faceImage);

                    return FaceResponse.builder()
                            .success(true)
                            .code("0000")
                            .message(message)
                            .userId(userId)
                            .build();
                } else {
                    throw new ApiException(HttpStatus.BAD_REQUEST, code, message);
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to register face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", "Failed to register face: " + e.getMessage());
        }
    }

    public FaceResponse recognizeFace(Long userId, String imageData) {
        try {
            String url = faceRecognitionServiceUrl + "/face/recognize";

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(java.util.Map.of(
                            "userId", String.valueOf(userId),
                            "imageBase64", imageData,
                            "flow", "recognize",
                            "requestId", java.util.UUID.randomUUID().toString(),
                            "algorithmDet", "retinaface",
                            "algorithmReg", "mobilenet"
                    )),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "";
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown response";

                return FaceResponse.builder()
                        .success("0000".equals(code))
                        .message(message)
                        .userId(userId)
                        .code(code)
                        .data(jsonNode.has("data") ? jsonNode.get("data") : null)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to recognize face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RECOGNITION_FAILED", "Failed to recognize face: " + e.getMessage());
        }
    }

    public FaceResponse deleteFace(Long userId) {
        try {
            String url = faceRecognitionServiceUrl + "/face/delete-identity";

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(java.util.Map.of(
                            "userId", String.valueOf(userId),
                            "algorithm", "mobilenet",
                            "requestId", java.util.UUID.randomUUID().toString()
                    )),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .delete(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";
                String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown response";

                return FaceResponse.builder()
                        .success("success".equals(status))
                        .message(message)
                        .userId(userId)
                        .code("success".equals(status) ? "0000" : "DELETION_FAILED")
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to delete face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DELETION_FAILED", "Failed to delete face: " + e.getMessage());
        }
    }

    public FaceResponse isRegistered(Long userId) {
        boolean registered = faceFeatureRepository.existsByUserIdAndStatus(userId, FaceFeature.FaceStatus.active);

        return FaceResponse.builder()
                .success(true)
                .isRegistered(registered)
                .userId(userId)
                .build();
    }
}
