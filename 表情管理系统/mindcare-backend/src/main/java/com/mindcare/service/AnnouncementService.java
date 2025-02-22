package com.mindcare.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mindcare.entity.Announcement;
import com.mindcare.repository.AnnouncementRepository;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    
    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }
    
    @Transactional
    public Announcement createAnnouncement(String content) {
        // 将之前的公告设置为非活动
        announcementRepository.findLatestActive()
            .ifPresent(announcement -> {
                announcement.setActive(false);
                announcementRepository.save(announcement);
            });
            
        // 创建新公告
        Announcement announcement = new Announcement();
        announcement.setContent(content);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setActive(true);
        
        return announcementRepository.save(announcement);
    }
    
    public Announcement getLatestAnnouncement() {
        return announcementRepository.findLatestActive()
            .orElse(null);
    }
} 