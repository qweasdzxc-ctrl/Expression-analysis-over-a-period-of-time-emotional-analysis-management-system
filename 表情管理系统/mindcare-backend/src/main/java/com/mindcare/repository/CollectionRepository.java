package com.mindcare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.Collection;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    Optional<Collection> findByUserIdAndPostId(Long userId, Long postId);
    List<Collection> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    
    @Query("SELECT COUNT(c) FROM Collection c WHERE c.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(c) FROM Collection c WHERE c.postId = :postId")
    Long countByPostId(@Param("postId") Long postId);

    void deleteByPostId(Long postId);
} 