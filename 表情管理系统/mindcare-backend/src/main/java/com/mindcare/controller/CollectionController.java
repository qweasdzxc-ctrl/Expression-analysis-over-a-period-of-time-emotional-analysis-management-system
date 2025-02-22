package com.mindcare.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mindcare.entity.Post;
import com.mindcare.service.CollectionService;

@RestController
@RequestMapping("/api/collections")
@CrossOrigin(origins = "http://localhost:5173")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/user")
    public ResponseEntity<List<Post>> getUserCollections(@RequestParam Long userId) {
        try {
            if (userId == null) {
                log.warn("获取收藏帖子时 userId 参数为空");
                return ResponseEntity.badRequest().build();
            }

            log.debug("开始获取用户 {} 的收藏帖子", userId);
            List<Post> collections = collectionService.getCollectedPosts(userId);
            log.debug("成功获取用户 {} 的收藏帖子，共 {} 条", userId, collections.size());
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            log.error("获取用户收藏帖子失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>()); // 返回空列表而不是 null
        }
    }
} 