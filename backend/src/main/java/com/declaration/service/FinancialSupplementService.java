package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.FinancialSupplement;

import java.util.Map;

public interface FinancialSupplementService extends IService<FinancialSupplement> {
    
    /**
     * 获取开票明细计算过程
     * @param formId 申报单ID
     * @return 包含完整计算步骤的Map
     */
    Map<String, Object> getCalculationDetail(Long formId);
}
