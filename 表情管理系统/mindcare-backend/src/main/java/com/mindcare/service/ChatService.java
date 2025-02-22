package com.mindcare.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindcare.entity.ChatMessage;
import com.mindcare.entity.User;
import com.mindcare.repository.ChatMessageRepository;
import com.mindcare.repository.UserRepository;

@Service
@Transactional
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    
    public ChatService(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages() {
        try {
            // 使用分页获取最新的50条消息
            Pageable pageable = PageRequest.of(0, 50);
            List<ChatMessage> messages = chatMessageRepository.findTopMessages(pageable);
            
            // 确保所有关联的用户信息都被加载
            messages.forEach(message -> {
                if (message.getUser() != null) {
                    message.getUser().getUsername(); // 触发懒加载
                }
            });
            
            return messages;
        } catch (Exception e) {
            log.error("获取聊天消息失败", e);
            throw new RuntimeException("获取聊天消息失败", e);
        }
    }
    
    @Transactional
    public ChatMessage saveMessage(ChatMessage message) {
        try {
            // 获取发送消息的用户信息
            User user = userRepository.findById(message.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
                
            message.setUser(user);
            message.setCreateTime(LocalDateTime.now());
            return chatMessageRepository.save(message);
        } catch (Exception e) {
            log.error("保存聊天消息失败", e);
            throw new RuntimeException("保存聊天消息失败", e);
        }
    }
    
    @Transactional
    public void clearAllMessages() {
        chatMessageRepository.deleteAll();
    }
} 