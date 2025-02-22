package com.mindcare.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.Post;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.status = 'normal' ORDER BY p.createTime DESC")
    Page<Post> findAllByOrderByCreateTimeDesc(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.status = 'normal' ORDER BY p.createTime DESC")
    Page<Post> findByUserIdOrderByCreateTimeDesc(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    List<Post> findByUserId(Long userId);
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.user WHERE p.id IN :postIds")
    List<Post> findAllByIdWithUser(@Param("postIds") List<Long> postIds);
    
    @Query("SELECT p FROM Post p WHERE p.userId = :userId AND p.likeCount > :minLikes AND p.status = 'normal' ORDER BY p.likeCount DESC")
    List<Post> findByUserIdAndLikeCountGreaterThanOrderByLikeCountDesc(
        @Param("userId") Long userId, 
        @Param("minLikes") Long minLikes
    );
    
    long countByCreateTimeAfter(LocalDateTime time);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:search% OR p.content LIKE %:search%) ORDER BY p.createTime DESC")
    Page<Post> findByTitleContainingOrContentContainingForAdmin(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT p FROM Post p ORDER BY p.createTime DESC")
    Page<Post> findAllForAdmin(Pageable pageable);
} 