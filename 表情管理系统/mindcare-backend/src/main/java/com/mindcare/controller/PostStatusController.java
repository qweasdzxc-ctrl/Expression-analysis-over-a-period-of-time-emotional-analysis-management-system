package com.mindcare.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.service.CollectionService;
import com.mindcare.service.LikeService;
import com.mindcare.util.SecurityUtils;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:5173")
public class PostStatusController {
    
    private final LikeService likeService;
    private final CollectionService collectionService;
    
    public PostStatusController(LikeService likeService, CollectionService collectionService) {
        this.likeService = likeService;
        this.collectionService = collectionService;
    }
    
    @GetMapping("/{postId}/status")
    public ResponseEntity<Map<String, Boolean>> getPostStatus(@PathVariable Long postId) {
        try {
            // 如果用户未登录，返回默认状态
            if (!SecurityUtils.isAuthenticated()) {
                Map<String, Boolean> defaultStatus = new HashMap<>();
                defaultStatus.put("isLiked", false);
                defaultStatus.put("isCollected", false);
                return ResponseEntity.ok(defaultStatus);
            }
            
            Map<String, Boolean> status = new HashMap<>();
            status.put("isLiked", likeService.hasUserLikedPost(postId));
            status.put("isCollected", collectionService.hasUserCollectedPost(postId));
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            e.printStackTrace(); // 添加日志输出
            return ResponseEntity.internalServerError().build();
        }
    }
} 