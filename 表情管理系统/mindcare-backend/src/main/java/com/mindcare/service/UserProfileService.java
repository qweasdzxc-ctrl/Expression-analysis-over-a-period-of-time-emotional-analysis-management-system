package com.mindcare.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mindcare.entity.Post;
import com.mindcare.entity.User;
import com.mindcare.repository.CollectionRepository;
import com.mindcare.repository.CommentRepository;
import com.mindcare.repository.LikeRepository;
import com.mindcare.repository.PostRepository;
import com.mindcare.repository.UserRepository;

@Service
public class UserProfileService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;
    private final String uploadDir = "uploads/avatars/";
    private final OssService ossService;
    
    public UserProfileService(
        UserRepository userRepository,
        PostRepository postRepository,
        CommentRepository commentRepository,
        LikeRepository likeRepository,
        CollectionRepository collectionRepository,
        OssService ossService
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.collectionRepository = collectionRepository;
        this.ossService = ossService;
        
        // 确保上传目录存在
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录", e);
        }
    }
    
    public User getProfile(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }
    
    @Transactional
    public User updateProfile(Long userId, User updatedUser) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
            
        user.setUsername(updatedUser.getUsername());
        user.setBio(updatedUser.getBio());
        
        return userRepository.save(user);
    }
    
    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 生成文件名
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = "avatar/" + UUID.randomUUID().toString() + extension;

            // 上传到阿里云OSS
            String avatarUrl = ossService.uploadFile(fileName, file.getInputStream());

            // 更新用户头像URL
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            return avatarUrl;
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败", e);
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
    
    public List<Post> getUserPosts(Long userId) {
        // 创建一个分页请求，获取所有帖子（设置一个较大的页面大小）
        Pageable pageable = PageRequest.of(0, 1000);
        return postRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable).getContent();
    }

    public Map<String, Long> getUserStats(Long userId) {
        // 检查用户是否存在
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("用户不存在");
        }

        Map<String, Long> stats = new HashMap<>();
        
        // 获取发帖数
        stats.put("postCount", postRepository.countByUserId(userId));
        
        // 获取评论数
        stats.put("commentCount", commentRepository.countByUserId(userId));
        
        // 获取点赞数（包括发出的点赞和收到的点赞）
        stats.put("likeCount", likeRepository.countByUserId(userId));
        stats.put("receivedLikes", likeRepository.countReceivedLikes(userId));
        
        // 获取收藏数
        stats.put("collectCount", collectionRepository.countByUserId(userId));
        
        return stats;
    }
} 