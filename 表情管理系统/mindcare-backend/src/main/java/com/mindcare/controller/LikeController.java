package com.mindcare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Post;
import com.mindcare.service.LikeService;

@RestController
@RequestMapping("/api/likes")
@CrossOrigin(origins = "http://localhost:5173")
public class LikeController {
    
    private final LikeService likeService;
    
    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }
    
    @GetMapping("/received")
    public ResponseEntity<List<Map<String, Object>>> getReceivedLikes(@RequestParam Long userId) {
        try {
            return ResponseEntity.ok(likeService.getReceivedLikes(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/liked")
    public ResponseEntity<List<Post>> getUserLikedPosts(@PathVariable Long userId) {
        try {
            List<Post> posts = likeService.getUserLikedPosts(userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/received")
    public ResponseEntity<List<Post>> getUserReceivedLikesPosts(@PathVariable Long userId) {
        try {
            List<Post> posts = likeService.getUserReceivedLikesPosts(userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 