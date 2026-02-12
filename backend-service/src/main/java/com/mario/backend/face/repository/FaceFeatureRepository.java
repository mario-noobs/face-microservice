package com.mario.backend.face.repository;

import com.mario.backend.face.entity.FaceFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaceFeatureRepository extends JpaRepository<FaceFeature, Long> {

    Optional<FaceFeature> findByUserIdAndStatus(Long userId, FaceFeature.FaceStatus status);

    boolean existsByUserIdAndStatus(Long userId, FaceFeature.FaceStatus status);

    void deleteByUserId(Long userId);
}
