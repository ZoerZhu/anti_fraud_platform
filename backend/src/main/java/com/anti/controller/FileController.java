package com.anti.controller;

import com.anti.common.Result;
import com.anti.security.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    @Value("${upload.path:uploads}")
    private String uploadPath;

    @Value("${upload.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 上传图片
     */
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return Result.unauthorized("请先登录");
        }
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error(400, "图片大小不能超过10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return Result.error(400, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        try {
            String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path uploadDir = baseDir.resolve("images").resolve(datePath).normalize();
            if (!uploadDir.startsWith(baseDir)) {
                return Result.error(400, "上传路径非法");
            }
            Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();
            String suffix = resolveSuffix(originalFilename, contentType);
            if (!ALLOWED_EXTENSIONS.contains(suffix)) {
                return Result.error(400, "图片扩展名不被允许");
            }
            String filename = UUID.randomUUID().toString() + suffix;

            Path filePath = uploadDir.resolve(filename).normalize();
            if (!filePath.startsWith(uploadDir)) {
                return Result.error(400, "文件名非法");
            }
            Files.write(filePath, file.getBytes());

            String imageUrl = baseUrl + "/uploads/images/" + datePath + "/" + filename;
            return Result.success(imageUrl);
        } catch (IOException e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    private String resolveSuffix(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
