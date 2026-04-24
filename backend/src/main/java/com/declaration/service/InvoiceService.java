package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.DeclarationInvoice;

public interface InvoiceService extends IService<DeclarationInvoice> {
    IPage<DeclarationInvoice> getFinanceInvoices(PageParam pageParam, Integer invoiceType, String invoiceNo, Long formId);
}
