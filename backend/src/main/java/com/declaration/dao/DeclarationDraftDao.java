package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.DeclarationDraft;
import org.apache.ibatis.annotations.Mapper;

/**
 * 申报单草稿Mapper
 */
@Mapper
public interface DeclarationDraftDao extends BaseMapper<DeclarationDraft> {
}
