package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 提货单Mapper接口
 */
@Mapper
public interface DeliveryOrderDao extends BaseMapper<DeliveryOrder> {
}
