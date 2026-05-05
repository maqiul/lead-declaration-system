package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationMaterialItem;

import java.util.List;

public interface DeclarationMaterialItemService extends IService<DeclarationMaterialItem> {

    /** 按申报单查询资料项（按 sort/id 排序） */
    List<DeclarationMaterialItem> listByFormId(Long formId);

    /**
     * 合并视图：模板派生的虚拟项（id=null） + 已有实例（包含单据内手动新增）
     * 懒创建模式：用户未操作的资料项不落库，只以虚拟项形式在前端展示
     */
    List<DeclarationMaterialItem> viewByFormId(Long formId);

    /**
     * 幂等确保模板对应的资料项实例已落库：已存在则直接返回，不存在则按模板克隆一条
     */
    DeclarationMaterialItem ensureItemFromTemplate(Long formId, Long templateId);

    /**
     * 从全局启用模板克隆到指定申报单（幂等：code 已存在的项不重复插入）
     * @return 本次新增数量
     */
    int syncFromTemplate(Long formId);

    /**
     * 资料提交：完成 materialSubmit 任务
     * 校验：所有 required=1 的项都必须已上传
     */
    void submit(Long formId, Long currentUserId);

    /**
     * 资料审核：完成 materialAudit 任务
     * @param approved 通过/驳回
     * @param remark   审核备注
     */
    void audit(Long formId, boolean approved, String remark, Long auditorId);

    /**
     * 业务发票提交：完成 invoiceSubmit 任务
     * 校验：至少已上传一张业务发票（declaration_invoice.category=1）
     */
    void submitInvoice(Long formId, Long currentUserId);

    /**
     * 业务发票审核：完成 invoiceAudit 任务
     */
    void auditInvoice(Long formId, boolean approved, String remark, Long auditorId);
}
