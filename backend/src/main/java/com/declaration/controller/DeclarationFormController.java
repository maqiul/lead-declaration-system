package com.declaration.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.entity.BusinessAuditRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.dto.AuditHistoryDTO;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.*;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.service.*;
import com.declaration.utils.OrganizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
// import org.springframework.jdbc.core.JdbcTemplate; // Removed unused
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 申报单管理控制器 - RESTful API规范
 */
@Slf4j
@RestController
@RequestMapping("/v1/declarations")
@RequiredArgsConstructor
@Tag(name = "申报单管理", description = "出口申报单相关接口")
public class DeclarationFormController {

    private final DeclarationFormService declarationFormService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final DeclarationProductService declarationProductService;
    private final DeclarationCartonService declarationCartonService;
    private final DeclarationCartonProductService declarationCartonProductService;
    private final ExcelExportService excelExportService;
    private final DeclarationAttachmentService attachmentService;
    private final DeclarationRemittanceService remittanceService;
    private final ProcessInstanceService processInstanceService;
    private final TaskService flowableTaskService;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final BusinessAuditRecordDao auditRecordDao;

    /**
     * 获取申报单统计数据
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取申报单统计数据")
    @RequiresPermissions("business:declaration:statistics")
    public Result<DeclarationStatisticsDTO> getStatistics() {
        DeclarationStatisticsDTO statistics = declarationFormService.getStatistics();
        return Result.success(statistics);
    }

    /**
     * 保存水单信息
     */
    @PostMapping("/{id}/remittance")
    @Operation(summary = "保存水单信息")
    @RequiresPermissions("business:declaration:edit")
    public Result<DeclarationRemittance> saveRemittance(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody DeclarationRemittance remittance) {
        
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }
        
        remittance.setFormId(id);
        remittanceService.saveOrUpdate(remittance);
        
        try {
            // 自动生成水单记录导出文件（Service 内部已处理保存/替换逻辑）
            excelExportService.generateAndSaveRemittanceReport(remittance, form);
        } catch (Exception e) {
            log.warn("生成水单导出文件失败（不影响水单数据保存）: {}", e.getMessage());
        }
        
