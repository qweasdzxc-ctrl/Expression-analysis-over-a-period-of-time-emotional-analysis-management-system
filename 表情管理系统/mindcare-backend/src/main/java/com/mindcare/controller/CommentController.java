package com.mindcare.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.dto.CommentRequest;
import com.mindcare.dto.ErrorResponse;
import com.mindcare.entity.Comment;
import com.mindcare.service.CommentService;
import com.mindcare.util.SecurityUtils;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@CrossOrigin(origins = "http://localhost:5173")
public class CommentController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    private final CommentService commentService;
    
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }
    
    @GetMapping
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable Long postId,
            @RequestParam(required = false) Long userId) {
        try {
            if (userId != null) {
                return ResponseEntity.ok(commentService.getCommentsByUserId(userId));
            }
            return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        try {
            Comment comment = commentService.createComment(postId, request.getContent());
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        try {
            // 检查认证状态并记录日志
            logger.debug("删除评论请求 - postId: {}, commentId: {}", postId, commentId);
            if (!SecurityUtils.isAuthenticated()) {
                logger.warn("未认证的删除评论请求");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("请先登录"));
            }
            
            commentService.deleteComment(postId, commentId);
            logger.debug("评论删除成功");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("删除评论失败", e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("删除评论失败: " + e.getMessage()));
        }
    }
} 