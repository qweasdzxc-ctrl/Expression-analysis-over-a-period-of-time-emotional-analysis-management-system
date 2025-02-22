package com.mindcare.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mindcare.entity.Post;
import com.mindcare.repository.PostRepository;

@Service
public class AdminPostService {
    
    private final PostRepository postRepository;
    private final PostService postService;
    
    public AdminPostService(PostRepository postRepository, PostService postService) {
        this.postRepository = postRepository;
        this.postService = postService;
    }
    
    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable, String search) {
        if (StringUtils.hasText(search)) {
            return postRepository.findByTitleContainingOrContentContainingForAdmin(search, pageable);
        }
        return postRepository.findAllForAdmin(pageable);
    }
    
    @Transactional
    public void deletePost(Long postId) {
        postService.deletePost(postId, null); // 管理员可以删除任何帖子
    }
    
    @Transactional
    public void updatePostStatus(Long postId, String status) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        post.setStatus(status);
        postRepository.save(post);
    }
} 