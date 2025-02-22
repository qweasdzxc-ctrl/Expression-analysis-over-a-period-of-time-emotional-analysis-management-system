package com.mindcare.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mindcare.entity.Post;
import com.mindcare.repository.CollectionRepository;
import com.mindcare.repository.CommentRepository;
import com.mindcare.repository.LikeRepository;
import com.mindcare.repository.PostRepository;

@Service
public class UserStatsService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;
    
    public UserStatsService(
        PostRepository postRepository,
        CommentRepository commentRepository,
        LikeRepository likeRepository,
        CollectionRepository collectionRepository
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.collectionRepository = collectionRepository;
    }
    
    public Map<String, Long> getUserStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        
        // 获取用户发帖数
        stats.put("postCount", postRepository.countByUserId(userId));
        
        // 获取用户评论数
        stats.put("commentCount", commentRepository.countByUserId(userId));
        
        // 获取用户获赞数（包括帖子和评论的点赞）
        stats.put("receivedLikes", likeRepository.countReceivedLikes(userId));
        
        // 获取用户收藏数
        stats.put("collectCount", collectionRepository.countByUserId(userId));
        
        return stats;
    }

    // 添加获取用户点赞的帖子方法
    public List<Post> getLikedPosts(Long userId) {
        try {
            // 获取用户点赞的所有帖子ID
            List<Long> likedPostIds = likeRepository.findByUserId(userId).stream()
                .map(like -> like.getPostId())
                .filter(postId -> postId != null)
                .collect(Collectors.toList());
            
            // 如果没有点赞的帖子，返回空列表
            if (likedPostIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 根据帖子ID列表获取帖子详情
            return postRepository.findAllById(likedPostIds);
        } catch (Exception e) {
            throw new RuntimeException("获取点赞帖子失败", e);
        }
    }

    // 添加获取用户收藏的帖子方法
    public List<Post> getCollectedPosts(Long userId) {
        try {
            // 获取用户收藏的所有帖子ID
            List<Long> collectedPostIds = collectionRepository.findByUserIdOrderByCreateTimeDesc(userId).stream()
                .map(collection -> collection.getPostId())
                .collect(Collectors.toList());
            
            // 如果没有收藏的帖子，返回空列表
            if (collectedPostIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 根据帖子ID列表获取帖子详情
            return postRepository.findAllById(collectedPostIds);
        } catch (Exception e) {
            throw new RuntimeException("获取收藏帖子失败", e);
        }
    }
} 