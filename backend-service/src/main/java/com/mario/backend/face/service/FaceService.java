package com.mario.backend.face.service;

import com.mario.backend.common.exception.ApiException;
import com.mario.backend.common.exception.ErrorCode;
import com.mario.backend.common.http.ExternalServiceResponse;
import com.mario.backend.common.http.HttpClientService;
import com.mario.backend.face.dto.FaceResponse;
import com.mario.backend.face.entity.FaceFeature;
import com.mario.backend.face.entity.FaceImage;
import com.mario.backend.face.repository.FaceFeatureRepository;
import com.mario.backend.face.repository.FaceImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceService {

    private final FaceFeatureRepository faceFeatureRepository;
    private final FaceImageRepository faceImageRepository;
    private final MinioService minioService;
    private final HttpClientService httpClientService;

    @Value("${face-recognition.service-url:http://face-recognition-service:5000}")
    private String faceRecognitionServiceUrl;

    @Transactional
    public FaceResponse registerFace(Long userId, String imageData) {
        try {
            String url = faceRecognitionServiceUrl + "/face/create-identity";

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.post(url, Map.of(
                    "userId", String.valueOf(userId),
                    "imageBase64", imageData,
                    "flow", "register",
                    "requestId", UUID.randomUUID().toString(),
                    "algorithmDet", "retinaface",
                    "algorithmReg", "mobilenet"
            )));

            if (response.isSuccess()) {
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
                        .message(response.getMessage())
                        .userId(userId)
                        .build();
            } else {
                throw new ApiException(ErrorCode.FACE_REGISTRATION_FAILED, response.getMessage());
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to register face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_REGISTRATION_FAILED, "Failed to register face: " + e.getMessage());
        }
    }

    public FaceResponse recognizeFace(Long userId, String imageData) {
        try {
            String url = faceRecognitionServiceUrl + "/face/recognize";

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.post(url, Map.of(
                    "userId", String.valueOf(userId),
                    "imageBase64", imageData,
                    "flow", "recognize",
                    "requestId", UUID.randomUUID().toString(),
                    "algorithmDet", "retinaface",
                    "algorithmReg", "mobilenet"
            )));

            return FaceResponse.builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .userId(userId)
                    .code(response.getCode())
                    .data(response.getData())
                    .build();
        } catch (Exception e) {
            log.error("Failed to recognize face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_RECOGNITION_FAILED, "Failed to recognize face: " + e.getMessage());
        }
    }

    public FaceResponse deleteFace(Long userId) {
        try {
            String url = faceRecognitionServiceUrl + "/face/delete-identity";

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.delete(url, Map.of(
                    "userId", String.valueOf(userId),
                    "algorithm", "mobilenet",
                    "requestId", UUID.randomUUID().toString()
            )));

            return FaceResponse.builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .userId(userId)
                    .code(response.isSuccess() ? "0000" : ErrorCode.FACE_DELETION_FAILED.getCode())
                    .build();
        } catch (Exception e) {
            log.error("Failed to delete face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_DELETION_FAILED, "Failed to delete face: " + e.getMessage());
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
