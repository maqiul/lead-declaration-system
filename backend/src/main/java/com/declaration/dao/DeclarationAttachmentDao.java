package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.DeclarationAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 申报单附件Mapper接口
 */
@Mapper
public interface DeclarationAttachmentDao extends BaseMapper<DeclarationAttachment> {
}
