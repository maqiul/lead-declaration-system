package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.InvoiceDao;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationInvoice;
import com.declaration.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceDao, DeclarationInvoice> implements InvoiceService {

    @Autowired
    private DeclarationFormDao declarationFormDao;

    @Override
    public IPage<DeclarationInvoice> getFinanceInvoices(PageParam pageParam, Integer invoiceType, String invoiceNo, Long formId) {
        Page<DeclarationInvoice> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        LambdaQueryWrapper<DeclarationInvoice> wrapper = new LambdaQueryWrapper<>();

        // 强制只查询财务留底数据
        wrapper.eq(DeclarationInvoice::getCategory, 2);

        if (invoiceType != null) {
            wrapper.eq(DeclarationInvoice::getInvoiceType, invoiceType);
        }
        if (invoiceNo != null && !invoiceNo.isEmpty()) {
            wrapper.like(DeclarationInvoice::getInvoiceNo, invoiceNo);
        }
        if (formId != null) {
            wrapper.eq(DeclarationInvoice::getFormId, formId);
        }

        wrapper.orderByDesc(DeclarationInvoice::getCreateTime);
        IPage<DeclarationInvoice> result = page(page, wrapper);

        // 批量填充申报单号
        fillFormNo(result.getRecords());

        return result;
    }

    /**
     * 批量填充申报单号
     */
    private void fillFormNo(java.util.List<DeclarationInvoice> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            return;
        }

        // 提取所有formId
        java.util.List<Long> formIds = invoices.stream()
                .map(DeclarationInvoice::getFormId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (formIds.isEmpty()) {
            return;
        }

        // 批量查询申报单
        java.util.List<DeclarationForm> forms = declarationFormDao.selectBatchIds(formIds);
        Map<Long, String> formNoMap = forms.stream()
                .collect(Collectors.toMap(DeclarationForm::getId, DeclarationForm::getFormNo));

        // 填充申报单号到发票对象
        for (DeclarationInvoice invoice : invoices) {
            if (invoice.getFormId() != null && formNoMap.containsKey(invoice.getFormId())) {
                invoice.setFormNo(formNoMap.get(invoice.getFormId()));
            }
        }
    }
}
