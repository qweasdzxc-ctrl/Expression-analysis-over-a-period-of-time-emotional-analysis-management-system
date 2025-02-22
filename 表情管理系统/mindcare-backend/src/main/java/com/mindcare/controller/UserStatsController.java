package com.mindcare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Post;
import com.mindcare.entity.ViewHistory;
import com.mindcare.repository.ViewHistoryRepository;
import com.mindcare.service.CollectionService;
import com.mindcare.service.LikeService;
import com.mindcare.service.PostService;
import com.mindcare.service.UserStatsService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserStatsController {
    private final PostService postService;
    private final LikeService likeService;
    private final CollectionService collectionService;
    private final ViewHistoryRepository viewHistoryRepository;
    private final UserStatsService userStatsService;
    
    public UserStatsController(PostService postService,
                             LikeService likeService,
                             CollectionService collectionService,
                             ViewHistoryRepository viewHistoryRepository,
                             UserStatsService userStatsService) {
        this.postService = postService;
        this.likeService = likeService;
        this.collectionService = collectionService;
        this.viewHistoryRepository = viewHistoryRepository;
        this.userStatsService = userStatsService;
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getUserStats(Authentication auth) {
        // TODO: 从认证信息中获取用户ID
        Long userId = 1L; // 临时写死
        return ResponseEntity.ok(userStatsService.getUserStats(userId));
    }
    
    @GetMapping("/view-history")
    public ResponseEntity<List<ViewHistory>> getViewHistory() {
        // TODO: 从认证信息中获取用户ID
        Long userId = 1L; // 临时写死
        return ResponseEntity.ok(viewHistoryRepository.findByUserIdOrderByViewTimeDesc(userId));
    }

    @GetMapping("/stats/likes")
    public ResponseEntity<List<Post>> getLikedPosts(@RequestParam Long userId) {
        return ResponseEntity.ok(userStatsService.getLikedPosts(userId));
    }
}