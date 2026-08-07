package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationMaterialItem;

import java.util.List;
import java.util.Map;

public interface DeclarationMaterialItemService extends IService<DeclarationMaterialItem> {

    /** 按申报单查询资料项（按 sort/id 排序） */
    List<DeclarationMaterialItem> listByFormId(Long formId);

    /**
     * 合并视图：模板派生的虚拟项（id=null） + 已有实例（包含单据内手动新增）
     * 懒创建模式：用户未操作的资料项不落库，只以虚拟项形式在前端展示
     */
    List<DeclarationMaterialItem> viewByFormId(Long formId);

    /**
     * 批量统计资料上传进度（列表页展示用）：口径与表单页进度一致，
     * 基于合并视图（模板虚拟项+实例）统计必填项总数与已上传数
     * @return formId -> { total, required, uploaded, percent }
     */
    Map<Long, Map<String, Object>> mapUploadProgress(List<Long> formIds);

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
     * @param skipRequiredCheck 为true时，必填不全不报错，而是创建豁免记录并阻塞主流程
     */
    void submit(Long formId, Long currentUserId, boolean skipRequiredCheck);

    /**
     * 资料审核：完成 materialAudit 任务
     * @param approved 通过/驳回
     * @param remark   审核备注
     */
    void audit(Long formId, boolean approved, String remark, Long auditorId);

    /**
     * 补充资料提交：完成 supplementSubmit 任务
     * 校验：SUPPLEMENT 阶段所有 required=1 的项都必须已上传附件
     */
    void submitSupplement(Long formId, Long currentUserId);

    /**
     * 补充资料审核：完成 supplementAudit 任务
     */
    void auditSupplement(Long formId, boolean approved, String remark, Long auditorId);

    /**
     * 申请开票金额提交：完成 invoiceAmountSubmit 任务
     * 前置校验：外汇水单已关联；自动计算开票金额并保存到 declaration_form.requested_invoice_amount
     */
    void submitInvoiceAmount(Long formId, Long currentUserId);

    /**
     * 开票金额审核：完成 invoiceAmountAudit 任务
     */
    void auditInvoiceAmount(Long formId, boolean approved, String remark, Long auditorId);

    /**
     * 业务发票提交：完成 invoiceSubmit 任务
     * 校验：至少已上传一张业务发票（declaration_invoice.category=1）
     */
    void submitInvoice(Long formId, Long currentUserId);

    /**
     * 业务发票审核：完成 invoiceAudit 任务
     */
    void auditInvoice(Long formId, boolean approved, String remark, Long auditorId);

    /**
     * 通用阶段提交：根据 stage（Flowable taskKey）完成对应任务
     * 支持 materialSubmit / supplementSubmit / invoiceSubmit 等
     * 与 form_section 字典的 submitKey 配合使用
     * @param skipRequiredCheck 为true时，必填不全不报错，而是创建豁免记录并阻塞主流程
     */
    void submitStage(Long formId, String stage, Long currentUserId, boolean skipRequiredCheck);

    /**
     * 基础资料环节（BASIC）必填校验：必填项未上传附件时抛出异常。
     * 不走豁免流程，供申报单提交入口（表单页/列表页）拦截使用
     */
    void validateBasicRequired(Long formId);

    /**
     * 通用阶段审核：根据 stage（Flowable auditTaskKey）完成对应审核任务
     * 支持 materialAudit / supplementAudit / invoiceAudit 等
     * 与 form_section 字典的 auditTaskKey 配合使用
     */
    void auditStage(Long formId, String stage, boolean approved, String remark, Long auditorId);
}
