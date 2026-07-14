package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.PaymentRemittance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 出款水单服务接口
 */
public interface PaymentRemittanceService extends IService<PaymentRemittance> {

    /** 创建出款水单 */
    PaymentRemittance createRemittance(PaymentRemittance remittance);

    /** 提交出款水单审核 */
    boolean submitForAudit(Long remittanceId);

    /** 审核出款水单 */
    boolean auditRemittance(Long remittanceId, boolean approved, Long bankAccountId, String auditRemark);

    /** 反审核出款水单（退回草稿） */
    boolean revokeAudit(Long remittanceId);

    /** 关联申报单 */
    boolean relateToForm(Long remittanceId, Long formId, BigDecimal amount, Integer relationType);

    /** 取消关联申报单 */
    boolean unrelateFromForm(Long remittanceId, Long formId);

    /** 获取出款水单关联的所有申报单 */
    List<Map<String, Object>> getRelatedForms(Long remittanceId);

    /** 获取申报单关联的所有出款水单 */
    List<Map<String, Object>> getRemittancesByFormId(Long formId);

    /** 分页查询出款水单 */
    IPage<PaymentRemittance> getPage(PageParam pageParam, Integer status, String paymentNo, String relationStatus);

    /** 检查申报单是否已关联至少一条已审核的出款水单 */
    boolean hasApprovedRemittance(Long formId);
}
