package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationAttachmentDao;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationAttachmentService;
import org.springframework.stereotype.Service;

/**
 * 申报单附件服务实现类
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 申报单附件服务实现类
 */
@Slf4j
@Service
public class DeclarationAttachmentServiceImpl extends ServiceImpl<DeclarationAttachmentDao, DeclarationAttachment> implements DeclarationAttachmentService {

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @Override
    public DeclarationAttachment uploadFile(MultipartFile file, String fileType) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 使用配置的上传路径
        File uploadDir = new File(uploadPath);
        
        // 确保目录存在
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new RuntimeException("无法创建上传目录: " + uploadDir.getAbsolutePath());
            }
        }
        
        // 创建日期子目录
        String dateDir = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        File dateDirFile = new File(uploadDir, dateDir);
        if (!dateDirFile.exists()) {
            boolean created = dateDirFile.mkdirs();
            if (!created) {
                log.warn("无法创建日期子目录: {}", dateDirFile.getAbsolutePath());
            }
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        File dest = new File(dateDirFile, fileName);

        // 保存文件到本地
        file.transferTo(dest);
        log.info("文件上传成功: {}, 存储路径: {}", originalFilename, dest.getAbsolutePath());

        // 保存记录到数据库
        DeclarationAttachment attachment = new DeclarationAttachment();
        attachment.setFileName(originalFilename);
        // 保存相对路径: yyyyMM/uuid.ext
        attachment.setFileUrl("/api/v1/files/download?path=" + dateDir + "/" + fileName);
        attachment.setFileType(fileType != null ? fileType : "Common");
        attachment.setCreateTime(LocalDateTime.now());
        
        this.save(attachment);
        
        return attachment;
    }
}
