package com.mindcare.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mindcare.entity.FaceCapture;
import com.mindcare.repository.FaceCaptureRepository;
import com.mindcare.service.CozeService;

@RestController
@RequestMapping("/api/face-captures")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {
    RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS
})
public class FaceCaptureController {
    
    private static final Logger log = LoggerFactory.getLogger(FaceCaptureController.class);
    
    private final FaceCaptureRepository faceCaptureRepository;
    private final CozeService cozeService;
    
    public FaceCaptureController(FaceCaptureRepository faceCaptureRepository, CozeService cozeService) {
        this.faceCaptureRepository = faceCaptureRepository;
        this.cozeService = cozeService;
    }
    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> result = cozeService.uploadImage(file.getBytes());
            
            // 添加额外信息方便前端调试
            result.put("success", true);
            result.put("message", "图片上传成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "图片上传失败: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
        }
    }
    
    @PostMapping
    public ResponseEntity<?> saveFaceCapture(@RequestBody Map<String, String> payload) {
        try {
            String imageData = payload.get("image");
            String timestamp = payload.get("timestamp");
            
            if (imageData == null || timestamp == null) {
                log.error("Missing required fields in payload");
                return ResponseEntity.badRequest().body("Missing required fields");
            }
            
            log.debug("Received capture request for timestamp: {}", timestamp);
            
            // 使用 DateTimeFormatter 处理 ISO 格式的时间戳
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
            
            // 先保存图片到数据库
            FaceCapture faceCapture = new FaceCapture();
            faceCapture.setImageData(imageData);
            faceCapture.setCaptureTime(dateTime);
            
            FaceCapture savedCapture = faceCaptureRepository.save(faceCapture);
            log.debug("Saved capture with ID: {}", savedCapture.getId());
            
            return ResponseEntity.ok("Image captured successfully");
            
        } catch (DateTimeParseException e) {
            log.error("Error parsing timestamp: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid timestamp format");
        } catch (Exception e) {
            log.error("Error processing face capture: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to process face capture");
        }
    }
    
    @PostMapping("/analyze-period")
    public ResponseEntity<?> analyzeTimePeriod(@RequestBody Map<String, String> payload) {
        try {
            // 从请求中获取开始和结束时间
            String startTimeStr = payload.get("startTime");
            String endTimeStr = payload.get("endTime");
            
            if (startTimeStr == null || endTimeStr == null) {
                return ResponseEntity.badRequest().body("开始时间和结束时间不能为空");
            }
            
            // 解析时间字符串
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, formatter);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, formatter);
            
            log.info("Analyzing period from {} to {}", startTime, endTime);
            
            // 获取指定时间段的所有表情捕捉数据
            List<FaceCapture> captures = faceCaptureRepository.findByCaptureTimeBetween(startTime, endTime);
            log.info("Found {} captures in database", captures.size());
            
            if (captures.isEmpty()) {
                log.warn("No captures found in the specified time period");
                return ResponseEntity.ok("没有找到需要分析的表情数据，请先进行表情捕捉");
            }
            
            // 打印每个捕获的详细信息
            captures.forEach(capture -> 
                log.info("Capture: id={}, time={}, hasImage={}, imageSize={}", 
                    capture.getId(), 
                    capture.getCaptureTime(),
                    capture.getImageData() != null,
                    capture.getImageData() != null ? capture.getImageData().length() : 0)
            );
            
            try {
                String analysisResult = cozeService.analyzeEmotions(captures, startTime, endTime);
                log.info("Analysis completed successfully");
                
                // 创建包含工作流数据的响应
                Map<String, Object> response = new HashMap<>();
                response.put("analysis", analysisResult);
                
                // 添加工作流数据
                Map<String, Object> workflowData = new HashMap<>();
                workflowData.put("type", "emotion_analysis");
                workflowData.put("name", "emotion_analysis_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                workflowData.put("data", analysisResult);
                response.put("workflowData", workflowData);
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                log.error("Error during emotion analysis: ", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("分析失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error in analyze-period endpoint: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("分析失败: " + e.getMessage());
        }
    }
} 