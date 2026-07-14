package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.entity.BusinessAuditRecord;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.FlowTemplateNode;
import com.declaration.flowable.DeclarationProcessVersionHelper;
import com.declaration.service.DeclarationFlowMigrationService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.FlowTemplateService;
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
    private final FlowTemplateService flowTemplateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> resumeOne(Long formId) {
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null) {
            throw new IllegalArgumentException("申报单不存在");
        }
        // 优先按表单的 templateCode 获取对应模板的最新流程定义，避免多模板时随机取到错误的流程
        ProcessDefinition latestDef = null;
        String templateCode = form.getTemplateCode();
        if (templateCode != null && !templateCode.isEmpty()) {
            latestDef = processVersionHelper.getLatestDefinition(templateCode);
        }
        if (latestDef == null) {
            latestDef = requireLatestNewProcessDefinition();
        }
        ActiveProcessSnapshot active = findActiveProcess(formId);
        MigrationTarget target = resolveTarget(form, active);
        if (target == null) {
            throw new IllegalArgumentException("当前状态(" + form.getStatus() + ")不支持恢复流程，仅允许 status 在 1~9 之间");
        }
        Integer statusBefore = form.getStatus();

        if (active != null) {
            if (active.newVersion) {
                // 检查流程定义 key 是否与表单 templateCode 一致
                String runningKey = active.processDefinitionId != null && active.processDefinitionId.contains(":")
                        ? active.processDefinitionId.substring(0, active.processDefinitionId.indexOf(':'))
                        : null;
                if (runningKey != null && !runningKey.equals(latestDef.getKey())) {
                    // 流程定义 key 不匹配，需终止错误流程并重新启动
                    log.warn("申报单 {} 流程定义不匹配: 运行中={}, 应为={}, 将终止并重新启动",
                            formId, runningKey, latestDef.getKey());
                    runtimeService.deleteProcessInstance(active.processInstanceId,
                            "resume-flow: 流程定义不匹配(" + runningKey + "→" + latestDef.getKey() + ")");
                    return startNewVersionInstance(form, formId, statusBefore, target, latestDef, "REPLACE_WRONG_KEY_THEN_START");
                }
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
                    // 额外检查流程定义 key 是否与表单 templateCode 一致
                    String tmplCode = form.getTemplateCode();
                    String runKey = active.processDefinitionId != null && active.processDefinitionId.contains(":")
                            ? active.processDefinitionId.substring(0, active.processDefinitionId.indexOf(':'))
                            : null;
                    if (tmplCode == null || tmplCode.isEmpty() || runKey == null || tmplCode.equals(runKey)) {
                        Map<String, Object> skip = row(form, "已是新版流程且当前节点正确");
                        skip.put("processDefinitionVersion", active.version);
                        skip.put("currentActivityId", active.currentActivityId);
                        skipped.add(skip);
                        continue;
                    }
                    // key 不匹配，不跳过，继续执行迁移
                }
                if (dryRun) {
                    Map<String, Object> preview = row(form, target.reason);
                    preview.put("targetActivityId", target.activityId);
                    preview.put("statusAfter", target.adjustedStatus != null ? target.adjustedStatus : form.getStatus());
                    if (active != null) {
                        preview.put("currentActivityId", active.currentActivityId);
                        preview.put("processDefinitionVersion", active.version);
                        preview.put("processDefinitionIsNew", active.newVersion);
                        // 检查 key 是否匹配，决定迁移操作类型
                        String tmplKey = form.getTemplateCode();
                        String activeKey = active.processDefinitionId != null && active.processDefinitionId.contains(":")
                                ? active.processDefinitionId.substring(0, active.processDefinitionId.indexOf(':')) : null;
                        if (active.newVersion && activeKey != null && tmplKey != null && !tmplKey.equals(activeKey)) {
                            preview.put("migrationAction", "REPLACE_WRONG_KEY_THEN_START");
                            preview.put("reason", "流程定义不匹配(" + activeKey + "→" + tmplKey + ")");
                        } else {
                            preview.put("migrationAction", active.newVersion ? "SYNC_NODE" : "REPLACE_OLD_THEN_START");
                        }
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
        log.info("迁移检查 formId={}, status={}, active={}, target={}",
                formId, form.getStatus(),
                active != null ? "{newVersion=" + active.newVersion + ", procDefId=" + active.processDefinitionId
                        + ", activity=" + active.currentActivityId + ", version=" + active.version + "}" : "null",
                target != null ? target.activityId : "null");
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
        // 检查流程定义 key 是否与表单的 templateCode 一致
        String templateCode = form.getTemplateCode();
        if (templateCode != null && !templateCode.isEmpty() && active.processDefinitionId != null) {
            String runningKey = active.processDefinitionId.contains(":")
                    ? active.processDefinitionId.substring(0, active.processDefinitionId.indexOf(':'))
                    : active.processDefinitionId;
            if (!templateCode.equals(runningKey)) {
                hint.put("needsMigration", true);
                hint.put("hint", "流程定义不匹配：表单关联「" + templateCode + "」但实际运行「" + runningKey + "」，建议恢复以切换到正确的流程");
                hint.put("processKeyMismatch", true);
                return hint;
            }
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

    /**
     * 校验目标节点是否存在于流程定义的 BPMN 中。
     * 若不存在，从模板节点配置（flow_template_node）动态查找下一个启用且 targetStatus 更大的节点作为回退目标。
     */
    private MigrationTarget resolveValidTarget(
            ProcessDefinition latestDef, MigrationTarget originalTarget, Integer currentStatus) {
        var bpmnModel = repositoryService.getBpmnModel(latestDef.getId());
        if (bpmnModel == null || bpmnModel.getMainProcess() == null) {
            return originalTarget;
        }
        // 收集 BPMN 中所有 UserTask 节点 ID
        var availableIds = new java.util.HashSet<String>();
        bpmnModel.getMainProcess().findFlowElementsOfType(org.flowable.bpmn.model.UserTask.class)
                .forEach(ut -> availableIds.add(ut.getId()));

        if (availableIds.contains(originalTarget.activityId)) {
            return originalTarget;
        }

        // 目标节点在 BPMN 中不存在，从模板节点配置动态查找回退目标
        log.warn("节点 {} 在流程定义 {} 中不存在，尝试从模板配置回退", originalTarget.activityId, latestDef.getKey());

        FlowTemplate template = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, latestDef.getKey())
                .one();
        if (template == null) {
            log.warn("未找到模板 code={}，无法回退", latestDef.getKey());
            return originalTarget;
        }

        // 获取模板启用的节点列表（按 sortOrder 排序，含 flow_node 关联信息）
        List<FlowTemplateNode> templateNodes = flowTemplateService.getTemplateNodes(template.getId());
        for (FlowTemplateNode tn : templateNodes) {
            if (tn.getEnabled() == null || tn.getEnabled() != 1) continue;
            if (tn.getNode() == null) continue;
            String nodeKey = tn.getNode().getNodeKey();
            Integer targetStatus = tn.getNode().getTargetStatus();
            // 找第一个在 BPMN 中存在、且 targetStatus > 当前状态的启用节点
            if (targetStatus != null && targetStatus > currentStatus && availableIds.contains(nodeKey)) {
                log.info("回退节点: {} → {} (targetStatus={})", originalTarget.activityId, nodeKey, targetStatus);
                return target(nodeKey, targetStatus, originalTarget.syncTemplates,
                        originalTarget.reason + "(节点回退)");
            }
        }
        // 兜底：返回 BPMN 中第一个可用节点
        String fallback = availableIds.stream().findFirst().orElse(null);
        if (fallback != null) {
            log.info("兜底回退: {} → {}", originalTarget.activityId, fallback);
            return target(fallback, currentStatus, originalTarget.syncTemplates, originalTarget.reason + "(兜底回退)");
        }
        return originalTarget;
    }

    /** 启动新版流程并跳转到目标节点 */
    private Map<String, Object> startNewVersionInstance(
            DeclarationForm form, Long formId, Integer statusBefore,
            MigrationTarget target, ProcessDefinition latestDef, String action) {
        ProcessInstance pi = null;
        try {
            // 校验并修正目标节点（流程定义可能不包含某些节点）
            target = resolveValidTarget(latestDef, target, form.getStatus());

            Map<String, Object> vars = new HashMap<>();
            vars.put("starterId", form.getCreateBy() != null ? String.valueOf(form.getCreateBy()) : "system");
            vars.put("resumeMode", true);
            pi = runtimeService.startProcessInstanceByKey(latestDef.getKey(), String.valueOf(formId), vars);

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
            throw new IllegalStateException("未找到启用的流程定义，请先部署 BPMN");
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
