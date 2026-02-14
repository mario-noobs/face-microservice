package com.mario.backend.face.repository;

import com.mario.backend.face.entity.FaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaceImageRepository extends JpaRepository<FaceImage, Long> {

    List<FaceImage> findByUserId(Long userId);

    boolean existsByUserIdAndImageHash(Long userId, String imageHash);

    void deleteByUserId(Long userId);
}
