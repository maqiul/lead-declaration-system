package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.PaymentRemittanceFormRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出款水单与申报单关联 Mapper 接口
 */
@Mapper
public interface PaymentRemittanceFormRelationDao extends BaseMapper<PaymentRemittanceFormRelation> {
}
