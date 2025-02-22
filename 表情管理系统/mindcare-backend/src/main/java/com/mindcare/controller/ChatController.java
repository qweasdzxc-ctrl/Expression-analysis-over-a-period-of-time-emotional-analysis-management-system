package com.mindcare.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.ChatMessage;
import com.mindcare.service.ChatService;
import com.mindcare.service.OnlineUserService;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    private final ChatService chatService;
    private final OnlineUserService onlineUserService;
    private final HttpServletRequest request;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    public ChatController(
        ChatService chatService, 
        OnlineUserService onlineUserService,
        HttpServletRequest request
    ) {
        this.chatService = chatService;
        this.onlineUserService = onlineUserService;
        this.request = request;
    }
    
    @PostMapping("/send")
    public ResponseEntity<ChatMessage> sendMessage(@RequestBody ChatMessage message) {
        try {
            Long userId = getCurrentUserId();
            message.setUserId(userId);
            ChatMessage savedMessage = chatService.saveMessage(message);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(savedMessage);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> getMessages() {
        try {
            List<ChatMessage> messages = chatService.getMessages();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(messages);
        } catch (Exception e) {
            log.error("获取聊天消息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/online-count")
    public ResponseEntity<Integer> getOnlineCount() {
        return ResponseEntity.ok(onlineUserService.getOnlineCount());
    }
    
    @PostMapping("/online")
    public ResponseEntity<?> userOnline() {
        Long userId = getCurrentUserId();
        onlineUserService.userOnline(userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/offline")
    public ResponseEntity<?> userOffline() {
        Long userId = getCurrentUserId();
        onlineUserService.userOffline(userId);
        return ResponseEntity.ok().build();
    }
    
    // 获取当前登录用户ID的辅助方法
    private Long getCurrentUserId() {
        // 从请求头中获取用户ID
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            return Long.parseLong(userIdStr);
        }
        throw new RuntimeException("未登录");
    }
}

class MessageRequest {
    private String content;
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
} 