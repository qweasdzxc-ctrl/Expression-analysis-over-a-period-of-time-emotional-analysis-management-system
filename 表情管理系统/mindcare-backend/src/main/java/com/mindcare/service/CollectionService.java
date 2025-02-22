package com.mindcare.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindcare.entity.Collection;
import com.mindcare.entity.Post;
import com.mindcare.repository.CollectionRepository;
import com.mindcare.repository.PostRepository;
import com.mindcare.util.SecurityUtils;

@Service
public class CollectionService {
    private final CollectionRepository collectionRepository;
    private final PostRepository postRepository;
    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);
    
    public CollectionService(CollectionRepository collectionRepository, PostRepository postRepository) {
        this.collectionRepository = collectionRepository;
        this.postRepository = postRepository;
    }
    
    public boolean isPostCollectedByUser(Long postId, Long userId) {
        return collectionRepository.existsByUserIdAndPostId(userId, postId);
    }
    
    @Transactional
    public void collectPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        boolean isCollected = isPostCollectedByUser(postId, userId);
        
        if (isCollected) {
            // 如果已经收藏，则取消收藏
            collectionRepository.deleteByUserIdAndPostId(userId, postId);
            post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
        } else {
            // 如果未收藏，则添加收藏
            Collection collection = new Collection();
            collection.setUserId(userId);
            collection.setPostId(postId);
            collection.setCreateTime(LocalDateTime.now());
            collectionRepository.save(collection);
            post.setCollectCount(post.getCollectCount() + 1);
        }
        
        postRepository.save(post);
        postRepository.flush();
    }
    
    public List<Collection> getUserCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
    
    public Map<String, Long> getUserStats(Long userId) {
        long collectCount = collectionRepository.countByUserId(userId);
        Map<String, Long> stats = new HashMap<>();
        stats.put("collectCount", collectCount);
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Post> getCollectedPosts(Long userId) {
        try {
            if (userId == null) {
                log.warn("尝试获取收藏帖子时 userId 为空");
                return new ArrayList<>();
            }

            log.debug("开始获取用户 {} 的收藏帖子", userId);
            
            List<Collection> collections = collectionRepository.findByUserIdOrderByCreateTimeDesc(userId);
            log.debug("找到 {} 条收藏记录", collections.size());
            
            if (collections.isEmpty()) {
                log.debug("用户 {} 没有收藏任何帖子", userId);
                return new ArrayList<>();
            }

            List<Long> postIds = collections.stream()
                .map(Collection::getPostId)
                .collect(Collectors.toList());
            
            log.debug("找到用户 {} 收藏的帖子ID: {}", userId, postIds);
            
            List<Post> posts = postRepository.findAllByIdWithUser(postIds);
            log.debug("成功获取到 {} 个收藏帖子", posts.size());
            
            return posts;
        } catch (Exception e) {
            log.error("获取收藏帖子失败: userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public boolean hasUserCollectedPost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return collectionRepository.existsByUserIdAndPostId(userId, postId);
    }
} 