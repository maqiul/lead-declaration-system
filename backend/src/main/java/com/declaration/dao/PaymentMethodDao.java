package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付方式配置Mapper接口
 */
@Mapper
public interface PaymentMethodDao extends BaseMapper<PaymentMethod> {
}
