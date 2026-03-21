package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationAttachment;

/**
 * 申报单附件服务类
 */
public interface DeclarationAttachmentService extends IService<DeclarationAttachment> {
    /**
     * 上传文件并保存记录
     * @param file 文件对象
     * @param fileType 文件类型
     * @return 附件记录
     */
    DeclarationAttachment uploadFile(org.springframework.web.multipart.MultipartFile file, String fileType) throws java.io.IOException;
}
