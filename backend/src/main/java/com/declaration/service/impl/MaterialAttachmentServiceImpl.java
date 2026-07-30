package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.MaterialAttachmentDao;
import com.declaration.entity.MaterialAttachment;
import com.declaration.entity.User;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.MaterialAttachmentService;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资料项附件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAttachmentServiceImpl extends ServiceImpl<MaterialAttachmentDao, MaterialAttachment>
        implements MaterialAttachmentService {

    private final DeclarationAttachmentService attachmentService;
    private final UserService userService;

    @Override
    public MaterialAttachment uploadForItem(Long itemId, MultipartFile file, String stage) throws IOException {
        // 复用已有的文件存储服务
        DeclarationAttachment att = attachmentService.uploadFile(file, "MaterialItem");

        MaterialAttachment ma = new MaterialAttachment();
        ma.setItemId(itemId);
        ma.setFileName(att.getFileName());
        ma.setFileUrl(att.getFileUrl());
        ma.setFileSize(file.getSize());
        ma.setUploadTime(LocalDateTime.now());
        // 记录上传时所处环节（多环节共享时，后续环节不可删除前序环节上传的附件）
        ma.setStage(stage);
        if (StpUtil.isLogin()) {
            Long uid = StpUtil.getLoginIdAsLong();
            ma.setUploadBy(uid);
            ma.setCreateBy(uid);
            ma.setUpdateBy(uid);
        }
        this.save(ma);
        return ma;
    }

    @Override
    public List<MaterialAttachment> listByItemId(Long itemId) {
        if (itemId == null) return Collections.emptyList();
        LambdaQueryWrapper<MaterialAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialAttachment::getItemId, itemId)
               .orderByDesc(MaterialAttachment::getCreateTime);
        List<MaterialAttachment> list = this.list(wrapper);
        fillUploadByName(list);
        return list;
    }

    @Override
    public Map<Long, List<MaterialAttachment>> listByItemIds(Collection<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyMap();
        LambdaQueryWrapper<MaterialAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialAttachment::getItemId, itemIds)
               .orderByDesc(MaterialAttachment::getCreateTime);
        List<MaterialAttachment> all = this.list(wrapper);
        fillUploadByName(all);
        return all.stream().collect(Collectors.groupingBy(MaterialAttachment::getItemId));
    }

    /** 批量回填上传人/创建人/更新人显示名 */
    private void fillUploadByName(List<MaterialAttachment> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> ids = new HashSet<>();
        for (MaterialAttachment a : list) {
            if (a.getUploadBy() != null) ids.add(a.getUploadBy());
            if (a.getCreateBy() != null) ids.add(a.getCreateBy());
            if (a.getUpdateBy() != null) ids.add(a.getUpdateBy());
        }
        if (ids.isEmpty()) return;
        Map<Long, String> nameMap = new HashMap<>();
        try {
            List<User> users = userService.listByIds(ids);
            if (users != null) {
                for (User u : users) {
                    String display = (u.getNickname() != null && !u.getNickname().isBlank())
                            ? u.getNickname() : u.getUsername();
                    nameMap.put(u.getId(), display);
                }
            }
        } catch (Exception e) {
            log.warn("回填附件人名失败：{}", e.getMessage());
        }
        for (MaterialAttachment a : list) {
            if (a.getUploadBy() != null) a.setUploadByName(nameMap.get(a.getUploadBy()));
            if (a.getCreateBy() != null) a.setCreateByName(nameMap.get(a.getCreateBy()));
            if (a.getUpdateBy() != null) a.setUpdateByName(nameMap.get(a.getUpdateBy()));
        }
    }

    @Override
    public boolean removeAttachment(Long attachmentId) {
        return this.removeById(attachmentId);
    }

    @Override
    public int removeAllByItemId(Long itemId) {
        LambdaQueryWrapper<MaterialAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialAttachment::getItemId, itemId);
        return this.baseMapper.delete(wrapper);
    }

    @Override
    public long countByItemId(Long itemId) {
        if (itemId == null) return 0;
        LambdaQueryWrapper<MaterialAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialAttachment::getItemId, itemId);
        return this.count(wrapper);
    }
}
