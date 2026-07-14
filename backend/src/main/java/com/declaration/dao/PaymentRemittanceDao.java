package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.PaymentRemittance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出款水单 Mapper 接口
 */
@Mapper
public interface PaymentRemittanceDao extends BaseMapper<PaymentRemittance> {
}
