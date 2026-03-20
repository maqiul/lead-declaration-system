package com.declaration.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.*;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final DeclarationProductService declarationProductService;
    private final DeclarationCartonService declarationCartonService;
    private final DeclarationCartonProductService declarationCartonProductService;
    private final ExcelExportService excelExportService;
    private final DeclarationAttachmentService attachmentService;
    private final DeclarationRemittanceService remittanceService;
    private final ProcessInstanceService processInstanceService;
    private final DeclarationDraftService declarationDraftService;
    private final TaskService flowableTaskService;
    private final ObjectMapper objectMapper;
    private final UserService userService;

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
    public Result<Void> saveRemittance(
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
        
        return Result.success();
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
    public Result<DeclarationRemittance> getRemittance(@PathVariable Long remittanceId) {
        DeclarationRemittance remittance = remittanceService.getById(remittanceId);
        if (remittance == null) {
            return Result.fail("水单不存在");
        }
        return Result.success(remittance);
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

            Object resultObj = auditData.get("result"); // 1-通过, 2-驳回
            boolean isApproved = resultObj != null && ("1".equals(resultObj.toString()) || Boolean.TRUE.equals(resultObj));

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

            Task activeTask = activeTasks.get(0);
            if (activeTasks.size() > 1) {
                log.warn("业务Key={}有多个任务，默认处理: {}", id, activeTask.getId());
            }

            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", isApproved);
            flowableTaskService.complete(activeTask.getId(), variables);

            return Result.success("审核成功");
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
            log.info("申报单 {} 单据重新生成成功", form.getFormNo());
            
            return Result.success("单据重新生成成功");
        } catch (Exception e) {
            log.error("重新生成单据失败", e);
            return Result.fail("单据生成失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/submit-audit")
    @Operation(summary = "提交到下一步审核")
    @RequiresPermissions("business:declaration:submit")
    public Result<Void> submitForAudit(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestParam String auditType) {
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }

        Task activeTask = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(String.valueOf(id))
                .singleResult();

        if (activeTask == null) {
            return Result.fail("找不到待办的流程任务");
        }

        flowableTaskService.complete(activeTask.getId());
        return Result.success();
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

            // 如果明确查草稿 (status == 0)
            if (status != null && status == 0) {
                LambdaQueryWrapper<DeclarationDraft> draftWrapper = new LambdaQueryWrapper<>();
                draftWrapper.eq(DeclarationDraft::getUserId, userId);
                if (formNo != null && !formNo.isEmpty()) {
                    draftWrapper.like(DeclarationDraft::getFormNo, formNo);
                }
                draftWrapper.orderByDesc(DeclarationDraft::getUpdateTime);
                
                Page<DeclarationDraft> draftPage = new Page<>(pageParam.getCurrent(), pageParam.getSize());
                IPage<DeclarationDraft> draftResult = declarationDraftService.page(draftPage, draftWrapper);
                
                // 转换为 DeclarationForm 以适配前端
                IPage<DeclarationForm> convertedResult = draftResult.convert(draft -> {
                    DeclarationForm f = new DeclarationForm();
                    f.setId(draft.getId());
                    f.setFormNo(draft.getFormNo());
                    f.setShipperCompany(draft.getShipperCompany());
                    f.setConsigneeCompany(draft.getConsigneeCompany());
                    f.setTotalAmount(draft.getTotalAmount());
                    f.setStatus(0);
                    f.setCreateTime(draft.getCreateTime());
                    f.setUpdateTime(draft.getUpdateTime());
                    f.setCreateBy(draft.getUserId());
                    return f;
                });
                return Result.success(convertedResult);
            }

            // 查询正式表
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
            
            return Result.success(result);
        }

        IPage<DeclarationForm> result = declarationFormService.page(page);
        fillApplicantNames(result.getRecords());
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

    @PostMapping("/draft")
    @Operation(summary = "保存草稿")
    @RequiresPermissions("business:declaration:add")
    public Result<Long> saveDraft(@RequestBody DeclarationForm form) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            Long orgId = OrganizationUtils.getCurrentUserOrgId();

            DeclarationDraft draft = new DeclarationDraft();
            // 如果已存在草稿ID (编辑现有草稿)
            if (form.getId() != null && form.getStatus() != null && form.getStatus() == 0) {
                draft.setId(form.getId());
            }
            
            draft.setUserId(userId);
            draft.setOrgId(orgId);
            draft.setFormNo(form.getFormNo());
            draft.setShipperCompany(form.getShipperCompany());
            draft.setConsigneeCompany(form.getConsigneeCompany());
            draft.setTotalAmount(form.getTotalAmount());
            draft.setFormData(objectMapper.writeValueAsString(form));
            
            declarationDraftService.saveOrUpdate(draft);
            return Result.success(draft.getId());
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
        
        // 如果明确是查草稿，或者在正式表查不到且已知是草稿
        if (status != null && status == 0) {
            DeclarationDraft draft = declarationDraftService.getById(id);
            if (draft != null) {
                try {
                    DeclarationForm form = objectMapper.readValue(draft.getFormData(), DeclarationForm.class);
                    form.setId(draft.getId());
                    form.setStatus(0);
                    return Result.success(form);
                } catch (Exception e) {
                    log.error("解析草稿JSON失败", e);
                }
            }
        }

        DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
        // 如果正式表查不到，尝试查下草稿表 (容错处理)
        if (form == null) {
            DeclarationDraft draft = declarationDraftService.getById(id);
            if (draft != null) {
                try {
                    form = objectMapper.readValue(draft.getFormData(), DeclarationForm.class);
                    form.setId(draft.getId());
                    form.setStatus(0);
                    return Result.success(form);
                } catch (Exception e) {
                    log.error("解析草稿JSON失败", e);
                }
            }
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
        form.setId(id);
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
        
        // 如果是删除草稿
        if (status != null && status == 0) {
            declarationDraftService.removeById(id);
            return Result.success();
        }

        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            // 尝试从草稿表删除 (容错)
            declarationDraftService.removeById(id);
            return Result.success();
        }
        
        // 只有草稿状态才能被删除 (正式表中的草稿)
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
            
            form.setStatus(1); // 已提交状态
            declarationFormService.updateById(form);
            
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
                
                processInstanceService.startProcessInstance("declarationProcess", String.valueOf(id), variables);
                log.info("申报单 {} 流程启动成功", form.getFormNo());
            } catch (Exception e) {
                log.error("申报单 {} 流程启动失败", form.getFormNo(), e);
                
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
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已提交";
            case 2: return "已审核";
            case 3: return "已完成";
            default: return "状态" + status;
        }
    }

    // ================== 提货单附件管理 ==================
    
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