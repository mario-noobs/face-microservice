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
        if (faceFeatureRepository.existsByUserIdAndStatus(userId, FaceFeature.FaceStatus.active)) {
            throw new ApiException(HttpStatus.CONFLICT, "FACE_ALREADY_REGISTERED", "Face already registered for this user");
        }

        String objectName = minioService.uploadImage(userId, imageData);

        try {
            String featureVector = extractFeatures(imageData);

            FaceFeature faceFeature = FaceFeature.builder()
                    .userId(userId)
                    .featureVector(featureVector)
                    .status(FaceFeature.FaceStatus.active)
                    .build();
            faceFeatureRepository.save(faceFeature);

            FaceImage faceImage = FaceImage.builder()
                    .userId(userId)
                    .imagePath(objectName)
                    .bucketName(minioService.getBucketName())
                    .objectName(objectName)
                    .build();
            faceImageRepository.save(faceImage);

            return FaceResponse.builder()
                    .success(true)
                    .message("Face registered successfully")
                    .userId(userId)
                    .build();

        } catch (Exception e) {
            minioService.deleteImage(objectName);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "REGISTRATION_FAILED", "Failed to register face: " + e.getMessage());
        }
    }

    public FaceResponse recognizeFace(Long userId, String imageData) {
        FaceFeature storedFeature = faceFeatureRepository.findByUserIdAndStatus(userId, FaceFeature.FaceStatus.active)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FACE_NOT_REGISTERED", "No registered face found for this user"));

        try {
            double confidence = compareFaces(imageData, storedFeature.getFeatureVector());

            boolean isMatch = confidence >= 0.7;

            return FaceResponse.builder()
                    .success(isMatch)
                    .message(isMatch ? "Face recognized successfully" : "Face recognition failed")
                    .userId(userId)
                    .confidence(confidence)
                    .build();

        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RECOGNITION_FAILED", "Failed to recognize face: " + e.getMessage());
        }
    }

    @Transactional
    public FaceResponse deleteFace(Long userId) {
        FaceFeature faceFeature = faceFeatureRepository.findByUserIdAndStatus(userId, FaceFeature.FaceStatus.active)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FACE_NOT_REGISTERED", "No registered face found for this user"));

        faceFeature.setStatus(FaceFeature.FaceStatus.inactive);
        faceFeatureRepository.save(faceFeature);

        return FaceResponse.builder()
                .success(true)
                .message("Face deleted successfully")
                .userId(userId)
                .build();
    }

    public FaceResponse isRegistered(Long userId) {
        boolean registered = faceFeatureRepository.existsByUserIdAndStatus(userId, FaceFeature.FaceStatus.active);

        return FaceResponse.builder()
                .success(true)
                .isRegistered(registered)
                .userId(userId)
                .build();
    }

    private String extractFeatures(String imageData) throws IOException {
        String url = faceRecognitionServiceUrl + "/extract";

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(java.util.Map.of("image_data", imageData)),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Feature extraction failed: " + response.code());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            return jsonNode.get("features").toString();
        }
    }

    private double compareFaces(String imageData, String storedFeatures) throws IOException {
        String url = faceRecognitionServiceUrl + "/compare";

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(java.util.Map.of(
                        "image_data", imageData,
                        "stored_features", storedFeatures
                )),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Face comparison failed: " + response.code());
            }

            JsonNode jsonNode = objectMapper.readTree(response.body().string());
            return jsonNode.get("confidence").asDouble();
        }
    }
}
