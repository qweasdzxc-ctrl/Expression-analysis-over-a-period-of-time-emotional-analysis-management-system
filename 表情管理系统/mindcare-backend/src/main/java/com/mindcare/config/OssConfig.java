package com.mindcare.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.SetBucketCORSRequest;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;

@Configuration
public class OssConfig {
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;
    
    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Bean
    public OSS ossClient() {
        // 创建OSSClient实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        
        // 设置存储空间的跨域资源共享规则
        setBucketCors(ossClient);
        
        return ossClient;
    }
    
    private void setBucketCors(OSS ossClient) {
        SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
        
        // 创建跨域规则
        CORSRule corsRule = new CORSRule();
        // 允许的来源
        corsRule.setAllowedOrigins(Arrays.asList("*"));
        // 允许的请求方法
        corsRule.setAllowedMethods(Arrays.asList("GET", "HEAD", "PUT", "POST", "DELETE"));
        // 允许的头部
        corsRule.setAllowedHeaders(Arrays.asList("*"));
        // 暴露的头部
        corsRule.setExposeHeaders(Arrays.asList("Content-Length", "Content-Type", "ETag", 
            "x-oss-request-id", "x-oss-version-id"));
        // 缓存时间
        corsRule.setMaxAgeSeconds(300);
        
        // 添加规则
        request.setCorsRules(Arrays.asList(corsRule));
        
        // 应用规则
        ossClient.setBucketCORS(request);
    }
} 