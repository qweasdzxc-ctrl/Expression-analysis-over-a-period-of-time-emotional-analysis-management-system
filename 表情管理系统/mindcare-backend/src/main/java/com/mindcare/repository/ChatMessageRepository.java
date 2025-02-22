package com.mindcare.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mindcare.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 按创建时间倒序获取所有消息
    List<ChatMessage> findAllByOrderByCreateTimeDesc();
    
    // 按创建时间倒序获取指定数量的消息
    @Query("SELECT m FROM ChatMessage m ORDER BY m.createTime DESC")
    List<ChatMessage> findTopMessages(Pageable pageable);
    
    // 获取指定用户的消息数量
    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
} 