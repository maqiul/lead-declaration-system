package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.DeclarationRemittance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 水单信息服务接口(集成Flowable)
 */
public interface DeclarationRemittanceService extends IService<DeclarationRemittance> {

    /**
     * 创建水单
     */
    DeclarationRemittance createRemittance(DeclarationRemittance remittance);

    /**
     * 提交水单审核(启动Flowable流程)
     */
    boolean submitForAudit(Long remittanceId);

    /**
     * 审核水单(完成Flowable任务)
     */
    boolean auditRemittance(Long remittanceId, boolean approved, Long bankAccountId, BigDecimal taxRate, String auditRemark);

    /**
     * 关联申报单
     */
    boolean relateToForm(Long remittanceId, Long formId, BigDecimal amount, Integer relationType);

    /**
     * 取消关联申报单
     */
    boolean unrelateFromForm(Long remittanceId, Long formId);

    /**
     * 获取水单关联的所有申报单
     */
    List<Map<String, Object>> getRelatedForms(Long remittanceId);

    /**
     * 获取申报单关联的所有水单
     */
    List<Map<String, Object>> getRemittancesByFormId(Long formId);

    /**
     * 分页查询水单
     */
    IPage<DeclarationRemittance> getPage(PageParam pageParam, Integer remittanceType, Integer status, String remittanceNo);

    /**
     * 计算水单手续费(根据选择的银行自动计算)
     */
    BigDecimal calculateBankFee(Long bankAccountId, BigDecimal amount);

    /**
     * 获取水单的Flowable流程实例ID
     */
    String getProcessInstanceId(Long remittanceId);

    /**
     * 获取当前待审核任务列表
     */
    List<Map<String, Object>> getPendingAuditTasks();
}
