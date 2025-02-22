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

import com.mindcare.entity.Like;
import com.mindcare.entity.Post;
import com.mindcare.repository.LikeRepository;
import com.mindcare.repository.PostRepository;
import com.mindcare.repository.UserRepository;
import com.mindcare.util.SecurityUtils;

@Service
public class LikeService {
    private static final Logger log = LoggerFactory.getLogger(LikeService.class);
    
    private final LikeRepository likeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public LikeService(LikeRepository likeRepository, 
                      PostRepository postRepository,
                      UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
    
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }
    
    @Transactional
    public void likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        boolean isLiked = isPostLikedByUser(postId, userId);
        
        if (isLiked) {
            // 如果已经点赞，则取消点赞
            likeRepository.deleteByUserIdAndPostId(userId, postId);
            // 更新帖子的点赞总数
            Long totalLikes = likeRepository.countByPostId(postId);
            post.setLikeCount(totalLikes.intValue());
        } else {
            // 如果未点赞，则添加点赞
            Like like = new Like();
            like.setUserId(userId);
            like.setPostId(postId);
            like.setCreateTime(LocalDateTime.now());
            likeRepository.save(like);
            // 更新帖子的点赞总数
            Long totalLikes = likeRepository.countByPostId(postId);
            post.setLikeCount(totalLikes.intValue());
        }
        
        postRepository.save(post);
        postRepository.flush();
    }
    
    public Map<String, Long> getUserStats(Long userId) {
        Map<String, Long> stats = new HashMap<>();
        stats.put("likeCount", likeRepository.countByUserId(userId));
        stats.put("receivedLikes", likeRepository.countReceivedLikes(userId));
        return stats;
    }
    
    public List<Map<String, Object>> getReceivedLikes(Long userId) {
        try {
            // 获取用户发布的所有帖子
            List<Post> userPosts = postRepository.findByUserId(userId);
            
            // 收集所有点赞信息
            List<Map<String, Object>> receivedLikes = new ArrayList<>();
            
            for (Post post : userPosts) {
                List<Like> postLikes = likeRepository.findByPostIdOrderByCreateTimeDesc(post.getId());
                for (Like like : postLikes) {
                    Map<String, Object> likeInfo = new HashMap<>();
                    likeInfo.put("like", like);
                    likeInfo.put("post", post);
                    likeInfo.put("user", userRepository.findById(like.getUserId()).orElse(null));
                    receivedLikes.add(likeInfo);
                }
            }
            
            return receivedLikes;
        } catch (Exception e) {
            log.error("获取收到的点赞失败", e);
            return new ArrayList<>();
        }
    }

    public List<Post> getUserLikedPosts(Long userId) {
        List<Like> likes = likeRepository.findByUserIdOrderByCreateTimeDesc(userId);
        List<Long> postIds = likes.stream()
            .map(Like::getPostId)
            .collect(Collectors.toList());
        return postRepository.findAllByIdWithUser(postIds);
    }

    public List<Post> getUserReceivedLikesPosts(Long userId) {
        return postRepository.findByUserIdAndLikeCountGreaterThanOrderByLikeCountDesc(userId, 0L);
    }

    public boolean hasUserLikedPost(Long postId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return likeRepository.existsByUserIdAndPostId(userId, postId);
    }
} 