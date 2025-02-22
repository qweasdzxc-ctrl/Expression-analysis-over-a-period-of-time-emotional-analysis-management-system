package com.mindcare.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Announcement;
import com.mindcare.service.AnnouncementService;
import com.mindcare.service.ChatService;

@RestController
@RequestMapping("/api/admin")
public class AdminManageController {
    private final ChatService chatService;
    private final AnnouncementService announcementService;
    
    public AdminManageController(
        ChatService chatService,
        AnnouncementService announcementService
    ) {
        this.chatService = chatService;
        this.announcementService = announcementService;
    }
    
    @DeleteMapping("/chat/clear")
    public ResponseEntity<?> clearChat() {
        try {
            chatService.clearAllMessages();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/announcements")
    public ResponseEntity<?> createAnnouncement(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            Announcement announcement = announcementService.createAnnouncement(content);
            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/announcements/latest")
    public ResponseEntity<?> getLatestAnnouncement() {
        try {
            Announcement announcement = announcementService.getLatestAnnouncement();
            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 