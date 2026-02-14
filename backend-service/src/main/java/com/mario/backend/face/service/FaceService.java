package com.mario.backend.face.service;

import com.mario.backend.common.exception.ApiException;
import com.mario.backend.common.exception.ErrorCode;
import com.mario.backend.common.http.ExternalServiceResponse;
import com.mario.backend.common.http.HttpClientException;
import com.mario.backend.common.http.HttpClientService;
import com.mario.backend.common.http.NonRetryableHttpException;
import com.mario.backend.logging.annotation.Traceable;
import com.mario.backend.logging.context.TraceContext;
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

import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceService {

    private final FaceFeatureRepository faceFeatureRepository;
    private final FaceImageRepository faceImageRepository;
    private final MinioService minioService;
    private final HttpClientService httpClientService;
    private final IdempotencyService idempotencyService;

    @Value("${face-recognition.service-url:http://face-recognition-service:5000}")
    private String faceRecognitionServiceUrl;

    @Traceable("face.registerFace")
    @Transactional
    public FaceResponse registerFace(Long userId, String imageData) {
        String imageHash = idempotencyService.computeImageHash(imageData);

        if (faceImageRepository.existsByUserIdAndImageHash(userId, imageHash)) {
            throw new ApiException(ErrorCode.FACE_ALREADY_REGISTERED);
        }

        try {
            IdempotencyService.setCurrentKey(imageHash);

            String url = faceRecognitionServiceUrl + "/face/create-identity";
            String requestId = ofNullable(TraceContext.getTraceId()).orElseGet(() -> UUID.randomUUID().toString());

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.post(url, Map.of(
                    "userId", String.valueOf(userId),
                    "imageBase64", imageData,
                    "flow", "register",
                    "requestId", requestId,
                    "algorithmDet", "retinaface",
                    "algorithmReg", "mobilenet"
            )));

            if (response.isSuccess()) {
                String encoding = response.getData() != null && response.getData().has("face_encoding_base64")
                        ? response.getData().get("face_encoding_base64").asText()
                        : null;

                if (encoding != null) {
                    FaceFeature faceFeature = FaceFeature.builder()
                            .userId(userId)
                            .featureVector(encoding)
                            .status(FaceFeature.FaceStatus.active)
                            .build();
                    faceFeatureRepository.save(faceFeature);
                }

                String objectName = minioService.uploadImage(userId, imageData);
                FaceImage faceImage = FaceImage.builder()
                        .userId(userId)
                        .imagePath(objectName)
                        .bucketName(minioService.getBucketName())
                        .objectName(objectName)
                        .imageHash(imageHash)
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
        } catch (NonRetryableHttpException e) {
            log.error("External service rejected face registration for userId={}: status={}, message={}",
                    userId, e.getHttpStatusCode(), e.getMessage());
            throw new ApiException(ErrorCode.FACE_REGISTRATION_FAILED, e.getMessage());
        } catch (HttpClientException e) {
            log.error("External service unavailable during face registration for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to register face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_REGISTRATION_FAILED, "Failed to register face: " + e.getMessage());
        } finally {
            IdempotencyService.clearCurrentKey();
        }
    }

    @Traceable("face.recognizeFace")
    public FaceResponse recognizeFace(Long userId, String imageData) {
        try {
            String url = faceRecognitionServiceUrl + "/face/recognize";
            String requestId = ofNullable(TraceContext.getTraceId()).orElseGet(() -> UUID.randomUUID().toString());

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.post(url, Map.of(
                    "userId", String.valueOf(userId),
                    "imageBase64", imageData,
                    "flow", "recognize",
                    "requestId", requestId,
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
        } catch (NonRetryableHttpException e) {
            log.error("External service rejected face recognition for userId={}: status={}, message={}",
                    userId, e.getHttpStatusCode(), e.getMessage());
            throw new ApiException(ErrorCode.FACE_RECOGNITION_FAILED, e.getMessage());
        } catch (HttpClientException e) {
            log.error("External service unavailable during face recognition for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to recognize face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_RECOGNITION_FAILED, "Failed to recognize face: " + e.getMessage());
        }
    }

    @Traceable("face.deleteFace")
    @Transactional
    public FaceResponse deleteFace(Long userId) {
        try {
            String url = faceRecognitionServiceUrl + "/face/delete-identity";
            String requestId = ofNullable(TraceContext.getTraceId()).orElseGet(() -> UUID.randomUUID().toString());

            ExternalServiceResponse response = new ExternalServiceResponse(httpClientService.delete(url, Map.of(
                    "userId", String.valueOf(userId),
                    "algorithm", "mobilenet",
                    "requestId", requestId
            )));

            if (response.isSuccess()) {
                faceFeatureRepository.findByUserIdAndStatus(userId, FaceFeature.FaceStatus.active)
                        .ifPresent(feature -> {
                            feature.setStatus(FaceFeature.FaceStatus.inactive);
                            faceFeatureRepository.save(feature);
                        });
            }

            return FaceResponse.builder()
                    .success(response.isSuccess())
                    .message(response.getMessage())
                    .userId(userId)
                    .code(response.isSuccess() ? "0000" : ErrorCode.FACE_DELETION_FAILED.getCode())
                    .build();
        } catch (NonRetryableHttpException e) {
            log.error("External service rejected face deletion for userId={}: status={}, message={}",
                    userId, e.getHttpStatusCode(), e.getMessage());
            throw new ApiException(ErrorCode.FACE_DELETION_FAILED, e.getMessage());
        } catch (HttpClientException e) {
            log.error("External service unavailable during face deletion for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_RETRY_EXHAUSTED, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete face for userId={}: {}", userId, e.getMessage());
            throw new ApiException(ErrorCode.FACE_DELETION_FAILED, "Failed to delete face: " + e.getMessage());
        }
    }

    @Traceable("face.isRegistered")
    public FaceResponse isRegistered(Long userId) {
        boolean registered = faceFeatureRepository.existsByUserIdAndStatus(userId, FaceFeature.FaceStatus.active);

        return FaceResponse.builder()
                .success(true)
                .isRegistered(registered)
                .userId(userId)
                .build();
    }
}
