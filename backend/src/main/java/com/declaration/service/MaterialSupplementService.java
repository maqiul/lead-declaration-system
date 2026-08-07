package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.MaterialSupplement;

import java.util.List;
import java.util.Map;

/**
 * 资料补交流程服务（独立 Flowable 流程，不阻塞申报主流程；
 * 流程未配置/启动失败时回退纯状态机）
 */
public interface MaterialSupplementService extends IService<MaterialSupplement> {

    /**
     * 发起资料补交（创建草稿补交单，审核人不可见；不启动流程）
     * 校验：申报单已进入资料环节（status>=2，退回待审11除外）、无在途/草稿补交；补交上传覆盖全部资料环节
     */
    MaterialSupplement start(Long formId, String reason, Long initiatorId);

    /**
     * 更新补交原因（仅草稿态 -1 可改）：发起补交免弹窗，原因可在上传资料过程中内联补填
     */
    void updateReason(Long supplementId, String reason, Long operatorId);

    /**
     * 查询某申报单当前补交单：优先在途（status=0），无则回退最新草稿（status=-1）；均无返回 null
     */
    MaterialSupplement getCurrentByFormId(Long formId);

    /**
     * 查询某申报单的补交历史（全部状态，按发起时间倒序），每条携带文件快照：
     * 记录每一次补交了哪些文件
     */
    List<MaterialSupplement> listHistoryByFormId(Long formId);

    /**
     * 提交补交审核：草稿(-1) → 补交中(0)，此时才启动补交流程，审核人才可见
     * 校验：必须为草稿态且已上传至少一条增量（附件或资料项）
     */
    void submitForAudit(Long supplementId, Long operatorId);

    /**
     * 查询某申报单在途的补交单（status=0），无则返回 null
     */
    MaterialSupplement getActiveByFormId(Long formId);

    /**
     * 批量查询在途补交单（列表页用）
     * @return formId -> 在途补交单ID（status=0，取最新一条）
     */
    Map<Long, Long> mapActiveByFormIds(List<Long> formIds);

    /**
     * 审核人待审补交列表（status=0）
     * @param declarationType 申报类型过滤（SELF-内部/EXTERNAL-外部），为空查全部
     */
    List<MaterialSupplement> listPending(String declarationType);

    /**
     * 取消草稿补交单（仅 status=-1）：删除草稿期增量并删除补交单
     */
    void cancel(Long supplementId, Long operatorId);

    /**
     * 增量明细：该补交单打标的资料项 + 附件
     * @return {items: [...], attachments: [...]}
     */
    Map<String, Object> getIncrements(Long supplementId);

    /**
     * 审核补交：通过→增量转正（清除 supplement_id，保留上传时 stage）；
     * 驳回→删除增量附件与增量资料项；均写 BusinessAuditRecord 留痕。
     * 有 Flowable 流程实例时通过 complete 任务推进流程，由监听器落地结果；
     * 无流程实例时直接更新状态
     */
    void audit(Long supplementId, boolean approved, String remark, Long auditorId);

    /**
     * 审核结果落地：增量转正/清除 + 状态更新 + 留痕。
     * 供 Flowable 结束监听器与无流程实例的直接审核路径复用
     */
    void applyAuditResult(MaterialSupplement supplement, boolean approved, String remark, Long auditorId);

    /**
     * 申报单退回草稿时清理：终止在途流程实例、删除增量资料、删除补交记录
     */
    void cleanupByFormId(Long formId);
}
