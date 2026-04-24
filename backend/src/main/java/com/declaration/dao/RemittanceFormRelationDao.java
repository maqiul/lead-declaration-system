package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.RemittanceFormRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 水单与申报单关联Mapper接口
 */
@Mapper
public interface RemittanceFormRelationDao extends BaseMapper<RemittanceFormRelation> {
}
