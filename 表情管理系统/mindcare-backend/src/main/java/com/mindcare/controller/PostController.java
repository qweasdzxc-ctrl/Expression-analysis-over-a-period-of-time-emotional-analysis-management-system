package com.mindcare.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mindcare.entity.Post;
import com.mindcare.service.CollectionService;
import com.mindcare.service.LikeService;
import com.mindcare.service.PostService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PostController {
    private final PostService postService;
    private final LikeService likeService;
    private final CollectionService collectionService;
    private final HttpServletRequest request;
    
    public PostController(PostService postService, 
                         LikeService likeService,
                         CollectionService collectionService,
                         HttpServletRequest request) {
        this.postService = postService;
        this.likeService = likeService;
        this.collectionService = collectionService;
        this.request = request;
    }
    
    @GetMapping("/posts")
    public ResponseEntity<Page<Post>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return ResponseEntity.ok(postService.getPosts(pageable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/posts")
    public ResponseEntity<?> createPost(
        @RequestParam("title") String title,
        @RequestParam("content") String content,
        @RequestParam(value = "images", required = false) MultipartFile[] images
    ) {
        try {
            Long userId = getCurrentUserId();
            Post post = postService.createPost(title, content, images, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", post);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/posts/{postId}/view")
    public ResponseEntity<?> viewPost(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            postService.viewPost(postId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/posts/{postId}/like/status")
    public ResponseEntity<Map<String, Boolean>> getLikeStatus(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            Map<String, Boolean> status = new HashMap<>();
            status.put("isLiked", likeService.isPostLikedByUser(postId, userId));
            status.put("isCollected", collectionService.isPostCollectedByUser(postId, userId));
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        try {
            boolean isLiked = likeService.isPostLikedByUser(postId, userId);
            likeService.likePost(postId, userId);
            
            Post post = postService.getPost(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("post", post);
            response.put("isLiked", !isLiked);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("error", e.getMessage());
                put("post", postService.getPost(postId));
                put("isLiked", likeService.isPostLikedByUser(postId, userId));
            }});
        }
    }
    
    @PostMapping("/posts/{postId}/collect")
    public ResponseEntity<?> collectPost(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        try {
            boolean isCollected = collectionService.isPostCollectedByUser(postId, userId);
            collectionService.collectPost(postId, userId);
            
            Post post = postService.getPost(postId);
            Map<String, Object> response = new HashMap<>();
            response.put("post", post);
            response.put("isCollected", !isCollected);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("error", e.getMessage());
                put("post", postService.getPost(postId));
                put("isCollected", collectionService.isPostCollectedByUser(postId, userId));
            }});
        }
    }
    
    @GetMapping("/posts/{postId}")
    public ResponseEntity<Post> getPost(@PathVariable Long postId) {
        try {
            Post post = postService.getPost(postId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<Post>> getUserPosts(@PathVariable Long userId) {
        try {
            List<Post> posts = postService.getUserPosts(userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        try {
            Long userId = getCurrentUserId();
            postService.deletePost(postId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
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