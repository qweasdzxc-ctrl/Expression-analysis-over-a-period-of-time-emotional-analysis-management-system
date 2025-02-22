package com.mindcare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;

@Configuration
@Validated
public class CozeConfig {
    
    @Value("${coze.api.key}")
    private String apiKey;
    
    @Value("${coze.api.url}")
    private String apiUrl;
    
    @Value("${coze.api.workflow_id}")
    private String workflowId;
    
    @PostConstruct
    public void validateConfig() {
        if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalStateException("Coze API key is not configured");
        }
        if (StringUtils.isEmpty(apiUrl)) {
            throw new IllegalStateException("Coze API URL is not configured");
        }
        if (StringUtils.isEmpty(workflowId)) {
            throw new IllegalStateException("Coze workflow ID is not configured");
        }
    }
    
    // Getters
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
} 