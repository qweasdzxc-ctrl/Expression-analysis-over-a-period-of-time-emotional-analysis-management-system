package com.mindcare.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.entity.FaceCapture;

@Service
@Component
public class CozeService {
    
    private static final Logger log = LoggerFactory.getLogger(CozeService.class);
    
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;
    
    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;
    
    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;
    
    @Value("${aliyun.oss.bucketName}")
    private String bucketName;
    
    @Value("${local.image.path}")
    private String localImagePath;  // 在application.properties中配置本地保存路径
    
    private OSS ossClient;
    private final RestTemplate restTemplate;
    
    public CozeService() {
        this.restTemplate = new RestTemplate();
    }
    
    @PostConstruct
    public void init() {
        try {
            log.info("Initializing OSS client with endpoint: {}, bucket: {}", endpoint, bucketName);
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // 验证 bucket 是否存在
            if (!ossClient.doesBucketExist(bucketName)) {
                log.error("Bucket {} does not exist", bucketName);
                throw new RuntimeException("OSS bucket does not exist: " + bucketName);
            }
            log.info("OSS client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize OSS client: ", e);
            throw new RuntimeException("Failed to initialize OSS client: " + e.getMessage());
        }
    }
    
    public String analyzeEmotions(List<FaceCapture> captures, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            log.info("Starting batch analysis of {} captures", captures.size());
            
            ObjectMapper mapper = new ObjectMapper();
            
            // 合并图片
            BufferedImage combinedImage = combineImages(captures);
            
            // 生成文件名
            String fileName = String.format("emotions/%s.jpg", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            
            // 将合并后的图片上传到 OSS
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(combinedImage, "jpg", bos);
            byte[] imageBytes = bos.toByteArray();
            
            try {
                // 在上传文件时设置文件的元数据
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType("image/jpeg");
                metadata.setCacheControl("no-cache");
                
                // 上传到OSS时包含元数据
                ossClient.putObject(new PutObjectRequest(bucketName, fileName, 
                    new ByteArrayInputStream(imageBytes), metadata));
                
                // 生成公开访问的 URL，只添加图片处理参数
                String imageUrl = String.format("https://%s.%s/%s?x-oss-process=image/resize,w_800", 
                    bucketName, endpoint, fileName);
                log.info("Generated OSS URL: {}", imageUrl);
                
                // 构建返回对象
                Map<String, Object> result = new HashMap<>();
                result.put("imageUrl", imageUrl);
                result.put("imageCount", captures.size());
                result.put("startTime", startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                result.put("endTime", endTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                
                // 构建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("workflow_id", "7468668994539929627");
                
                // 构建参数对象，使用处理后的图片URL
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("input", Collections.singletonList(imageUrl));
                requestBody.put("parameters", parameters);
                
                log.info("Calling Coze API with URL: {}", imageUrl);
                String response = callCozeApi(requestBody);
                JsonNode root = mapper.readTree(response);
                
                if (root.has("data")) {
                    JsonNode data = mapper.readTree(root.get("data").asText());
                    result.put("analysis", data.get("output").asText());
                } else {
                    result.put("analysis", "无法分析表情");
                }
                
                return mapper.writeValueAsString(result);
                
            } catch (Exception e) {
                log.error("Failed to upload image to OSS: ", e);
                throw new RuntimeException("Failed to upload image: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error analyzing emotions: ", e);
            throw new RuntimeException("Failed to analyze emotions: " + e.getMessage());
        }
    }
    
    private BufferedImage combineImages(List<FaceCapture> captures) {
        try {
            if (captures.isEmpty()) {
                throw new IllegalArgumentException("No captures to analyze");
            }

            // 计算布局
            int imageCount = captures.size();
            int cols = (int) Math.ceil(Math.sqrt(imageCount)); // 列数
            int rows = (int) Math.ceil(imageCount / (double) cols); // 行数
            
            // 获取第一张图片的尺寸作为标准
            String firstImageData = captures.get(0).getImageData();
            if (firstImageData.startsWith("data:")) {
                firstImageData = firstImageData.substring(firstImageData.indexOf(",") + 1);
            }
            byte[] firstImageBytes = Base64.getDecoder().decode(firstImageData);
            BufferedImage firstImage = ImageIO.read(new ByteArrayInputStream(firstImageBytes));
            
            int standardWidth = firstImage.getWidth();
            int standardHeight = firstImage.getHeight();
            
            // 创建新的合并图片
            int padding = 10; // 图片间的间距
            int totalWidth = cols * (standardWidth + padding) + padding;
            int totalHeight = rows * (standardHeight + padding) + padding;
            
            BufferedImage combined = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combined.createGraphics();
            
            // 设置白色背景
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, totalWidth, totalHeight);
            
            // 设置文字属性
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            
            // 绘制每张图片
            int index = 0;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    if (index >= captures.size()) {
                        break;
                    }
                    
                    FaceCapture capture = captures.get(index);
                    String imageData = capture.getImageData();
                    if (imageData != null && !imageData.isEmpty()) {
                        // 处理图片数据
                        if (imageData.startsWith("data:")) {
                            imageData = imageData.substring(imageData.indexOf(",") + 1);
                        }
                        
                        // 解码并绘制图片
                        byte[] imageBytes = Base64.getDecoder().decode(imageData);
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
                        
                        // 计算位置
                        int x = padding + col * (standardWidth + padding);
                        int y = padding + row * (standardHeight + padding);
                        
                        // 绘制图片
                        g2d.drawImage(image.getScaledInstance(standardWidth, standardHeight, Image.SCALE_SMOOTH), 
                            x, y, null);
                        
                        // 绘制时间戳
                        String timestamp = capture.getCaptureTime().format(timeFormatter);
                        g2d.drawString(timestamp, x + 5, y + standardHeight - 5);
                    }
                    index++;
                }
            }
            
            g2d.dispose();
            
            // 压缩图片质量以减小大小
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            
            // 设置更高的压缩率
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.1f);  // 降低图片质量到 10%
            
            // 缩小图片尺寸
            int maxWidth = 800;
            int maxHeight = 600;
            
            double scale = Math.min(
                (double) maxWidth / combined.getWidth(),
                (double) maxHeight / combined.getHeight()
            );
            
            if (scale < 1.0) {
                int newWidth = (int) (combined.getWidth() * scale);
                int newHeight = (int) (combined.getHeight() * scale);
                
                BufferedImage resized = new BufferedImage(newWidth, newHeight, combined.getType());
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(combined, 0, 0, newWidth, newHeight, null);
                g.dispose();
                
                combined = resized;
            }
            
            ImageOutputStream ios = ImageIO.createImageOutputStream(bos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(combined, null, null), param);
            writer.dispose();
            ios.close();
            
            String base64Combined = Base64.getEncoder().encodeToString(bos.toByteArray());
            String dataUrl = "data:image/jpeg;base64," + base64Combined;
            
            // 打印链接长度信息
            log.info("============= 合并图片信息 =============");
            log.info("图片数量: {}", captures.size());
            log.info("原始尺寸: {}x{}", totalWidth, totalHeight);
            log.info("压缩后尺寸: {}x{}", combined.getWidth(), combined.getHeight());
            log.info("链接长度: {} 字符", dataUrl.length());
            log.info("压缩率: {:.2f}%", (bos.size() * 100.0) / (totalWidth * totalHeight * 3));
            log.info("=======================================");
            
            return combined;
            
        } catch (Exception e) {
            log.error("Failed to combine images: ", e);
            throw new RuntimeException("Failed to combine images: " + e.getMessage());
        }
    }
    
    private String callCozeApi(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer pat_guXlGejsAnj9GaDAgpmfyBW5E31rnadVZYJ1Asie03dPzwY9T3GKOoZCqqvVHKnf");
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.coze.cn/v1/workflow/run",
                HttpMethod.POST,
                request,
                String.class
            );
            
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("API call failed: ", e);
            throw new RuntimeException("Failed to call Coze API: " + e.getMessage());
        }
    }

    private String saveImageLocally(byte[] imageBytes, String fileName) throws IOException {
        // 确保目录存在
        Path directory = Paths.get(localImagePath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        // 从完整的文件名中提取时间戳部分
        String localFileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        Path filePath = directory.resolve(localFileName);
        
        // 保存文件
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageBytes);
        }
        
        // 返回本地文件路径
        return "@" + filePath.toAbsolutePath().toString();
    }

    public Map<String, Object> uploadImage(byte[] imageBytes) {
        try {
            // 生成文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = timestamp + ".jpg";

            // 设置元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setCacheControl("no-cache");

            // 上传到OSS
            ossClient.putObject(new PutObjectRequest(bucketName, "emotions/" + fileName, 
                new ByteArrayInputStream(imageBytes), metadata));

            // 创建工作流需要的数据格式
            Map<String, Object> workflowData = new HashMap<>();
            workflowData.put("type", "image");
            workflowData.put("data", Base64.getEncoder().encodeToString(imageBytes));
            workflowData.put("name", fileName);

            // 打印工作流数据到控制台
            log.info("Workflow data: {}", workflowData);
            System.out.println("发送到工作流的数据：");
            System.out.println("类型: " + workflowData.get("type"));
            System.out.println("文件名: " + workflowData.get("name"));
            System.out.println("数据长度: " + ((String)workflowData.get("data")).length() + " 字符");
            // 打印 Base64 数据的前100个字符作为预览
            System.out.println("数据预览: " + ((String)workflowData.get("data")).substring(0, 100) + "...");

            return workflowData;

        } catch (Exception e) {
            log.error("Failed to upload image", e);
            throw new RuntimeException("上传图片失败", e);
        }
    }
}