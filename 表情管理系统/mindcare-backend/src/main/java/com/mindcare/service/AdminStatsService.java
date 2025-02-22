package com.mindcare.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindcare.repository.PostRepository;
import com.mindcare.repository.UserRepository;

@Service
public class AdminStatsService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    public AdminStatsService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }
    
    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        
        // 获取总用户数
        stats.put("userCount", userRepository.count());
        
        // 获取今日活跃用户数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        stats.put("activeUserCount", userRepository.countByLastLoginTimeAfter(todayStart));
        
        // 获取总帖子数
        stats.put("postCount", postRepository.count());
        
        // 获取今日新增帖子数
        stats.put("todayPostCount", postRepository.countByCreateTimeAfter(todayStart));
        
        return stats;
    }
} 