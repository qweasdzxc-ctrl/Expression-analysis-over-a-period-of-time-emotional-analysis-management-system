package com.mindcare.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Post;
import com.mindcare.service.AdminPostService;

@RestController
@RequestMapping("/api/admin/posts")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminPostController {
    
    private final AdminPostService adminPostService;
    
    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }
    
    @GetMapping
    public ResponseEntity<Page<Post>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<Post> posts = adminPostService.getPosts(pageRequest, search);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@PathVariable Long postId) {
        try {
            adminPostService.deletePost(postId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/{postId}/{action}")
    public ResponseEntity<?> handlePostStatus(
            @PathVariable Long postId,
            @PathVariable String action) {
        try {
            String status = "pending";
            switch (action) {
                case "enable":
                    status = "normal";
                    break;
                case "disable":
                    status = "disabled";
                    break;
                default:
                    return ResponseEntity.badRequest().body("Invalid action");
            }
            
            adminPostService.updatePostStatus(postId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
} 