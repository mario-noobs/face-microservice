package com.mario.backend.face.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_features")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature_vector", columnDefinition = "TEXT")
    private String featureVector;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private FaceStatus status = FaceStatus.active;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FaceStatus {
        active, inactive
    }
}
