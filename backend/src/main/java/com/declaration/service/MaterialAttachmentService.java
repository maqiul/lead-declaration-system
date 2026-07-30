package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.MaterialAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 资料项附件服务
 */
public interface MaterialAttachmentService extends IService<MaterialAttachment> {

    /**
     * 上传文件并关联到资料项
     */
    MaterialAttachment uploadForItem(Long itemId, MultipartFile file, String stage) throws IOException;

    /**
     * 查询某资料项的所有附件
     */
    List<MaterialAttachment> listByItemId(Long itemId);

    /**
     * 批量查询多个资料项的附件（避免 N+1）
     * @return key=itemId, value=附件列表
     */
    Map<Long, List<MaterialAttachment>> listByItemIds(Collection<Long> itemIds);

    /**
     * 删除单个附件
     */
    boolean removeAttachment(Long attachmentId);

    /**
     * 删除某资料项的所有附件
     */
    int removeAllByItemId(Long itemId);

    /**
     * 统计某资料项的附件数量
     */
    long countByItemId(Long itemId);
}
