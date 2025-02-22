package com.mindcare.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mindcare.entity.Post;
import com.mindcare.entity.ViewHistory;
import com.mindcare.repository.CollectionRepository;
import com.mindcare.repository.CommentRepository;
import com.mindcare.repository.LikeRepository;
import com.mindcare.repository.PostRepository;
import com.mindcare.repository.ViewHistoryRepository;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final ViewHistoryRepository viewHistoryRepository;
    private final LikeRepository likeRepository;
    private final CollectionRepository collectionRepository;
    private final CommentRepository commentRepository;
    private final OssService ossService;
    private static final Logger log = LoggerFactory.getLogger(PostService.class);
    
    public PostService(
        PostRepository postRepository,
        ViewHistoryRepository viewHistoryRepository,
        LikeRepository likeRepository,
        CollectionRepository collectionRepository,
        CommentRepository commentRepository,
        OssService ossService
    ) {
        this.postRepository = postRepository;
        this.viewHistoryRepository = viewHistoryRepository;
        this.likeRepository = likeRepository;
        this.collectionRepository = collectionRepository;
        this.commentRepository = commentRepository;
        this.ossService = ossService;
    }
    
    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findAllByOrderByCreateTimeDesc(pageable);
    }
    
    public Page<Post> getPostsByUserId(Long userId, Pageable pageable) {
        return postRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
    }
    
    @Transactional
    public Post createPost(String title, String content, MultipartFile[] images, Long userId) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setUserId(userId);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        post.setStatus("pending"); // 确保新帖子状态为待审核
        
        if (images != null && images.length > 0) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                try {
                    String fileName = "posts/" + UUID.randomUUID().toString() + 
                        getFileExtension(image.getOriginalFilename());
                    String imageUrl = ossService.uploadFile(fileName, image.getInputStream());
                    imageUrls.add(imageUrl);
                } catch (IOException e) {
                    throw new RuntimeException("图片上传失败", e);
                }
            }
            post.setImageUrls(imageUrls);
        }
        
        return postRepository.save(post);
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }
    
    @Transactional
    public void viewPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        // 记录浏览历史
        ViewHistory viewHistory = new ViewHistory();
        viewHistory.setPostId(postId);
        viewHistory.setUserId(userId);
        viewHistory.setViewTime(LocalDateTime.now());
        viewHistoryRepository.save(viewHistory);
        
        // 更新浏览次数
        post.setViewCount(post.getViewCount() + 1);
        postRepository.save(post);
    }
    
    public Map<String, Long> getUserStats(Long userId) {
        long postCount = postRepository.countByUserId(userId);
        Map<String, Long> stats = new HashMap<>();
        stats.put("postCount", postCount);
        return stats;
    }
    
    public Post getPost(Long postId) {
        return postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
    }
    
    public List<Post> getUserPosts(Long userId) {
        return postRepository.findByUserIdOrderByCreateTimeDesc(userId, Pageable.unpaged()).getContent();
    }
    
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此帖子");
        }
        
        try {
            // 删除相关的点赞记录
            likeRepository.deleteByPostId(postId);
            
            // 删除相关的收藏记录
            collectionRepository.deleteByPostId(postId);
            
            // 删除相关的评论
            commentRepository.deleteByPostId(postId);
            
            // 删除相关的浏览记录
            viewHistoryRepository.deleteByPostId(postId);
            
            // 删除帖子相关的图片
            if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                for (String imageUrl : post.getImageUrls()) {
                    try {
                        // 从 URL 中提取文件名
                        String fileName = "posts/" + imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                        ossService.deleteFile(fileName);
                    } catch (Exception e) {
                        // 记录错误但继续执行
                        log.error("删除帖子图片失败: " + imageUrl, e);
                    }
                }
            }
            
            // 最后删除帖子
            postRepository.delete(post);
        } catch (Exception e) {
            log.error("删除帖子失败: ", e);
            throw new RuntimeException("删除帖子失败: " + e.getMessage());
        }
    }
} 