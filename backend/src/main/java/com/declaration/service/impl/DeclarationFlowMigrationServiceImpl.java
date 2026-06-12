package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.entity.BusinessAuditRecord;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.flowable.DeclarationProcessVersionHelper;
import com.declaration.service.DeclarationFlowMigrationService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationMaterialItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 老申报流程迁移实现。
 *
 * <p>背景：旧版 BPMN 在「资料提交/审核」后流程实例即结束，但业务 status 仍停留在 2/3 等，
 * 导致无法完成 materialSubmit / materialAudit 等 Flowable 任务。</p>
 *
 * <p>新版流程在资料审核通过后增加「补充资料」「申请开票金额」等环节，需按业务痕迹智能定位节点。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationFlowMigrationServiceImpl implements DeclarationFlowMigrationService {

    private static final String BT_MATERIAL_AUDIT = "DECLARATION_MATERIAL_AUDIT";

    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final DeclarationProcessVersionHelper processVersionHelper;
    private final DeclarationFormService declarationFormService;
    private final DeclarationMaterialItemService materialItemService;
    private final BusinessAuditRecordDao auditRecordDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resumeOne(Long formId) {
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null) {
            throw new IllegalArgumentException("申报单不存在");
        }
        ProcessDefinition latestDef = requireLatestNewProcessDefinition();
        ActiveProcessSnapshot active = findActiveProcess(formId);
        MigrationTarget target = resolveTarget(form, active);
        if (target == null) {
            throw new IllegalArgumentException("当前状态(" + form.getStatus() + ")不支持恢复流程，仅允许 status 在 1~9 之间");
        }
        Integer statusBefore = form.getStatus();

        if (active != null) {
            if (active.newVersion) {
                return syncNewVersionInstance(form, formId, statusBefore, target, active, latestDef);
            }
            // 旧版流程仍在运行：终止后按新版重建
            log.warn("申报单 {} 存在旧版流程实例 procDefId={} v{}，将终止并迁移到新版",
                    formId, active.processDefinitionId, active.version);
            runtimeService.deleteProcessInstance(active.processInstanceId, "resume-flow: 旧版流程迁移到新版");
            return startNewVersionInstance(form, formId, statusBefore, target, latestDef, "REPLACE_OLD_THEN_START");
        }

        return startNewVersionInstance(form, formId, statusBefore, target, latestDef, "START_NEW");
    }

    @Override
    public Map<String, Object> resumeBatch(boolean dryRun, List<Integer> statuses) {
        List<Integer> filter = (statuses == null || statuses.isEmpty())
                ? List.of(1, 2, 3, 4, 5, 6, 7, 8, 9)
                : statuses;

        List<DeclarationForm> candidates = declarationFormService.lambdaQuery()
                .in(DeclarationForm::getStatus, filter)
                .list();

        List<Map<String, Object>> success = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        for (DeclarationForm form : candidates) {
            Long formId = form.getId();
            try {
                ActiveProcessSnapshot active = findActiveProcess(formId);
                MigrationTarget target = resolveTarget(form, active);
                if (target == null) {
                    skipped.add(row(form, "状态不支持"));
                    continue;
                }
                if (active != null && active.newVersion && target.activityId.equals(active.currentActivityId)) {
                    Map<String, Object> skip = row(form, "已是新版流程且当前节点正确");
                    skip.put("processDefinitionVersion", active.version);
                    skip.put("currentActivityId", active.currentActivityId);
                    skipped.add(skip);
                    continue;
                }
                if (dryRun) {
                    Map<String, Object> preview = row(form, target.reason);
                    preview.put("targetActivityId", target.activityId);
                    preview.put("statusAfter", target.adjustedStatus != null ? target.adjustedStatus : form.getStatus());
                    if (active != null) {
                        preview.put("currentActivityId", active.currentActivityId);
                        preview.put("processDefinitionVersion", active.version);
                        preview.put("processDefinitionIsNew", active.newVersion);
                        preview.put("migrationAction", active.newVersion ? "SYNC_NODE" : "REPLACE_OLD_THEN_START");
                    } else {
                        preview.put("migrationAction", "START_NEW");
                    }
                    success.add(preview);
                    continue;
                }
                Map<String, Object> one = resumeOne(formId);
                success.add(one);
            } catch (Exception e) {
                Map<String, Object> err = row(form, e.getMessage());
                failed.add(err);
                log.warn("批量恢复失败 formId={}: {}", formId, e.getMessage());
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dryRun", dryRun);
        summary.put("statusFilter", filter);
        summary.put("totalCandidates", candidates.size());
        summary.put("successCount", success.size());
        summary.put("skippedCount", skipped.size());
        summary.put("failedCount", failed.size());
        summary.put("success", success);
        summary.put("skipped", skipped);
        summary.put("failed", failed);
        return summary;
    }

    @Override
    public boolean needsLegacyMigration(Long formId) {
        Map<String, Object> hint = migrationHint(formId);
        return Boolean.TRUE.equals(hint.get("needsMigration"));
    }

    @Override
    public Map<String, Object> migrationHint(Long formId) {
        Map<String, Object> hint = new LinkedHashMap<>();
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null || form.getStatus() == null || form.getStatus() < 1 || form.getStatus() > 9) {
            hint.put("needsMigration", false);
            return hint;
        }
        ActiveProcessSnapshot active = findActiveProcess(formId);
        MigrationTarget target = resolveTarget(form, active);
        if (target == null) {
            hint.put("needsMigration", false);
            return hint;
        }
        hint.put("targetActivityId", target.activityId);
        hint.put("statusAfter", target.adjustedStatus);
        hint.put("reason", target.reason);
        if (active == null) {
            hint.put("legacyProcess", false);
            hint.put("needsMigration", true);
            hint.put("hint", "流程已结束或未启动，建议恢复以挂载新版流程");
            return hint;
        }
        hint.put("legacyProcess", !active.newVersion);
        hint.put("processDefinitionVersion", active.version);
        hint.put("currentActivityId", active.currentActivityId);
        // 旧版流程实例：无论当前在资料提交/审核还是发票节点，均需替换为新版定义
        if (!active.newVersion) {
            hint.put("needsMigration", true);
            hint.put("hint", buildLegacyMigrationHint(form.getStatus(), active.currentActivityId));
            return hint;
        }
        boolean needs = !target.activityId.equals(active.currentActivityId);
        hint.put("needsMigration", needs);
        if (needs) {
            hint.put("hint", "新版流程节点与业务状态不一致，建议恢复以同步节点");
        }
        return hint;
    }

    /**
     * 根据业务状态、审核痕迹、当前运行实例版本，解析应跳转的 BPMN 节点。
     */
    MigrationTarget resolveTarget(DeclarationForm form, ActiveProcessSnapshot active) {
        if (active != null && !active.newVersion) {
            MigrationTarget legacy = resolveLegacyRunningTarget(form, active);
            if (legacy != null) {
                return legacy;
            }
        }
        return resolveTargetByStatus(form);
    }

    /**
     * 旧版流程实例仍在运行：按当前节点纠正迁移目标（避免落在 invoiceSubmit）。
     */
    private MigrationTarget resolveLegacyRunningTarget(DeclarationForm form, ActiveProcessSnapshot active) {
        Long formId = form.getId();
        String node = active.currentActivityId;
        if (node == null) {
            return null;
        }
        switch (node) {
            case "invoiceSubmit":
            case "invoiceAudit":
                if (hasApprovedMaterialAudit(formId) || form.getStatus() != null && form.getStatus() >= 8) {
                    return target("supplementSubmit", 4, true,
                            "老流程运行在发票节点(资料审后误跳)，迁移到新版补充资料提交");
                }
                return target("invoiceSubmit", 8, true, "老流程-业务发票");
            case "materialSubmit":
                return target("materialSubmit", 2, false, "老流程-资料提交");
            case "materialAudit":
                return resolveStatus3(formId);
            case "deptAudit":
                return target("deptAudit", 1, false, "老流程-待初审");
            default:
                return resolveTargetByStatus(form);
        }
    }

    MigrationTarget resolveTargetByStatus(DeclarationForm form) {
        Integer status = form.getStatus();
        if (status == null || status < 1 || status > 9) {
            return null;
        }

        Long formId = form.getId();

        switch (status) {
            case 1:
                return target("deptAudit", 1, false, "待初审");
            case 2:
                return target("materialSubmit", 2, false, "待资料提交");
            case 3:
                return resolveStatus3(formId);
            case 4:
                return target("supplementSubmit", 4, true, "待补充资料提交");
            case 5:
                return target("supplementAudit", 5, false, "待补充资料审核");
            case 6:
                return target("invoiceAmountSubmit", 6, false, "待申请开票金额");
            case 7:
                return target("invoiceAmountAudit", 7, false, "待开票金额审核");
            case 8:
                return resolveStatus8(formId);
            case 9:
                return target("invoiceAudit", 9, false, "待发票审核");
            default:
                return null;
        }
    }

    /** status=3：区分「待审资料」与「资料已审过应进补充资料」 */
    private MigrationTarget resolveStatus3(Long formId) {
        // 检查是否有已审核通过的「退回上一步」记录（从 status=4 退回到 status=3）
        // 如果有，说明是故意退回到资料审核节点重新审核，不需要迁移
        if (hasApprovedRollback(formId, 4)) {
            return target("materialAudit", 3, false, "退回上一步重新审核");
        }
        BusinessAuditRecord approved = latestMaterialAudit(formId, 1);
        if (approved != null) {
            return target("supplementSubmit", 4, true,
                    "资料审核已通过（老流程未进补充资料环节），迁移到补充资料提交");
        }
        BusinessAuditRecord pending = latestMaterialAudit(formId, 0);
        if (pending != null) {
            return target("materialAudit", 3, false, "资料已提交待审核");
        }
        if (hasUploadedMaterialItems(formId)) {
            return target("materialAudit", 3, false, "已有资料上传记录，迁移到资料审核");
        }
        return target("materialSubmit", 2, false, "尚未提交资料，迁移到资料提交");
    }

    /**
     * status=8：老版「待发票提交」(原 status=4) 经 SQL 迁移而来，若从未做补充资料则回到补充环节。
     */
    private MigrationTarget resolveStatus8(Long formId) {
        if (!hasSupplementProgress(formId) && hasApprovedMaterialAudit(formId)) {
            return target("supplementSubmit", 4, true,
                    "老流程资料审过后直接进入发票环节，补走补充资料提交");
        }
        return target("invoiceSubmit", 8, true, "待业务发票提交");
    }

    private MigrationTarget target(String activityId, int status, boolean syncTemplates, String reason) {
        MigrationTarget t = new MigrationTarget();
        t.activityId = activityId;
        t.adjustedStatus = status;
        t.syncTemplates = syncTemplates;
        t.reason = reason;
        return t;
    }

    /** 在新版流程实例上同步到目标节点（不重新启动流程） */
    private Map<String, Object> syncNewVersionInstance(
            DeclarationForm form, Long formId, Integer statusBefore,
            MigrationTarget target, ActiveProcessSnapshot active, ProcessDefinition latestDef) {
        String fromActivity = active.currentActivityId;
        if (!fromActivity.equals(target.activityId)) {
            runtimeService.createChangeActivityStateBuilder()
                    .processInstanceId(active.processInstanceId)
                    .moveActivityIdTo(fromActivity, target.activityId)
                    .changeState();
        }
        applyPostMigration(form, formId, target);
        Map<String, Object> data = buildResult(form, formId, statusBefore, target,
                active.processInstanceId, fromActivity, latestDef, "SYNC_NODE");
        log.info("申报单 {} 新版流程节点同步 {} -> {}", formId, fromActivity, target.activityId);
        return data;
    }

    /** 启动新版流程并跳转到目标节点 */
    private Map<String, Object> startNewVersionInstance(
            DeclarationForm form, Long formId, Integer statusBefore,
            MigrationTarget target, ProcessDefinition latestDef, String action) {
        ProcessInstance pi = null;
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("starterId", form.getCreateBy() != null ? String.valueOf(form.getCreateBy()) : "system");
            vars.put("resumeMode", true);
            pi = runtimeService.startProcessInstanceByKey(DeclarationProcessVersionHelper.PROCESS_KEY, String.valueOf(formId), vars);

            String currentActivity = resolveCurrentActivityId(pi.getId());
            if (!currentActivity.equals(target.activityId)) {
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(pi.getId())
                        .moveActivityIdTo(currentActivity, target.activityId)
                        .changeState();
            }
            runtimeService.removeVariable(pi.getId(), "resumeMode");
            applyPostMigration(form, formId, target);

            Map<String, Object> data = buildResult(form, formId, statusBefore, target,
                    pi.getId(), currentActivity, latestDef, action);
            log.info("申报单 {} 流程已恢复 {} -> {} ({})", formId, currentActivity, target.activityId, target.reason);
            return data;
        } catch (Exception e) {
            if (pi != null) {
                try {
                    runtimeService.deleteProcessInstance(pi.getId(), "resume-flow 失败回滚");
                } catch (Exception ex) {
                    log.error("回滚流程实例 {} 失败", pi.getId(), ex);
                }
            }
            throw new RuntimeException("恢复流程失败: " + e.getMessage(), e);
        }
    }

    private void applyPostMigration(DeclarationForm form, Long formId, MigrationTarget target) {
        if (target.syncTemplates) {
            materialItemService.syncFromTemplate(formId);
        }
        if (target.adjustedStatus != null && !target.adjustedStatus.equals(form.getStatus())) {
            form.setStatus(target.adjustedStatus);
            declarationFormService.updateById(form);
        }
    }

    private Map<String, Object> buildResult(
            DeclarationForm form, Long formId, Integer statusBefore, MigrationTarget target,
            String processInstanceId, String fromActivityId, ProcessDefinition latestDef, String action) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("formId", formId);
        data.put("formNo", form.getFormNo());
        data.put("processInstanceId", processInstanceId);
        data.put("fromActivityId", fromActivityId);
        data.put("targetActivityId", target.activityId);
        data.put("statusBefore", statusBefore);
        data.put("statusAfter", target.adjustedStatus != null ? target.adjustedStatus : statusBefore);
        data.put("reason", target.reason);
        data.put("migrationAction", action);
        data.put("processDefinitionId", latestDef.getId());
        data.put("processDefinitionVersion", latestDef.getVersion());
        data.put("processDefinitionIsNew", true);
        return data;
    }

    private ProcessDefinition requireLatestNewProcessDefinition() {
        ProcessDefinition latest = processVersionHelper.getLatestDefinition();
        if (latest == null) {
            throw new IllegalStateException("未找到流程定义 " + DeclarationProcessVersionHelper.PROCESS_KEY + "，请先部署 BPMN");
        }
        if (!processVersionHelper.isNewVersionDefinition(latest.getId())) {
            throw new IllegalStateException(
                    "当前最新流程定义(v" + latest.getVersion() + ")不含「补充资料」节点，"
                            + "请先部署新版 declaration-process.bpmn20.xml");
        }
        return latest;
    }

    private ActiveProcessSnapshot findActiveProcess(Long formId) {
        List<ProcessInstance> running = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(String.valueOf(formId))
                .list();
        ProcessInstance pi = running.isEmpty() ? null : running.get(0);
        if (pi == null) {
            return null;
        }
        ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(pi.getProcessDefinitionId())
                .singleResult();
        ActiveProcessSnapshot snap = new ActiveProcessSnapshot();
        snap.processInstanceId = pi.getId();
        snap.processDefinitionId = pi.getProcessDefinitionId();
        snap.version = def != null ? def.getVersion() : -1;
        snap.newVersion = processVersionHelper.isNewVersionDefinition(pi.getProcessDefinitionId());
        snap.currentActivityId = resolveCurrentActivityId(pi.getId());
        return snap;
    }

    private String resolveCurrentActivityId(String processInstanceId) {
        return runtimeService.createExecutionQuery()
                .processInstanceId(processInstanceId)
                .onlyChildExecutions()
                .list().stream()
                .map(Execution::getActivityId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("deptAudit");
    }

    private static final class ActiveProcessSnapshot {
        String processInstanceId;
        String processDefinitionId;
        int version;
        boolean newVersion;
        String currentActivityId;
    }

    private BusinessAuditRecord latestMaterialAudit(Long formId, int auditStatus) {
        return auditRecordDao.selectOne(
                new LambdaQueryWrapper<BusinessAuditRecord>()
                        .eq(BusinessAuditRecord::getBusinessId, formId)
                        .eq(BusinessAuditRecord::getBusinessType, BT_MATERIAL_AUDIT)
                        .eq(BusinessAuditRecord::getAuditStatus, auditStatus)
                        .orderByDesc(BusinessAuditRecord::getApplyTime)
                        .last("LIMIT 1"));
    }

    private boolean hasApprovedMaterialAudit(Long formId) {
        return latestMaterialAudit(formId, 1) != null;
    }

    /** 检查是否有已审核通过的「退回上一步」记录（从指定 preStatus 退回） */
    private boolean hasApprovedRollback(Long formId, int preStatus) {
        return auditRecordDao.selectCount(
                new LambdaQueryWrapper<BusinessAuditRecord>()
                        .eq(BusinessAuditRecord::getBusinessId, formId)
                        .eq(BusinessAuditRecord::getBusinessType, "DECLARATION_ROLLBACK")
                        .eq(BusinessAuditRecord::getAuditStatus, 1)
                        .eq(BusinessAuditRecord::getPreStatus, preStatus)
        ) > 0;
    }

    private boolean hasUploadedMaterialItems(Long formId) {
        return materialItemService.lambdaQuery()
                .eq(DeclarationMaterialItem::getFormId, formId)
                .eq(DeclarationMaterialItem::getStatus, 1)
                .count() > 0;
    }

    private boolean hasSupplementProgress(Long formId) {
        long supplementItems = materialItemService.lambdaQuery()
                .eq(DeclarationMaterialItem::getFormId, formId)
                .eq(DeclarationMaterialItem::getStage, "SUPPLEMENT")
                .count();
        if (supplementItems == 0) {
            return false;
        }
        return materialItemService.lambdaQuery()
                .eq(DeclarationMaterialItem::getFormId, formId)
                .eq(DeclarationMaterialItem::getStage, "SUPPLEMENT")
                .eq(DeclarationMaterialItem::getStatus, 1)
                .count() > 0;
    }

    private String buildLegacyMigrationHint(Integer status, String currentActivityId) {
        if (status != null && status == 2) {
            return "当前为旧版流程（资料提交环节），迁移后将使用新版流程并保留待资料提交状态";
        }
        if (status != null && status == 3) {
            return "当前为旧版流程（资料审核环节），迁移后将使用新版流程；若资料已审过将自动进入补充资料环节";
        }
        if ("materialSubmit".equals(currentActivityId) || "materialAudit".equals(currentActivityId)) {
            return "当前为旧版流程（资料环节），迁移到新版后可继续补充资料、开票金额等步骤";
        }
        if ("invoiceSubmit".equals(currentActivityId) || "invoiceAudit".equals(currentActivityId)) {
            return "当前为旧版流程（发票环节），迁移到新版补充资料环节";
        }
        return "当前为旧版流程，需迁移到新版流程定义";
    }

    private Map<String, Object> row(DeclarationForm form, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("formId", form.getId());
        m.put("formNo", form.getFormNo());
        m.put("status", form.getStatus());
        m.put("message", message);
        return m;
    }

    private static final class MigrationTarget {
        String activityId;
        Integer adjustedStatus;
        boolean syncTemplates;
        String reason;
    }
}
