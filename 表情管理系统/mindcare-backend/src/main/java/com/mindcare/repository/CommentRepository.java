package com.mindcare.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mindcare.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // 按帖子ID查询评论，按时间倒序排序
    List<Comment> findByPostIdOrderByCreateTimeDesc(Long postId);
    
    // 按用户ID查询评论，按时间倒序排序
    List<Comment> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    // 按帖子ID删除评论
    void deleteByPostId(Long postId);
    
    // 根据内容搜索评论，支持分页
    @Query("SELECT c FROM Comment c WHERE c.content LIKE %:content% ORDER BY c.createTime DESC")
    Page<Comment> findByContentContaining(@Param("content") String content, Pageable pageable);
    
    // 获取所有评论，按创建时间倒序排序，支持分页
    @Query("SELECT c FROM Comment c ORDER BY c.createTime DESC")
    Page<Comment> findAllByOrderByCreateTimeDesc(Pageable pageable);
    
    // 获取指定用户的评论数量
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
} 