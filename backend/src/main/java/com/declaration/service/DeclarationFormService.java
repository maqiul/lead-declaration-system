package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.dto.AuditHistoryDTO;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.BusinessAuditRecord;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeliveryOrder;

import java.util.List;

/**
 * 出口申报单服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface DeclarationFormService extends IService<DeclarationForm> {

    /**
     * 保存申报单及其关联数据
     * @param form 申报单数据
     * @return 是否保存成功
     */
    boolean saveDeclarationForm(DeclarationForm form);

    /**
     * 根据ID获取完整的申报单信息（包含产品和箱子信息）
     * @param id 申报单ID
     * @return 完整的申报单信息
     */
    DeclarationForm getFullDeclarationForm(Long id);

    /**
     * 更新申报单及其关联数据
     * @param form 申报单数据
     * @return 是否更新成功
     */
    boolean updateDeclarationForm(DeclarationForm form);

    /**
     * 获取申报单统计数据
     * @return 统计数据
     */
    DeclarationStatisticsDTO getStatistics();

    // ================== 提货单相关方法 ==================

    /**
     * 保存提货单
     * @param deliveryOrder 提货单数据
     * @return 是否保存成功
     */
    boolean saveDeliveryOrder(DeliveryOrder deliveryOrder);

    /**
     * 获取申报单的提货单列表
     * @param formId 申报单ID
     * @return 提货单列表
     */
    List<DeliveryOrder> getDeliveryOrdersByFormId(Long formId);

    /**
     * 更新提货单
     * @param deliveryOrder 提货单数据
     * @return 是否更新成功
     */
    boolean updateDeliveryOrder(DeliveryOrder deliveryOrder);

    /**
     * 删除提货单
     * @param id 提货单ID
     * @return 是否删除成功
     */
    boolean deleteDeliveryOrder(Long id);

    /**
     * 根据ID获取提货单
     * @param id 提货单ID
     * @return 提货单信息
     */
    DeliveryOrder getDeliveryOrderById(Long id);

    // ================== 审核相关方法 ==================

    /**
     * 审核水单
     * @param id 水单ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 是否审核成功
     */
    /**
     * 审核水单
     * @deprecated 已废弃,请使用 DeclarationRemittanceService.auditRemittance()
     */
    @Deprecated
    boolean auditRemittance(Long id, boolean approved, String remark);

    /**
     * 审核提货单
     * @param id 提货单ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 是否审核成功
     */
    boolean auditDeliveryOrder(Long id, boolean approved, String remark);

    /**
     * 申请退回草稿
     * @param id 申报单ID
     * @param reason 申请原因
     * @return 是否成功
     */
    boolean applyReturnToDraft(Long id, String reason);

    /**
     * 审核退回草稿申请
     * @param id 申报单ID
     * @param approved 是否通过
     * @param remark 审核备注
     * @return 是否成功
     */
    boolean auditReturnToDraft(Long id, boolean approved, String remark);

    /**
     * 获取退回申请审核历史
     * @param id 申报单 ID
     * @return 审核记录列表（包含用户名）
     */
    List<AuditHistoryDTO> getReturnAuditHistory(Long id);
    
    /**
     * 保存审核历史记录
     * @param record 审核记录
     */
    void saveAuditRecord(BusinessAuditRecord record);
}