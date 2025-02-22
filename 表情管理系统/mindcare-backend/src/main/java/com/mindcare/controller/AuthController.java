package com.mindcare.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Admin;
import com.mindcare.entity.Announcement;
import com.mindcare.entity.User;
import com.mindcare.service.AdminService;
import com.mindcare.service.AnnouncementService;
import com.mindcare.service.UserService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    
    private final UserService userService;
    private final AdminService adminService;
    private final AnnouncementService announcementService;
    
    public AuthController(UserService userService, AdminService adminService, AnnouncementService announcementService) {
        this.userService = userService;
        this.adminService = adminService;
        this.announcementService = announcementService;
    }
    
    @PostMapping("/user/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/user/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        try {
            User user = userService.login(credentials.get("username"), credentials.get("password"));
            String sessionId = UUID.randomUUID().toString(); // 生成会话ID
            
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("sessionId", sessionId);
            
            // 获取最新公告
            Announcement announcement = announcementService.getLatestAnnouncement();
            if (announcement != null) {
                response.put("announcement", announcement);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@RequestBody Admin admin) {
        try {
            Admin registeredAdmin = adminService.register(admin);
            return ResponseEntity.ok(registeredAdmin);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/admin/login")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> credentials) {
        try {
            Admin admin = adminService.login(credentials.get("username"), credentials.get("password"));
            String sessionId = UUID.randomUUID().toString(); // 生成会话ID
            
            Map<String, Object> response = new HashMap<>();
            response.put("admin", admin);
            response.put("sessionId", sessionId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 