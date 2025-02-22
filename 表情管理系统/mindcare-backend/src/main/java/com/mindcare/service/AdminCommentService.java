package com.mindcare.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.mindcare.entity.Comment;
import com.mindcare.repository.CommentRepository;

@Service
public class AdminCommentService {
    
    private final CommentRepository commentRepository;
    
    public AdminCommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    
    @Transactional(readOnly = true)
    public Page<Comment> getComments(Pageable pageable, String search) {
        if (StringUtils.hasText(search)) {
            return commentRepository.findByContentContaining(search, pageable);
        }
        return commentRepository.findAllByOrderByCreateTimeDesc(pageable);
    }
    
    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
} 