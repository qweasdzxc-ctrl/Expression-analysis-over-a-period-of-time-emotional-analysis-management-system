package com.mindcare.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class OnlineUserService {
    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();
    
    public void userOnline(Long userId) {
        onlineUsers.add(userId);
    }
    
    public void userOffline(Long userId) {
        onlineUsers.remove(userId);
    }
    
    public int getOnlineCount() {
        return onlineUsers.size();
    }
    
    public Set<Long> getOnlineUsers() {
        return onlineUsers;
    }
} 