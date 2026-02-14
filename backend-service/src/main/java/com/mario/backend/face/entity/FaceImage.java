package com.mario.backend.face.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "face_images", indexes = {
    @Index(name = "idx_face_images_image_hash", columnList = "image_hash")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "bucket_name", length = 100)
    private String bucketName;

    @Column(name = "object_name")
    private String objectName;

    @Column(name = "image_hash", length = 40)
    private String imageHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
