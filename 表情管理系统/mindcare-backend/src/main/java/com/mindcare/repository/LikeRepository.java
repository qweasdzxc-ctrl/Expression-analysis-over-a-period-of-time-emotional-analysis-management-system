package com.mindcare.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.Like;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);
    Optional<Like> findByUserIdAndCommentId(Long userId, Long commentId);
    
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    
    void deleteByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
    
    // 统计用户点赞数
    @Query("SELECT COUNT(l) FROM Like l WHERE l.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    // 统计帖子的点赞总数
    @Query("SELECT COUNT(l) FROM Like l WHERE l.postId = :postId")
    Long countByPostId(@Param("postId") Long postId);
    
    // 统计用户获得的点赞数（包括帖子和评论的点赞）
    @Query("SELECT COUNT(l) FROM Like l WHERE l.postId IN (SELECT p.id FROM Post p WHERE p.userId = :userId)")
    Long countReceivedLikes(@Param("userId") Long userId);

    // 添加获取用户点赞记录的方法
    List<Like> findByUserId(Long userId);

    List<Like> findByPostIdOrderByCreateTimeDesc(Long postId);
    
    List<Like> findByUserIdOrderByCreateTimeDesc(Long userId);

    void deleteByPostId(Long postId);
} 