package com.mindcare.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.FaceCapture;

@Repository
public interface FaceCaptureRepository extends JpaRepository<FaceCapture, Long> {
    List<FaceCapture> findByCaptureTimeBetween(LocalDateTime startTime, LocalDateTime endTime);
} 