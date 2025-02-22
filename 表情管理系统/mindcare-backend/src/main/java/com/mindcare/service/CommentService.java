package com.mindcare.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindcare.entity.Comment;
import com.mindcare.entity.Post;
import com.mindcare.entity.User;
import com.mindcare.repository.CommentRepository;
import com.mindcare.repository.PostRepository;
import com.mindcare.repository.UserRepository;
import com.mindcare.util.SecurityUtils;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }
    
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreateTimeDesc(postId);
    }
    
    @Transactional
    public Comment createComment(Long postId, String content) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
            
        User currentUser = userRepository.findById(SecurityUtils.getCurrentUserId())
            .orElseThrow(() -> new RuntimeException("用户不存在"));
            
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPost(post);
        comment.setUser(currentUser);
        comment.setUserId(currentUser.getId());
        comment.setPostId(postId);
        comment.setCreateTime(LocalDateTime.now());
        
        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        
        return commentRepository.save(comment);
    }
    
    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("评论不存在"));
            
        // 验证评论是否属于指定的帖子
        if (!comment.getPostId().equals(postId)) {
            throw new RuntimeException("评论不属于该帖子");
        }
        
        // 验证是否有权限删除（管理员或评论作者）
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        if (!isAdmin && !comment.getUserId().equals(currentUserId)) {
            throw new RuntimeException("没有权限删除该评论");
        }
        
        // 更新帖子评论数
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setCommentCount(post.getCommentCount() - 1);
        postRepository.save(post);
        
        // 删除评论
        commentRepository.delete(comment);
    }
    
    // 添加获取用户评论的方法
    public List<Comment> getCommentsByUserId(Long userId) {
        return commentRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }
} 