        return Result.success(remittance);
    }

    /**
     * 替换申报单附件
     */
    @PostMapping("/{id}/attachments/{attachmentId}/replace")
    @Operation(summary = "替换申报单附件")
    @RequiresPermissions("business:declaration:edit")
    public Result<DeclarationAttachment> replaceAttachment(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Parameter(description = "附件ID") @PathVariable Long attachmentId,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 验证申报单是否存在
            DeclarationForm form = declarationFormService.getById(id);
            if (form == null) {
                return Result.fail("申报单不存在");
            }
            
            // 验证附件是否存在
            DeclarationAttachment oldAttachment = attachmentService.getById(attachmentId);
            if (oldAttachment == null) {
                return Result.fail("附件不存在");
            }
            
            // 验证附件属于该申报单
            if (!oldAttachment.getFormId().equals(id)) {
                return Result.fail("附件不属于该申报单");
            }
            
            // 校验文件格式必须一致
            String originalFileName = oldAttachment.getFileName();
            String newFileName = file.getOriginalFilename();
            if (originalFileName != null && newFileName != null) {
                String originalExt = getFileExtension(originalFileName);
                String newExt = getFileExtension(newFileName);
                if (!originalExt.equalsIgnoreCase(newExt)) {
                    return Result.fail("文件格式必须与原文件一致 (原格式: " + originalExt + ")");
                }
            }
            
            // 上传新文件
            DeclarationAttachment newAttachment = attachmentService.uploadFile(file, oldAttachment.getFileType());
            if (newAttachment == null) {
                return Result.fail("文件上传失败");
            }
            
            // 更新附件关联关系
            newAttachment.setFormId(id);
            newAttachment.setId(oldAttachment.getId()); // 保持原ID，实现替换而不是新增
            newAttachment.setCreateTime(oldAttachment.getCreateTime()); // 保持原始创建时间
            
            // 更新数据库记录
            boolean updated = attachmentService.updateById(newAttachment);
            if (!updated) {
                return Result.fail("附件替换失败");
            }
            
            log.info("申报单 {} 的附件 {} 替换成功", id, attachmentId);
            return Result.success(newAttachment);
            
        } catch (Exception e) {
            log.error("替换附件失败", e);
            return Result.fail("替换附件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取申报单附件列表
     */
    @GetMapping("/{id}/attachments")
    @Operation(summary = "获取申报单附件列表")
    @RequiresPermissions("business:declaration:view")
    public Result<List<DeclarationAttachment>> getAttachments(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        
        LambdaQueryWrapper<DeclarationAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationAttachment::getFormId, id);
        wrapper.orderByDesc(DeclarationAttachment::getCreateTime);
        
        List<DeclarationAttachment> attachments = attachmentService.list(wrapper);
        return Result.success(attachments);
    }

    /**
     * 获取水单详情
     */
    @GetMapping("/remittance/{remittanceId}")
    @Operation(summary = "获取水单详情")
    @RequiresPermissions("business:declaration:view")
    public Result<DeclarationRemittance> getRemittance(@PathVariable Long remittanceId) {
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance == null) {
            return Result.fail("水单不存在");
        }
        return Result.success(remittance);
    }

    /**
     * 获取申报单的水单列表
     */
    @GetMapping("/{id}/remittances")
    @Operation(summary = "获取申报单的水单列表")
    @RequiresPermissions("business:declaration:view")
    public Result<List<DeclarationRemittance>> getRemittancesByFormId(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        List<DeclarationRemittance> list = remittanceService.lambdaQuery()
                .eq(DeclarationRemittance::getFormId, id)
                .orderByAsc(DeclarationRemittance::getRemittanceDate)
                .list();
        return Result.success(list);
    }

    /**
     * 更新水单信息
     */
    @PutMapping("/remittance/{remittanceId}")
    @Operation(summary = "更新水单信息")
    @RequiresPermissions("business:declaration:edit")
    public Result<Void> updateRemittance(
            @PathVariable Long remittanceId,
            @RequestBody DeclarationRemittance remittance) {
        
        DeclarationRemittance existing = remittanceService.getById(remittanceId);
        if (existing == null) {
            return Result.fail("水单不存在");
        }
        
        // 保持原有的formId不变
        remittance.setId(remittanceId);
        remittance.setFormId(existing.getFormId());
        remittanceService.updateById(remittance);
        
        return Result.success();
    }

    /**
     * 删除水单信息
     */
    @DeleteMapping("/remittance/{remittanceId}")
    @Operation(summary = "删除水单信息")
    @RequiresPermissions("business:declaration:delete")
    public Result<Void> deleteRemittance(@PathVariable Long remittanceId) {
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance == null) {
            return Result.fail("水单不存在");
        }
        
        remittanceService.removeById(remittanceId);
        return Result.success();
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审核申报单")
    @RequiresPermissions("business:declaration:audit")
    public Result<String> auditDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody Map<String, Object> auditData) {
        try {
            DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
            if (form == null) {
                return Result.fail("申报单不存在");
            }

            Object resultObj = auditData.get("result"); // 1-通过，2-驳回
            boolean isApproved = false;
            if(resultObj != null && "1".equals(resultObj.toString())) {
                isApproved = true;
            }
                        
            log.info("审核申报单 - id={}, resultObj={}, isApproved={}", id, resultObj, isApproved);
            
            // 支持通过 taskKey 参数精确指定要完成的任务（用于并行流程）
            String taskKey = auditData.get("taskKey") != null ? auditData.get("taskKey").toString() : null;

            List<Task> activeTasks = flowableTaskService.createTaskQuery()
                    .processInstanceBusinessKey(String.valueOf(id))
                    .list();

            if (activeTasks == null || activeTasks.isEmpty()) {
                // 深度诊断：检查流程实例是否存在或已结束
                HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceBusinessKey(String.valueOf(id))
                        .singleResult();
                
                String diagnostic = "找不到待办任务。";
                if (hpi != null) {
                    if (hpi.getEndTime() != null) {
                        diagnostic += " 该流程已于 " + hpi.getEndTime() + " 结束 (EndActivity=" + hpi.getEndActivityId() + ")。";
                    } else {
                        diagnostic += " 流程实例存在 (ID=" + hpi.getId() + ") 但当前节点不是 UserTask。";
                    }
                } else {
                    diagnostic += " 未在系统中找到该业务 Key 的任何流程记录。";
                }
                
                log.warn("业务Key={}审核失败: {}", id, diagnostic);
                return Result.fail(diagnostic);
            }

            Task activeTask = null;
            
            // 如果指定了 taskKey，精确匹配任务
            if (taskKey != null && !taskKey.isEmpty()) {
                for (Task t : activeTasks) {
                    if (taskKey.equals(t.getTaskDefinitionKey())) {
                        activeTask = t;
                        break;
                    }
                }
                if (activeTask == null) {
                    // 列出当前可用的任务供诊断
                    String availableTasks = activeTasks.stream()
                            .map(Task::getTaskDefinitionKey)
                            .collect(Collectors.joining(", "));
                    return Result.fail("未找到指定的任务节点: " + taskKey + "。当前可用任务: [" + availableTasks + "]");
                }
            } else {
                // 智能匹配：优先处理审核类任务（按优先级排序）
                // 优先级: deptAudit > depositAudit > balanceAudit > pickupListAudit > 其他
                String[] priorityOrder = {"deptAudit", "depositAudit", "balanceAudit", "pickupListAudit"};
                for (String key : priorityOrder) {
                    for (Task t : activeTasks) {
                        if (key.equals(t.getTaskDefinitionKey())) {
                            activeTask = t;
                            break;
                        }
                    }
                    if (activeTask != null) break;
                }
                
                // 如果没有匹配优先级任务，选择第一个非财务补充任务
                if (activeTask == null) {
                    for (Task t : activeTasks) {
                        if (!"financeUploadTask".equals(t.getTaskDefinitionKey())) {
                            activeTask = t;
                            break;
                        }
                    }
                }
                
                // 最后兜底：取第一个任务
                if (activeTask == null) {
                    activeTask = activeTasks.get(0);
                }
            }
            
            if (activeTasks.size() > 1) {
                log.info("业务Key={}有{}个并行任务，当前处理的节点为: {}", id, activeTasks.size(), activeTask.getTaskDefinitionKey());
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", isApproved);
            System.out.println("variables: " + variables.get("approved"));
            // 先创建或更新审核历史记录（在流程流转之前记录审核结果）
            try {
                // 查询申报提交时创建的记录（不限制 auditStatus，找最新的）
                BusinessAuditRecord latestRecord = auditRecordDao.selectOne(
                    new LambdaQueryWrapper<BusinessAuditRecord>()
                        .eq(BusinessAuditRecord::getBusinessId, id)
                        // .eq(BusinessAuditRecord::getBusinessType, "DECLARATION_SUBMIT")
                        .orderByDesc(BusinessAuditRecord::getApplyTime)
                        .last("limit 1")
                );
                
                if (latestRecord != null) {
                    // 所有审核节点都更新同一条记录
                    latestRecord.setAuditStatus(isApproved ? 1 : 2); // 1-通过，2-驳回
                    if (StpUtil.isLogin()) {
                        latestRecord.setAuditorId(StpUtil.getLoginIdAsLong());
                    }
                    String remark = auditData.get("remark") != null ? auditData.get("remark").toString() : "";
                    
                    // 根据 taskKey 确定审核类型名称
                    String taskNameMap;
                    if ("deptAudit".equals(taskKey)) {
                        taskNameMap = "初审";
                    } else if ("depositAudit".equals(taskKey)) {
                        taskNameMap = "定金审核";
                    } else if ("balanceAudit".equals(taskKey)) {
                        taskNameMap = "尾款审核";
                    } else if ("pickupListAudit".equals(taskKey)) {
                        taskNameMap = "提货单审核";
                    } else {
                        taskNameMap = taskKey + "审核";
                    }
                    
                    latestRecord.setAuditRemark(remark.isEmpty() ? (taskNameMap + (isApproved ? "通过" : "驳回")) : remark);
                    latestRecord.setAuditTime(LocalDateTime.now());
                    auditRecordDao.updateById(latestRecord);
                    log.info("申报单 {} {}记录已更新：{}", form.getFormNo(), taskNameMap, isApproved ? "通过" : "驳回");
                } else {
                    // 如果没有找到记录，创建新的（兼容情况）
                    BusinessAuditRecord record = new BusinessAuditRecord();
                    record.setBusinessId(id);
                    record.setBusinessType("DECLARATION_SUBMIT");
                    if (StpUtil.isLogin()) {
                        record.setApplicantId(StpUtil.getLoginIdAsLong());
                    }
                    record.setApplyReason(taskKey + "审核");
                    record.setApplyTime(LocalDateTime.now());
                    record.setAuditStatus(isApproved ? 1 : 2);
                    if (StpUtil.isLogin()) {
                        record.setAuditorId(StpUtil.getLoginIdAsLong());
                    }
                    String remark = auditData.get("remark") != null ? auditData.get("remark").toString() : "";
                    record.setAuditRemark(remark.isEmpty() ? (isApproved ? "审核通过" : "审核驳回") : remark);
                    record.setAuditTime(LocalDateTime.now());
                    auditRecordDao.insert(record);
                    log.info("申报单 {} 审核记录创建成功：{}", form.getFormNo(), isApproved ? "通过" : "驳回");
                }
            } catch (Exception e) {
                log.error("申报单 {} 更新审核记录失败", form.getFormNo(), e);
                // 审核记录更新失败不影响主流程，继续执行
            }
            
            // 然后调用 Flowable complete，触发流程流转和状态更新
            flowableTaskService.complete(activeTask.getId(), variables);

            String taskName = activeTask.getName() != null ? activeTask.getName() : activeTask.getTaskDefinitionKey();
            return Result.success("审核成功 (" + taskName + ")");
        } catch (Exception e) {
            log.error("审核申报单失败", e);
            return Result.fail("审核失败内部异常: " + e.getMessage() + " | 原因: " + (e.getCause() != null ? e.getCause().getMessage() : "无"));
        }
    }

    /**
     * 重新生成单据
     */
    @PostMapping("/{id}/regenerate-documents")
    @Operation(summary = "重新生成单据")
    @RequiresPermissions("business:declaration:audit")
    public Result<String> regenerateDocuments(@Parameter(description = "申报单ID") @PathVariable Long id) {
        try {
            DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
            if (form == null) {
                return Result.fail("申报单不存在");
            }
            
            // Service 内部已处理保存/替换逻辑，直接调用即可
            excelExportService.generateAndSaveExportDocuments(form);
            log.info("申报单 {} 标准单证重新生成成功", form.getFormNo());
            
            return Result.success("标准单证重新生成成功");
        } catch (Exception e) {
            log.error("重新生成单据失败", e);
            return Result.fail("单据生成失败: " + e.getMessage());
        }
        
    }

    @PostMapping("/{id}/regenerate-all-documents")
    @Operation(summary = "重新生成全套单据")
    @RequiresPermissions("business:declaration:audit")
    public Result<String> regenerateAllDocuments(@Parameter(description = "申报单ID") @PathVariable Long id) {
        try {
            DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
            if (form == null) {
                return Result.fail("申报单不存在");
            }
            
            // 生成全套单证（包含 standard 和 alltemple 两种模板）
//            excelExportService.generateAndSaveExportDocuments(form);
            excelExportService.generateAndSaveAllTempleExportDocuments(form);
            log.info("申报单 {} 全套单证重新生成成功", form.getFormNo());
            
            return Result.success("全套单证重新生成成功");
        } catch (Exception e) {
            log.error("重新生成全套单据失败", e);
            return Result.fail("全套单据生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/regenerate-remittance-report")
    @Operation(summary = "重新生成水单报告")
    @RequiresPermissions("business:declaration:audit")
    public Result<String> regenerateRemittanceReport(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Parameter(description = "水单类型 1-定金 2-尾款") @RequestParam Integer type) {
        try {
            DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
            if (form == null) {
                return Result.fail("申报单不存在");
            }
            
            // 查找对应类型的水单记录
            LambdaQueryWrapper<DeclarationRemittance> query = new LambdaQueryWrapper<>();
            query.eq(DeclarationRemittance::getFormId, id)
                 .eq(DeclarationRemittance::getRemittanceType, type);
            
            DeclarationRemittance remittance = remittanceService.getOne(query);
            if (remittance == null) {
                return Result.fail("未找到对应的水单记录");
            }
            
            // 重新生成水单报告
            excelExportService.generateAndSaveRemittanceReport(remittance, form);
            String typeName = type == 1 ? "定金" : "尾款";
            log.info("申报单 {} {}水单重新生成成功", form.getFormNo(), typeName);
            
            return Result.success(typeName + "水单重新生成成功");
        } catch (Exception e) {
            log.error("重新生成水单报告失败", e);
            return Result.fail("水单报告生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/submit-audit")
    @Operation(summary = "提交到下一步审核")
    @RequiresPermissions("business:declaration:submit")
    public Result<Void> submitForAudit(
            @Parameter(description = "申报单 ID") @PathVariable Long id,
            @RequestParam String auditType) {
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }
    
        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(String.valueOf(id))
                .list();
                    
        if (activeTasks == null || activeTasks.isEmpty()) {
            return Result.fail("找不到待办的流程任务");
        }
            
        // 根据 auditType 映射到具体的 taskDefinitionKey
        // deposit -> depositPayment (业务员提交定金凭证)
        // balance -> balancePayment (业务员提交尾款凭证)
        // pickup -> pickupListUpload (业务员上传提货单)
        // finance/financeUpload -> financeUploadTask (财务补充完成)
        // rejectHandler -> rejectHandler (驳回修改后重新提交)
        String targetTaskKey = null;
        switch (auditType) {
            case "deposit":
                targetTaskKey = "depositPayment";
                break;
            case "balance":
                targetTaskKey = "balancePayment";
                break;
            case "pickup":
                targetTaskKey = "pickupListUpload";
                break;
            case "finance":
            case "financeUpload":
                targetTaskKey = "financeUploadTask";
                break;
            case "rejectHandler":
                targetTaskKey = "rejectHandler";
                break;
            default:
                // 如果是直接传入 taskKey，也支持
                targetTaskKey = auditType;
        }
            
        Task activeTask = null;
        for (Task t : activeTasks) {
            if (targetTaskKey.equals(t.getTaskDefinitionKey())) {
                activeTask = t;
                break;
            }
        }
    
        if (activeTask == null) {
            String availableTasks = activeTasks.stream()
                    .map(Task::getTaskDefinitionKey)
                    .collect(Collectors.joining(", "));
            return Result.fail("找不到对应类型的流程任务 (auditType=" + auditType + ", targetTaskKey=" + targetTaskKey + ")。当前可用任务：[" + availableTasks + "]");
        }
    
        log.info("申报单 {} 提交任务：{} (auditType={})", id, activeTask.getTaskDefinitionKey(), auditType);
        flowableTaskService.complete(activeTask.getId());
            
        // 创建审核历史记录（在提交申请时）
        try {
            BusinessAuditRecord record = new BusinessAuditRecord();
            record.setBusinessId(id);
                
            // 根据提交的类型设置业务类型
            String businessType;
            String applyReason;
            switch (auditType) {
                case "deposit":
                    businessType = "REMITTANCE_AUDIT";
                    applyReason = "定金水单审核申请";
                    break;
                case "balance":
                    businessType = "REMITTANCE_AUDIT";
                    applyReason = "尾款水单审核申请";
                    break;
                case "pickup":
                    businessType = "DELIVERY_ORDER_AUDIT";
                    applyReason = "提货单审核申请";
                    break;
                default:
                    businessType = "DECLARATION_AUDIT";
                    applyReason = "申报审核申请";
            }
                
            record.setBusinessType(businessType);
            record.setApplicantId(StpUtil.getLoginIdAsLong());
            record.setApplyReason(applyReason);
            record.setApplyTime(LocalDateTime.now());
            record.setPreStatus(form.getStatus());
            declarationFormService.saveAuditRecord(record);
        } catch (Exception e) {
            log.error("创建审核历史记录失败", e);
            // 不影响主流程
        }
            
        return Result.success();
    }

    /**
     * 获取申报单的当前流程任务列表
     * 用于并行流程中查看所有活跃的任务节点
     */
    @GetMapping("/{id}/tasks")
    @Operation(summary = "获取申报单的当前流程任务列表")
    @RequiresPermissions("business:declaration:query")
    public Result<List<Map<String, Object>>> getActiveTasks(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        
        List<Task> activeTasks = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(String.valueOf(id))
                .list();
        
        if (activeTasks == null || activeTasks.isEmpty()) {
            // 检查流程是否已结束
            HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(String.valueOf(id))
                    .singleResult();
            
            if (hpi != null && hpi.getEndTime() != null) {
                // 流程已结束
                List<Map<String, Object>> result = new ArrayList<>();
                Map<String, Object> endInfo = new HashMap<>();
                endInfo.put("status", "completed");
                endInfo.put("endTime", hpi.getEndTime());
                endInfo.put("endActivityId", hpi.getEndActivityId());
                result.add(endInfo);
                return Result.success(result);
            }
            
            return Result.success(new ArrayList<>());
        }
        
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (Task task : activeTasks) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("taskId", task.getId());
            taskInfo.put("taskKey", task.getTaskDefinitionKey());
            taskInfo.put("taskName", task.getName());
            taskInfo.put("assignee", task.getAssignee());
            taskInfo.put("createTime", task.getCreateTime());
            
            // 根据 taskKey 添加任务分类
            String category = getTaskCategory(task.getTaskDefinitionKey());
            taskInfo.put("category", category);
            
            taskList.add(taskInfo);
        }
        
        return Result.success(taskList);
    }
    
    /**
     * 根据任务Key获取任务分类
     */
    private String getTaskCategory(String taskKey) {
        if (taskKey == null) return "unknown";
        switch (taskKey) {
            case "deptAudit":
                return "初审";
            case "rejectHandler":
                return "驳回修改";
            case "depositPayment":
                return "定金上传";
            case "depositAudit":
                return "定金审核";
            case "balancePayment":
                return "尾款上传";
            case "balanceAudit":
                return "尾款审核";
            case "pickupListUpload":
                return "提货单上传";
            case "pickupListAudit":
                return "提货单审核";
            case "financeUploadTask":
                return "财务补充";
            default:
                return taskKey;
        }
    }

    /**
     * 批量获取申报单的活跃任务
     * 用于列表页高效获取多个申报单的当前任务节点
     */
    @GetMapping("/batch-tasks")
    @Operation(summary = "批量获取申报单的活跃任务")
    @RequiresPermissions("business:declaration:query")
    public Result<Map<String, List<String>>> getBatchActiveTasks(
            @Parameter(description = "申报单ID列表，逗号分隔") @RequestParam String ids) {
        
        log.info("批量获取任务请求，ids: {}", ids);
        
        if (ids == null || ids.trim().isEmpty()) {
            log.info("IDs为空，返回空结果");
            return Result.success(new HashMap<>());
        }
        
        try {
            List<String> idList = Arrays.asList(ids.split(","));
            log.info("解析得到 {} 个ID: {}", idList.size(), idList);
            Map<String, List<String>> result = new HashMap<>();
            
            // 优化：先批量查询所有相关的流程实例，建立 processInstanceId -> businessKey 映射
            Map<String, String> processInstanceToBusinessKey = new HashMap<>();
            List<String> processInstanceIds = new ArrayList<>();
            
            for (String businessKey : idList) {
                String trimmedKey = businessKey.trim();
                log.debug("查询流程实例，businessKey: {}", trimmedKey);
                
                // 使用list()而不是singleResult()来处理可能的多个结果
                List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                        .processInstanceBusinessKey(trimmedKey)
                        .list();
                
                if (!instances.isEmpty()) {
                    // 如果有多个实例，取最新的一个（按开始时间排序）
                    ProcessInstance latestInstance = instances.stream()
                            .max(Comparator.comparing(ProcessInstance::getStartTime))
                            .orElse(instances.get(0));
                    
                    processInstanceIds.add(latestInstance.getId());
                    processInstanceToBusinessKey.put(latestInstance.getId(), trimmedKey);
                    log.debug("找到流程实例: {} -> {} (共{}个实例，取最新)", 
                             latestInstance.getId(), trimmedKey, instances.size());
                } else {
                    log.debug("未找到流程实例，businessKey: {}", trimmedKey);
                }
            }
            
            log.info("找到 {} 个流程实例", processInstanceIds.size());
            
            if (processInstanceIds.isEmpty()) {
                log.info("没有找到任何流程实例，返回空结果");
                return Result.success(result);
            }
            
            // 批量查询所有相关的活跃任务
            log.debug("查询活跃任务，processInstanceIds: {}", processInstanceIds);
            List<Task> tasks = flowableTaskService.createTaskQuery()
                    .processInstanceIdIn(processInstanceIds)
                    .list();
            
            log.info("查询到 {} 个活跃任务", tasks.size());
            
            // 按 businessKey 分组，提取 taskDefinitionKey
            for (Task task : tasks) {
                String businessKey = processInstanceToBusinessKey.get(task.getProcessInstanceId());
                if (businessKey != null) {
                    result.computeIfAbsent(businessKey, k -> new ArrayList<>())
                            .add(task.getTaskDefinitionKey());
                    log.debug("任务映射: {} -> {}", businessKey, task.getTaskDefinitionKey());
                }
            }
            
            log.info("最终结果: {}", result);
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("批量获取任务失败，ids: {}", ids, e);
            return Result.fail("获取任务失败: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "分页查询申报单")
    @RequiresPermissions("business:declaration:list")
    public Result<IPage<DeclarationForm>> getDeclarations(
            @Parameter(description = "分页参数") PageParam pageParam,
            @Parameter(description = "申报单号") @RequestParam(required = false) String formNo,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<DeclarationForm> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();

            // 查询正式/草稿混合表
            LambdaQueryWrapper<DeclarationForm> queryWrapper = new LambdaQueryWrapper<>();
            if (formNo != null && !formNo.isEmpty()) {
                queryWrapper.like(DeclarationForm::getFormNo, formNo);
            }
            if (status != null) {
                queryWrapper.eq(DeclarationForm::getStatus, status);
            }
            
            // 组织级数据权限隔离：有审核权限可查看所有数据
            boolean hasApprovePermission = StpUtil.hasPermission("business:declaration:audit");
            System.out.println(hasApprovePermission);
            System.out.println(userId);
            // 管理员(userId=1)或有审批权限的用户可以查看所有数据
            if (userId != 1L && !hasApprovePermission) {
                // 普通用户只能查看自己创建的或本组织的数据
                User currentUser = userService.getById(userId);
                if (currentUser != null && currentUser.getOrgId() != null) {
                    // 查看自己创建的 或 本组织的数据
                    queryWrapper.and(w -> w.eq(DeclarationForm::getCreateBy, userId)
                        .or().eq(DeclarationForm::getOrgId, currentUser.getOrgId()));
                } else {
                    // 用户没有组织，只能看自己创建的
                    queryWrapper.eq(DeclarationForm::getCreateBy, userId);
                }
            }
            
            queryWrapper.orderByDesc(DeclarationForm::getCreateTime);
            
            IPage<DeclarationForm> result = declarationFormService.page(page, queryWrapper);
            
            // 批量填充申请人名称
            fillApplicantNames(result.getRecords());
            // 批量填充财务补充状态
            fillFinanceUploadStatus(result.getRecords());
            
            return Result.success(result);
        }

        IPage<DeclarationForm> result = declarationFormService.page(page);
        fillApplicantNames(result.getRecords());
        fillFinanceUploadStatus(result.getRecords());
        return Result.success(result);
    }
    
    /**
     * 批量填充申请人名称
     */
    private void fillApplicantNames(List<DeclarationForm> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 收集所有 createBy ID
        Set<Long> userIds = records.stream()
            .map(DeclarationForm::getCreateBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        if (userIds.isEmpty()) {
            return;
        }
        
        // 批量查询用户
        List<User> users = userService.listByIds(userIds);
        Map<Long, String> userNameMap = users.stream()
            .collect(Collectors.toMap(
                User::getId,
                u -> u.getNickname() != null ? u.getNickname() : u.getUsername()
            ));
        
        // 填充 applicantName
        records.forEach(form -> {
            if (form.getCreateBy() != null) {
                form.setApplicantName(userNameMap.getOrDefault(form.getCreateBy(), "未知用户"));
            }
        });
    }

    /**
     * 批量填充财务补充审核状态
     */
    private void fillFinanceUploadStatus(List<DeclarationForm> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (DeclarationForm form : records) {
            // 如果已在正式表中且状态不为结束（简化判断：或者直接查询）
            long count = flowableTaskService.createTaskQuery()
                    .processInstanceBusinessKey(String.valueOf(form.getId()))
                    .taskDefinitionKey("financeUploadTask")
                    .count();
            form.setFinanceUploadPending(count > 0);
        }
    }

    @PostMapping("/draft")
    @Operation(summary = "保存草稿")
    @RequiresPermissions("business:declaration:add")
    public Result<JSONObject> saveDraft(@RequestBody DeclarationForm form) {
        try {
            Long orgId = OrganizationUtils.getCurrentUserOrgId();
            if (form.getOrgId() == null) {
                form.setOrgId(orgId);
            }
            form.setStatus(0); // 确保是草稿状态
            
            if (form.getId() != null) {
                declarationFormService.updateDeclarationForm(form);
            } else {
                declarationFormService.saveDeclarationForm(form);
            }
            JSONObject json = new JSONObject();
            json.put("formId",form.getId());
            json.put("formNo",form.getFormNo());
            return Result.success(json);
        } catch (Exception e) {
            log.error("保存草稿失败", e);
            return Result.fail("保存草稿失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取申报单详情")
    @RequiresPermissions("business:declaration:query")
    public Result<DeclarationForm> getDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
        
        // 查询单一记录时，也填充财务状态
        if (form != null && form.getId() != null) {
            long count = flowableTaskService.createTaskQuery()
                    .processInstanceBusinessKey(String.valueOf(form.getId()))
                    .taskDefinitionKey("financeUploadTask")
                    .count();
            form.setFinanceUploadPending(count > 0);
        }
        
        return Result.success(form);
    }

    /**
     * 创建申报单
     */
    @PostMapping
    @SaIgnore
    @Operation(summary = "新增申报单")
    @RequiresPermissions("business:declaration:add")
    public Result<Long> createDeclaration(@Valid @RequestBody DeclarationForm form) {
        // 生成申报单号
        if (form.getFormNo() == null || form.getFormNo().isEmpty()) {
            form.setFormNo(generateFormNo());
        }
        
        // 如果前端未提供发票号，则按规则生成
        if (form.getInvoiceNo() == null || form.getInvoiceNo().isEmpty()) {
            // 生成规则：ZIYI-yy-mmdd
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yy-MMdd"));
            form.setInvoiceNo("ZIYI-" + currentDate);
        }
        Long orgId = OrganizationUtils.getCurrentUserOrgId();
        if (form.getOrgId() == null) {
            form.setOrgId(orgId);
        }
        declarationFormService.saveDeclarationForm(form);
        return Result.success(form.getId());
    }

    /**
     * 更新申报单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新申报单")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> updateDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Valid @RequestBody DeclarationForm form) {
        // 验证申报单ID与路径参数的一致性
        form.setId(id);
        
        // 设置组织ID，确保不会被从前端修改
        Long currentOrgId = OrganizationUtils.getCurrentUserOrgId();
        if (currentOrgId != null) {
            form.setOrgId(currentOrgId);
        }
        
        declarationFormService.updateDeclarationForm(form);
        return Result.success();
    }

    /**
     * 删除申报单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除申报单")
    @RequiresPermissions("business:declaration:delete")
    public Result<Void> deleteDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.success();
        }
        
        // 只有草稿状态才能被删除
        if (form.getStatus() != 0) {
            return Result.fail("只有草稿状态的申报单才允许删除");
        }
        
        declarationFormService.removeById(id);
        return Result.success();
    }

    /**
     * 提交申报单
     */
    @PostMapping("/{id}/submit")
    @SaIgnore
    @Operation(summary = "提交申报单")
    @RequiresPermissions("business:declaration:update")
    public Result<String> submitDeclaration(@Parameter(description = "申报单ID") @PathVariable Long id) {
        log.info("调用提交申报单");
        DeclarationForm form = declarationFormService.getById(id);
        if (form != null) {
            if (form.getStatus() != 0) {
                return Result.fail("当前状态不支持提交操作");
            }
            
            // 验证组织权限，确保用户只能提交自己组织的申报单
            Long currentOrgId = OrganizationUtils.getCurrentUserOrgId();
            if (form.getOrgId() != null && currentOrgId != null && !form.getOrgId().equals(currentOrgId)) {
                log.warn("用户尝试提交不属于其组织的申报单: {}", id);
                return Result.fail("没有权限提交此申报单");
            }
            
            // 如果组织ID为空，则设置为当前用户组织ID
            if (form.getOrgId() == null && currentOrgId != null) {
                form.setOrgId(currentOrgId);
            }
            
            form.setStatus(1); // 已提交状态 - 待初审
            declarationFormService.updateById(form);
            
            // 创建审核历史记录（申报提交 - 待审核状态）
            try {
                BusinessAuditRecord record = new BusinessAuditRecord();
                record.setBusinessId(id);
                record.setBusinessType("DECLARATION_SUBMIT");
                if (StpUtil.isLogin()) {
                    record.setApplicantId(StpUtil.getLoginIdAsLong());
                }
                record.setApplyReason("申报提交");
                record.setApplyTime(LocalDateTime.now());
                record.setAuditStatus(0); // 0-待审核
                record.setPreStatus(0); // 从草稿状态提交
                // auditorId、auditRemark、auditTime 在审核时填充
                
                auditRecordDao.insert(record);
                log.info("申报单 {} 审核记录创建成功（待审核），recordId={}", form.getFormNo(), record.getId());
            } catch (Exception e) {
                log.error("申报单 {} 创建审核记录失败", form.getFormNo(), e);
                // 审核记录创建失败不影响主流程，继续执行
            }
            
            // 在提交时生成Excel文件（Service 内部已处理保存/替换逻辑）
            try {
                DeclarationForm fullForm = declarationFormService.getFullDeclarationForm(id);
                if (fullForm != null) {
                    excelExportService.generateAndSaveExportDocuments(fullForm);
                    log.info("申报单 {} 提交时自动生成全套单证成功", fullForm.getFormNo());
                }
            } catch (Exception e) {
                log.error("申报单 {} 提交时自动生成导出文件失败", form.getFormNo(), e);
                // 即使生成失败也不影响提交流程
            }
            
            // 启动 Flowable 流程
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("starterId", form.getCreateBy());
                variables.put("orgId", form.getOrgId());
                variables.put("formNo", form.getFormNo());
                
                log.info("准备启动流程：key={}, businessKey={}, variables={}", "declarationProcess", String.valueOf(id), variables);
                
                com.declaration.entity.ProcessInstance processInstance = processInstanceService.startProcessInstance("declarationProcess", String.valueOf(id), variables);
                
                log.info("申报单 {} 流程启动成功，instanceId={}", form.getFormNo(), processInstance.getInstanceId());
            } catch (Exception e) {
                log.error("申报单 {} 流程启动失败，formNo={}, createBy={}, orgId={}", form.getFormNo(), form.getFormNo(), form.getCreateBy(), form.getOrgId(), e);
                
                // 流程启动失败时进行手动状态补偿回滚，防止该单据卡死在状态 1 (无对应流程任务)
                form.setStatus(0);
                declarationFormService.updateById(form);
                
                return Result.fail("流水线引擎启动流程失败，错误详情：" + e.getMessage() + " (单据已自动回退到草稿状态)");
            }
        }
        return Result.success("提交成功");
    }

    /**
     * 获取申报单产品列表
     */
    @GetMapping("/{formId}/products")
    @Operation(summary = "获取申报单产品列表")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationProduct>> getProducts(@Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<DeclarationProduct> products = declarationProductService.lambdaQuery()
                .eq(DeclarationProduct::getFormId, formId)
                .orderByAsc(DeclarationProduct::getSortOrder)
                .list();
        return Result.success(products);
    }

    /**
     * 保存申报单产品
     */
    @PostMapping("/{formId}/products")
    @Operation(summary = "保存申报单产品")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveProducts(
            @Parameter(description = "申报单ID") @PathVariable Long formId,
            @Valid @RequestBody List<DeclarationProduct> products) {
        
        // 先删除原有产品
        declarationProductService.lambdaUpdate()
                .eq(DeclarationProduct::getFormId, formId)
                .remove();
        
        // 保存新产品
        if (products != null && !products.isEmpty()) {
            for (int i = 0; i < products.size(); i++) {
                DeclarationProduct product = products.get(i);
                // 如果是新填产品，清除临时ID由数据库生成真实ID
                if (product.getId() != null && product.getId() < 100000) {
                    product.setId(null);
                }
                product.setFormId(formId);
                product.setSortOrder(i);
                declarationProductService.save(product);
            }
        }
        
        return Result.success();
    }

    /**
     * 获取申报单箱子列表
     */
    @GetMapping("/{formId}/cartons")
    @Operation(summary = "获取申报单箱子列表")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationCarton>> getCartons(@Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<DeclarationCarton> cartons = declarationCartonService.lambdaQuery()
                .eq(DeclarationCarton::getFormId, formId)
                .orderByAsc(DeclarationCarton::getSortOrder)
                .list();
        return Result.success(cartons);
    }

    /**
     * 保存申报单箱子
     */
    @PostMapping("/{formId}/cartons")
    @Operation(summary = "保存申报单箱子")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveCartons(
            @Parameter(description = "申报单ID") @PathVariable Long formId,
            @Valid @RequestBody List<DeclarationCarton> cartons) {
        
        // 先删除原有箱子
        declarationCartonService.lambdaUpdate()
                .eq(DeclarationCarton::getFormId, formId)
                .remove();
        
        // 保存新箱子
        if (cartons != null && !cartons.isEmpty()) {
            for (int i = 0; i < cartons.size(); i++) {
                DeclarationCarton carton = cartons.get(i);
                // 如果是新加箱子，清除临时ID
                if (carton.getId() != null && carton.getId() < 100000) {
                    carton.setId(null);
                }
                carton.setFormId(formId);
                carton.setSortOrder(i);
                declarationCartonService.save(carton);
            }
        }
        
        return Result.success();
    }

    /**
     * 获取箱子产品关联
     */
    @GetMapping("/carton-products/{cartonId}")
    @Operation(summary = "获取箱子产品关联")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationCartonProduct>> getCartonProducts(
            @Parameter(description = "箱子ID") @PathVariable Long cartonId) {
        List<DeclarationCartonProduct> cartonProducts = declarationCartonProductService.lambdaQuery()
                .eq(DeclarationCartonProduct::getCartonId, cartonId)
                .list();
        return Result.success(cartonProducts);
    }

    /**
     * 保存箱子产品关联
     */
    @PostMapping("/carton-products")
    @Operation(summary = "保存箱子产品关联")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveCartonProducts(@Valid @RequestBody List<DeclarationCartonProduct> cartonProducts) {
        
        // 先删除原有关联
        if (cartonProducts != null && !cartonProducts.isEmpty()) {
            Long cartonId = cartonProducts.get(0).getCartonId();
            declarationCartonProductService.lambdaUpdate()
                    .eq(DeclarationCartonProduct::getCartonId, cartonId)
                    .remove();
        }
        
        // 保存新关联
        if (cartonProducts != null && !cartonProducts.isEmpty()) {
            for (DeclarationCartonProduct cartonProduct : cartonProducts) {
                // 清除关联记录自身的临时ID
                if (cartonProduct.getId() != null && cartonProduct.getId() < 100000) {
                    cartonProduct.setId(null);
                }
                declarationCartonProductService.save(cartonProduct);
            }
        }
        
        return Result.success();
    }

    /**
     * 导出申报单
     */
    @GetMapping("/{id}/export")
    @Operation(summary = "导出申报单")
    @RequiresPermissions("business:declaration:export")
    public void exportDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            HttpServletResponse response) {
        try {
            DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
            if (form == null) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"msg\":\"申报单不存在\"}");
                return;
            }
            
            // 设置响应头
            String fileName = "申报单_" + form.getFormNo() + "_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));
            response.setCharacterEncoding("UTF-8");
            
            // 使用 POI 创建 Excel
            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.createSheet("申报单详情");
            
            // 创建标题样式
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.xssf.usermodel.XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            
            int rowNum = 0;
            
            // 基本信息标题
            org.apache.poi.xssf.usermodel.XSSFRow titleRow = sheet.createRow(rowNum++);
            org.apache.poi.xssf.usermodel.XSSFCell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("申报单详情 - " + form.getFormNo());
            titleCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));
            
            rowNum++; // 空行
            
            // 基本信息
            createInfoRow(sheet, rowNum++, "申报单号", form.getFormNo(), headerStyle);
            createInfoRow(sheet, rowNum++, "发票号", form.getInvoiceNo(), headerStyle);
            createInfoRow(sheet, rowNum++, "申报日期", form.getDeclarationDate() != null ? form.getDeclarationDate().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "发货人公司", form.getShipperCompany(), headerStyle);
            createInfoRow(sheet, rowNum++, "发货人地址", form.getShipperAddress(), headerStyle);
            createInfoRow(sheet, rowNum++, "收货人公司", form.getConsigneeCompany(), headerStyle);
            createInfoRow(sheet, rowNum++, "收货人地址", form.getConsigneeAddress(), headerStyle);
            createInfoRow(sheet, rowNum++, "运输方式", form.getTransportMode(), headerStyle);
            createInfoRow(sheet, rowNum++, "出发城市", form.getDepartureCity(), headerStyle);
            createInfoRow(sheet, rowNum++, "目的国", form.getDestinationCountry(), headerStyle);
            createInfoRow(sheet, rowNum++, "币种", form.getCurrency(), headerStyle);
            createInfoRow(sheet, rowNum++, "总数量", form.getTotalQuantity() != null ? form.getTotalQuantity().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "总金额", form.getTotalAmount() != null ? form.getTotalAmount().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "总箱数", form.getTotalCartons() != null ? form.getTotalCartons().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "总毛重(KGS)", form.getTotalGrossWeight() != null ? form.getTotalGrossWeight().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "总净重(KGS)", form.getTotalNetWeight() != null ? form.getTotalNetWeight().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "总体积(CBM)", form.getTotalVolume() != null ? form.getTotalVolume().toString() : "", headerStyle);
            createInfoRow(sheet, rowNum++, "状态", getStatusText(form.getStatus()), headerStyle);
            
            // 产品明细
            if (form.getProducts() != null && !form.getProducts().isEmpty()) {
                rowNum++;
                org.apache.poi.xssf.usermodel.XSSFRow productTitleRow = sheet.createRow(rowNum++);
                productTitleRow.createCell(0).setCellValue("产品明细");
                productTitleRow.getCell(0).setCellStyle(headerStyle);
                
                // 产品表头
                org.apache.poi.xssf.usermodel.XSSFRow productHeaderRow = sheet.createRow(rowNum++);
                String[] productHeaders = {"序号", "产品名称", "HS编码", "数量", "单位", "单价", "金额", "毛重", "净重"};
                for (int i = 0; i < productHeaders.length; i++) {
                    org.apache.poi.xssf.usermodel.XSSFCell cell = productHeaderRow.createCell(i);
                    cell.setCellValue(productHeaders[i]);
                    cell.setCellStyle(headerStyle);
                }
                
                // 产品数据
                int productNo = 1;
                for (DeclarationProduct product : form.getProducts()) {
                    org.apache.poi.xssf.usermodel.XSSFRow productRow = sheet.createRow(rowNum++);
                    productRow.createCell(0).setCellValue(productNo++);
                    productRow.createCell(1).setCellValue(product.getProductName() != null ? product.getProductName() : "");
                    productRow.createCell(2).setCellValue(product.getHsCode() != null ? product.getHsCode() : "");
                    productRow.createCell(3).setCellValue(product.getQuantity() != null ? product.getQuantity() : 0);
                    productRow.createCell(4).setCellValue(product.getUnit() != null ? product.getUnit() : "");
                    productRow.createCell(5).setCellValue(product.getUnitPrice() != null ? product.getUnitPrice().doubleValue() : 0);
                    productRow.createCell(6).setCellValue(product.getAmount() != null ? product.getAmount().doubleValue() : 0);
                    productRow.createCell(7).setCellValue(product.getGrossWeight() != null ? product.getGrossWeight().doubleValue() : 0);
                    productRow.createCell(8).setCellValue(product.getNetWeight() != null ? product.getNetWeight().doubleValue() : 0);
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < 9; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 写入响应流
            workbook.write(response.getOutputStream());
            workbook.close();
            
        } catch (Exception e) {
            log.error("导出申报单失败", e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"导出失败: " + e.getMessage() + "\"}");
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * 创建信息行
     */
    private void createInfoRow(org.apache.poi.xssf.usermodel.XSSFSheet sheet, int rowNum, String label, String value, 
            org.apache.poi.xssf.usermodel.XSSFCellStyle headerStyle) {
        org.apache.poi.xssf.usermodel.XSSFRow row = sheet.createRow(rowNum);
        org.apache.poi.xssf.usermodel.XSSFCell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(headerStyle);
        row.createCell(1).setCellValue(value != null ? value : "");
    }
    
    /**
     * 获取状态文本
     * 状态定义（任务驱动流程）：
     * 0 - 草稿
     * 1 - 待初审
     * 2 - 处理中（并行任务进行中，具体进度由活跃任务决定）
     * 8 - 已完成
     * 注：状态3/4/5已移除，并行阶段主状态始终为2
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "待初审";
            case 2: return "处理中";
            case 8: return "已完成";
            default: return "状态" + status;
        }
    }

    // ================== 提货单与财务附件管理 ==================
    
    /**
     * 获取提货单附件列表
     */
    @GetMapping("/{id}/attachments/pickup")
    @Operation(summary = "获取提货单附件列表")
    @RequiresPermissions("business:declaration:query")
    public Result<List<DeclarationAttachment>> getPickupAttachments(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        List<DeclarationAttachment> attachments = attachmentService.lambdaQuery()
                .eq(DeclarationAttachment::getFormId, id)
                .eq(DeclarationAttachment::getFileType, "PickupList")
                .orderByDesc(DeclarationAttachment::getCreateTime)
                .list();
        return Result.success(attachments);
    }

    /**
     * 保存提货单附件
     */
    @PostMapping("/{id}/attachments/pickup")
    @Operation(summary = "保存提货单附件")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> savePickupAttachment(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody DeclarationAttachment attachment) {
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }
        
        // 验证必要字段
        if (attachment.getFileName() == null || attachment.getFileName().trim().isEmpty()) {
            return Result.fail("文件名不能为空");
        }
        if (attachment.getFileUrl() == null || attachment.getFileUrl().trim().isEmpty()) {
            return Result.fail("文件URL不能为空");
        }
        
        attachment.setFormId(id);
        attachment.setFileType("PickupList");
        attachment.setCreateTime(LocalDateTime.now());
        attachmentService.save(attachment);
        
        return Result.success();
    }

    /**
     * 删除提货单附件
     */
    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "删除申报单附件")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> deleteAttachment(
            @Parameter(description = "附件ID") @PathVariable Long attachmentId) {
        DeclarationAttachment attachment = attachmentService.getById(attachmentId);
        if (attachment == null) {
            return Result.fail("附件不存在");
        }
        
        attachmentService.removeById(attachmentId);
        return Result.success();
    }

    // ================== 提货单管理 API ==================

    /**
     * 提交提货单
     */
    @PostMapping("/{id}/delivery-order")
    @Operation(summary = "提交提货单")
    @RequiresPermissions("business:declaration:deliveryOrder:add")
    public Result<Long> saveDeliveryOrder(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody DeliveryOrder deliveryOrder) {
        
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }
        
        deliveryOrder.setFormId(id);
        boolean saved = declarationFormService.saveDeliveryOrder(deliveryOrder);
        if (saved) {
            return Result.success(deliveryOrder.getId());
        }
        return Result.fail("保存提货单失败");
    }

    /**
     * 获取提货单列表
     */
    @GetMapping("/{id}/delivery-orders")
    @Operation(summary = "获取提货单列表")
    @RequiresPermissions("business:declaration:deliveryOrder")
    public Result<List<DeliveryOrder>> getDeliveryOrders(
            @Parameter(description = "申报单ID") @PathVariable Long id) {
        
        List<DeliveryOrder> deliveryOrders = declarationFormService.getDeliveryOrdersByFormId(id);
        return Result.success(deliveryOrders);
    }

    /**
     * 更新提货单
     */
    @PutMapping("/delivery-order/{deliveryOrderId}")
    @Operation(summary = "更新提货单")
    @RequiresPermissions("business:declaration:deliveryOrder:edit")
    public Result<Void> updateDeliveryOrder(
            @Parameter(description = "提货单ID") @PathVariable Long deliveryOrderId,
            @RequestBody DeliveryOrder deliveryOrder) {
        
        DeliveryOrder existing = declarationFormService.getDeliveryOrderById(deliveryOrderId);
        if (existing == null) {
            return Result.fail("提货单不存在");
        }
        
        // 保持原有的formId和createdBy不变
        deliveryOrder.setId(deliveryOrderId);
        deliveryOrder.setFormId(existing.getFormId());
        deliveryOrder.setCreatedBy(existing.getCreatedBy());
        deliveryOrder.setCreatedAt(existing.getCreatedAt());
        
        boolean updated = declarationFormService.updateDeliveryOrder(deliveryOrder);
        if (updated) {
            return Result.success();
        }
        return Result.fail("更新提货单失败");
    }

    /**
     * 删除提货单
     */
    @DeleteMapping("/delivery-order/{deliveryOrderId}")
    @Operation(summary = "删除提货单")
    @RequiresPermissions("business:declaration:deliveryOrder:delete")
    public Result<Void> deleteDeliveryOrder(
            @Parameter(description = "提货单ID") @PathVariable Long deliveryOrderId) {
        
        DeliveryOrder existing = declarationFormService.getDeliveryOrderById(deliveryOrderId);
        if (existing == null) {
            return Result.fail("提货单不存在");
        }
        
        // 只有待审核状态才能删除
        if (existing.getStatus() != null && existing.getStatus() != 0) {
            return Result.fail("只有待审核的提货单才能删除");
        }
        
        boolean deleted = declarationFormService.deleteDeliveryOrder(deliveryOrderId);
        if (deleted) {
            return Result.success();
        }
        return Result.fail("删除提货单失败");
    }

    // ================== 财务审核 API ==================

    /**
     * 审核水单（定金/尾款）
     */
    @PostMapping("/remittance/{remittanceId}/audit")
    @Operation(summary = "审核水单")
    @RequiresPermissions("business:declaration:audit")
    public Result<Void> auditRemittance(
            @Parameter(description = "水单ID") @PathVariable Long remittanceId,
            @RequestBody Map<String, Object> params) {
        
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance == null) {
            return Result.fail("水单不存在");
        }
        
        Object approvedObj = params.get("approved");
        boolean approved = approvedObj != null && (Boolean.TRUE.equals(approvedObj) || "true".equals(approvedObj.toString()));
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";
        
        boolean result = declarationFormService.auditRemittance(remittanceId, approved, remark);
        if (result) {
            return Result.success();
        }
        return Result.fail("审核水单失败");
    }

    /**
     * 审核提货单
     */
    @PostMapping("/delivery-order/{deliveryOrderId}/audit")
    @Operation(summary = "审核提货单")
    @RequiresPermissions("business:declaration:deliveryOrder:audit")
    public Result<Void> auditDeliveryOrder(
            @Parameter(description = "提货单ID") @PathVariable Long deliveryOrderId,
            @RequestBody Map<String, Object> params) {
        
        DeliveryOrder deliveryOrder = declarationFormService.getDeliveryOrderById(deliveryOrderId);
        if (deliveryOrder == null) {
            return Result.fail("提货单不存在");
        }
        
        Object approvedObj = params.get("approved");
        boolean approved = approvedObj != null && (Boolean.TRUE.equals(approvedObj) || "true".equals(approvedObj.toString()));
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";
        
        boolean result = declarationFormService.auditDeliveryOrder(deliveryOrderId, approved, remark);
        if (result) {
            return Result.success();
        }
        return Result.fail("审核提货单失败");
    }
    /**
     * 申请退回草稿
     */
    @PostMapping("/{id}/apply-return")
    @Operation(summary = "申请退回草稿")
    @RequiresPermissions("business:declaration:return-apply")
    public Result<Void> applyReturnToDraft(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        
        String reason = params.get("reason");
        boolean result = declarationFormService.applyReturnToDraft(id, reason);
        if (result) {
            return Result.success();
        }
        return Result.fail("申请失败");
    }

    /**
     * 审核退回草稿申请
     */
    @PostMapping("/{id}/audit-return")
    @Operation(summary = "审核退回草稿申请")
    @RequiresPermissions("business:declaration:return-audit")
    public Result<Void> auditReturnToDraft(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        
        Object approvedObj = params.get("approved");
        boolean approved = approvedObj != null && (Boolean.TRUE.equals(approvedObj) || "true".equals(approvedObj.toString()));
        String remark = params.get("remark") != null ? params.get("remark").toString() : "";
        
        boolean result = declarationFormService.auditReturnToDraft(id, approved, remark);
        if (result) {
            return Result.success();
        }
        return Result.fail("操作失败");
    }

    /**
     * 获取退回申请审核历史
     */
    @GetMapping("/{id}/return-history")
    @Operation(summary = "获取退回申请审核历史")
    @RequiresPermissions("business:declaration:query")
    public Result<List<AuditHistoryDTO>> getReturnAuditHistory(
            @Parameter(description = "申报单 ID") @PathVariable Long id) {
            
        List<AuditHistoryDTO> history = declarationFormService.getReturnAuditHistory(id);
        return Result.success(history);
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * 生成申报单号
     */
    private String generateFormNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        return "DEC" + datePrefix + randomSuffix;
    }
}