package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationMaterialExemption;

import java.util.List;

/**
 * 资料豁免审批服务
 */
public interface DeclarationMaterialExemptionService extends IService<DeclarationMaterialExemption> {

    /**
     * 创建豁免记录（资料提交时必填不全，用户选择强制提交）
     * @return 豁免记录ID
     */
    Long createExemption(Long formId, String stage, String missingItemsJson,
                         String exemptionType, String mainTaskId, Long createBy);

    /**
     * 查询指定表单+阶段的待审核豁免记录
     */
    DeclarationMaterialExemption getPendingExemption(Long formId, String stage);

    /**
     * 查询指定表单+阶段的已通过豁免记录
     */
    DeclarationMaterialExemption getApprovedExemption(Long formId, String stage);

    /**
     * 查询指定表单的所有豁免记录
     */
    List<DeclarationMaterialExemption> listByFormId(Long formId);

    /**
     * 审核豁免（通过/驳回）
     * 通过时：complete 主流程被阻塞的任务
     * 驳回时：标记状态，主流程保持阻塞
     */
    void auditExemption(Long exemptionId, boolean approved, String remark, Long auditorId);
}
