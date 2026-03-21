package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.DeclarationRemittance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 水单信息Mapper接口
 */
@Mapper
public interface DeclarationRemittanceDao extends BaseMapper<DeclarationRemittance> {
}
