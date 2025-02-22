package com.mindcare.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Post;
import com.mindcare.entity.User;
import com.mindcare.service.CollectionService;
import com.mindcare.service.UserProfileService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final CollectionService collectionService;
    private final HttpServletRequest request;
    
    public UserProfileController(UserProfileService userProfileService, CollectionService collectionService, HttpServletRequest request) {
        this.userProfileService = userProfileService;
        this.collectionService = collectionService;
        this.request = request;
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable Long userId) {
        try {
            User user = userProfileService.getProfile(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/")
    public ResponseEntity<User> updateProfile(@RequestBody User user) {
        // TODO: 从认证信息中获取用户ID
        Long userId = 1L; // 临时写死
        return ResponseEntity.ok(userProfileService.updateProfile(userId, user));
    }
    
    @GetMapping("/posts/{userId}")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable Long userId) {
        try {
            List<Post> posts = userProfileService.getUserPosts(userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/collections")
    public ResponseEntity<List<Post>> getUserCollections(@RequestParam Long userId) {
        try {
            List<Post> collectedPosts = collectionService.getCollectedPosts(userId);
            return ResponseEntity.ok(collectedPosts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Long>> getUserStats(@PathVariable Long userId) {
        try {
            Map<String, Long> stats = userProfileService.getUserStats(userId);
            if (stats == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 