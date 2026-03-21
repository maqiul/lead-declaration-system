package com.declaration.controller;

import com.declaration.common.Result;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationAttachmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件处理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/files")
@Tag(name = "文件管理")
@RequiredArgsConstructor
public class FileController {

    private final DeclarationAttachmentService attachmentService;
    
    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<DeclarationAttachment> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type) {
        try {
            DeclarationAttachment attachment = attachmentService.uploadFile(file, type);
            return Result.success(attachment);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return Result.fail("上传文件失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    @Operation(summary = "下载或预览文件")
    public void downloadFile(
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false, defaultValue = "false") boolean download,
            HttpServletResponse response) {
        try {
            String targetPath = path;
            
            if (id != null) {
                DeclarationAttachment attachment = attachmentService.getById(id);
                if (attachment != null && attachment.getFileUrl() != null) {
                    // Extract path from URL: /api/v1/files/download?path=filename
                    String url = attachment.getFileUrl();
                    if (url.contains("path=")) {
                        targetPath = url.substring(url.indexOf("path=") + 5);
                    }
                }
            }

            if (targetPath == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            String decodedPath = URLDecoder.decode(targetPath, StandardCharsets.UTF_8.name());
            File file = new File(decodedPath);
            System.out.println("uploadPath: " + uploadPath);
            System.out.println("decodedPath: " + decodedPath);
            
            File uploadsDir = null; // 声明在外部
            
            // 解析文件路径
            if (!file.isAbsolute()) {
                // 使用配置的上传路径
                uploadsDir = new File(uploadPath);
                if (uploadsDir.exists()) {
                    // 先尝试直接在 uploads 下找
                    File directFile = new File(uploadsDir, decodedPath);
                    if (directFile.exists()) {
                        file = directFile;
                    } else {
                        // 递归搜索子目录 (如 exports/, 202603/ 等)
                        File found = searchFileInDir(uploadsDir, decodedPath);
                        if (found != null) {
                            file = found;
                        }
                    }
                }
            }
            
            if (!file.exists()) {
                log.error("文件不存在: {}", decodedPath);
                System.out.println("文件不存在，搜索路径: " + decodedPath);
                if (uploadsDir != null) {
                    System.out.println("上传目录: " + uploadsDir.getAbsolutePath());
                    // 列出上传目录内容用于调试
                    if (uploadsDir.exists()) {
                        System.out.println("上传目录内容:");
                        for (File f : uploadsDir.listFiles()) {
                            System.out.println("  " + f.getName() + (f.isDirectory() ? "/" : ""));
                        }
                    }
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 直接读取文件字节
            byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
            String filename = file.getName();
            
            // 调试信息
            System.out.println("准备返回文件: " + file.getAbsolutePath());
            System.out.println("文件大小: " + fileBytes.length + " bytes");
            
            // 根据文件扩展名确定MIME类型
            MediaType mediaType = getMediaTypeForFile(filename);
            System.out.println("MIME类型: " + mediaType);
            
            // 如果是图片类型且不强制下载，使用inline模式让浏览器直接预览
            String contentDisposition;
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            if (!download && isImageType(mediaType)) {
                contentDisposition = "inline; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename;
            } else {
                contentDisposition = "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename;
            }
            
            System.out.println("Content-Disposition: " + contentDisposition);
            
            // 直接写入 response
            response.setContentType(mediaType.toString());
            response.setContentLength(fileBytes.length);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            
            // 写入文件内容
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
            
            System.out.println("文件发送成功，大小: " + fileBytes.length + " bytes");
        } catch (Exception e) {
            log.error("下载文件失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ex) {
                log.error("设置错误状态失败", ex);
            }
        }
    }
    
    /**
     * 根据文件名获取MIME类型
     */
    private MediaType getMediaTypeForFile(String filename) {
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex + 1).toLowerCase();
        }
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            case "png":
                return MediaType.IMAGE_PNG;
            case "gif":
                return MediaType.IMAGE_GIF;
            case "webp":
                return MediaType.parseMediaType("image/webp");
            case "svg":
                return MediaType.parseMediaType("image/svg+xml");
            case "pdf":
                return MediaType.APPLICATION_PDF;
            case "json":
                return MediaType.APPLICATION_JSON;
            case "xml":
                return MediaType.APPLICATION_XML;
            case "html":
            case "htm":
                return MediaType.TEXT_HTML;
            case "txt":
                return MediaType.TEXT_PLAIN;
            case "css":
                return MediaType.parseMediaType("text/css");
            case "js":
                return MediaType.parseMediaType("application/javascript");
            case "xlsx":
                return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "xls":
                return MediaType.parseMediaType("application/vnd.ms-excel");
            case "docx":
                return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "doc":
                return MediaType.parseMediaType("application/msword");
            default:
                return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 递归搜索文件（在子目录中查找）
     */
    private File searchFileInDir(File dir, String fileName) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile() && f.getName().equals(fileName)) {
                return f;
            }
            if (f.isDirectory()) {
                File found = searchFileInDir(f, fileName);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为图片类型
     */
    private boolean isImageType(MediaType mediaType) {
        return mediaType.getType().equals("image");
    }
}
