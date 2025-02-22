package com.mindcare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.ViewHistory;

@Repository
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {
    List<ViewHistory> findByUserIdOrderByViewTimeDesc(Long userId);
    
    @Query("SELECT COUNT(DISTINCT v.postId) FROM ViewHistory v WHERE v.userId = :userId")
    long countDistinctPostsByUserId(@Param("userId") Long userId);

    void deleteByPostId(Long postId);
} 