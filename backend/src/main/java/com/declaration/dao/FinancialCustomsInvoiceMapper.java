package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.entity.FinancialCustomsInvoice;
import org.apache.ibatis.annotations.Param;

/**
 * 报关发票Mapper接口
 */
public interface FinancialCustomsInvoiceMapper extends BaseMapper<FinancialCustomsInvoice> {
    
    /**
     * 分页查询报关发票
     */
    IPage<FinancialCustomsInvoice> selectCustomsInvoicePage(Page<FinancialCustomsInvoice> page, 
                                                           @Param("declarationFormCode") String declarationFormCode,
                                                           @Param("invoiceNo") String invoiceNo,
                                                           @Param("customsBroker") String customsBroker,
                                                           @Param("status") Integer status,
                                                           @Param("orgId") Long orgId);
    
    /**
     * 根据申报单ID查询报关发票
     */
    FinancialCustomsInvoice selectByDeclarationFormId(@Param("declarationFormId") Long declarationFormId);
}