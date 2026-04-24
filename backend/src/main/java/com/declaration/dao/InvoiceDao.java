package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.DeclarationInvoice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceDao extends BaseMapper<DeclarationInvoice> {
}
