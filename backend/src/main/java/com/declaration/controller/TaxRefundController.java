package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.*;
import com.declaration.service.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 税务退费申请管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/tax-refunds")
@RequiredArgsConstructor
@Tag(name = "税务退费申请管理", description = "税务退费申请相关接口")
public class TaxRefundController {

    private final TaxRefundApplicationService taxRefundApplicationService;
    private final TaxRefundAttachmentService taxRefundAttachmentService;
    private final DeclarationFormService declarationFormService;
    private final DeclarationProductService declarationProductService;
    private final UserService userService;
    private final RuntimeService runtimeService;
    private final TaskService flowableTaskService;
    private final HistoryService historyService;
    private final OrganizationService organizationService;

    /**
     * 获取退税申请列表
     */
    @GetMapping
    @Operation(summary = "获取退税申请列表")
    @RequiresPermissions("business:tax-refund:view")
    public Result<Page<TaxRefundApplication>> getTaxRefundList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "申请编号") @RequestParam(required = false) String applicationNo,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "发起人") @RequestParam(required = false) String initiatorName) {

        Page<TaxRefundApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaxRefundApplication> wrapper = new LambdaQueryWrapper<>();

        // 组织级数据权限隔离
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean hasApprovePermission = StpUtil.hasPermission("business:tax-refund:approve") 
            || StpUtil.hasPermission("system:tax-refund:approve");
        
        // 管理员(userId=1)或有审批权限的用户可以查看所有
        if (currentUserId != 1L && !hasApprovePermission) {
            // 普通用户只能查看本组织的数据
            User currentUser = userService.getById(currentUserId);
            if (currentUser != null && currentUser.getOrgId() != null) {
                wrapper.eq(TaxRefundApplication::getOrgId, currentUser.getOrgId());
            } else {
                // 用户没有组织，只能看自己创建的
                wrapper.eq(TaxRefundApplication::getInitiatorId, currentUserId);
            }
        }

        if (applicationNo != null && !applicationNo.isEmpty()) {
            wrapper.like(TaxRefundApplication::getApplicationNo, applicationNo);
        }
        if (status != null) {
            wrapper.eq(TaxRefundApplication::getStatus, status);
        }
        if (initiatorName != null && !initiatorName.isEmpty()) {
            wrapper.like(TaxRefundApplication::getInitiatorName, initiatorName);
        }

        wrapper.orderByDesc(TaxRefundApplication::getCreateTime);
        Page<TaxRefundApplication> result = taxRefundApplicationService.page(page, wrapper);

        return Result.success(result);
    }

    /**
     * 获取退税申请详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取退税申请详情")
    @RequiresPermissions("business:tax-refund:detail")
    public Result<TaxRefundApplication> getTaxRefundDetail(@Parameter(description = "申请ID") @PathVariable Long id) {
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        return Result.success(application);
    }

    /**
     * 创建退税申请
     */
    @PostMapping
    @Operation(summary = "创建退税申请")
    @RequiresPermissions("business:tax-refund:create")
    public Result<TaxRefundApplication> createTaxRefund(@RequestBody TaxRefundApplication application) {
        // 获取当前登录用户信息
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            application.setInitiatorId(userId);
            application.setCreateBy(userId);
                
            // 获取用户详细信息
            User currentUser = userService.getById(userId);
            if (currentUser != null) {
                application.setInitiatorName(currentUser.getNickname() != null ? 
                    currentUser.getNickname() : currentUser.getUsername());
            }
                
            // 获取用户所属部门信息
            Object userOrgIdObj = StpUtil.getSession().get("orgId");
            if (userOrgIdObj != null) {
                Long orgId = Long.valueOf(userOrgIdObj.toString());
                application.setDepartmentId(orgId);
                application.setOrgId(orgId); // 设置组织ID用于数据隔离
                Organization org = organizationService.getById(orgId);
                if (org != null) {
                    application.setDepartmentName(org.getOrgName());
                }
            } else if (currentUser != null && currentUser.getOrgId() != null) {
                // 从User实体获取orgId作为备用
                application.setOrgId(currentUser.getOrgId());
            }
        }
            
        // 生成申请编号
        String applicationNo = "TR"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                String.format("%04d", (int) (Math.random() * 10000));
        application.setApplicationNo(applicationNo);
        application.setStatus(0); // 草稿状态
            
        if (taxRefundApplicationService.save(application)) {
            return Result.success(application);
        }
        return Result.fail("创建失败");
    }

    /**
     * 保存退税申请草稿
     */
    @PostMapping("/draft")
    @Operation(summary = "保存退税申请草稿")
    @RequiresPermissions("business:tax-refund:create")
    public Result<TaxRefundApplication> saveDraft(@RequestBody TaxRefundApplication application) {
        // 获取当前登录用户信息
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            application.setInitiatorId(userId);
            application.setCreateBy(userId);
            
            // 获取用户详细信息
            User currentUser = userService.getById(userId);
            if (currentUser != null) {
                application.setInitiatorName(currentUser.getNickname() != null ? 
                    currentUser.getNickname() : currentUser.getUsername());
            }
            
            // 获取用户所属部门信息
            Object userOrgIdObj = StpUtil.getSession().get("orgId");
            if (userOrgIdObj != null) {
                Long orgId = Long.valueOf(userOrgIdObj.toString());
                application.setDepartmentId(orgId);
                application.setOrgId(orgId); // 设置组织ID用于数据隔离
            } else if (currentUser != null && currentUser.getOrgId() != null) {
                // 从User实体获取orgId作为备用
                application.setOrgId(currentUser.getOrgId());
            }
        }
        
        // 生成申请编号（如果是新建草稿）
        if (application.getApplicationNo() == null || application.getApplicationNo().isEmpty()) {
            String applicationNo = "TR"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    String.format("%04d", (int) (Math.random() * 10000));
            application.setApplicationNo(applicationNo);
        }
        
        application.setStatus(0); // 草稿状态
        
        if (taxRefundApplicationService.save(application)) {
            return Result.success(application);
        }
        return Result.fail("保存失败");
    }
    @PutMapping("/{id}")
    @Operation(summary = "更新退税申请")
    @RequiresPermissions("business:tax-refund:update")
    public Result<Void> updateTaxRefund(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody TaxRefundApplication application) {

        application.setId(id);
        if (taxRefundApplicationService.updateById(application)) {
            return Result.success();
        }
        return Result.fail("更新失败");
    }

    /**
     * 批量删除退税申请
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除退税申请")
    @RequiresPermissions("business:tax-refund:delete")
    public Result<Void> batchDeleteTaxRefund(
            @Parameter(description = "申请ID列表") @RequestBody List<Long> ids) {
        
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请选择要删除的申请");
        }
        
        try {
            // 检查是否存在正在进行的流程
            for (Long id : ids) {
                List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                        .processInstanceBusinessKey(id.toString())
                        .list();
                
                if (!instances.isEmpty()) {
                    TaxRefundApplication application = taxRefundApplicationService.getById(id);
                    return Result.fail("申请 " + (application != null ? application.getApplicationNo() : id) + 
                            " 存在进行中的流程，无法删除");
                }
            }
            
            // 执行批量删除
            if (taxRefundApplicationService.removeByIds(ids)) {
                return Result.success();
            }
            return Result.fail("批量删除失败");
            
        } catch (Exception e) {
            log.error("批量删除退税申请失败", e);
            return Result.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除退税申请
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除退税申请")
    @RequiresPermissions("business:tax-refund:delete")
    public Result<Void> deleteTaxRefund(@Parameter(description = "申请ID") @PathVariable Long id) {
        if (taxRefundApplicationService.removeById(id)) {
            return Result.success();
        }
        return Result.fail("删除失败");
    }

    /**
     * 提交退税申请
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交退税申请")
    @RequiresPermissions("business:tax-refund:submit")
    public Result<Void> submitTaxRefund(@Parameter(description = "申请ID") @PathVariable Long id) {
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }

        try {
            // 启动退税流程
            Map<String, Object> variables = new HashMap<>();
            variables.put("initiator", StpUtil.getLoginIdAsString());
            variables.put("applicationId", id);
            variables.put("amount", application.getAmount());
            
            ProcessInstance processInstance = 
                runtimeService.startProcessInstanceByKey("tax_refund_process_simple", id.toString(), variables);
            
            log.info("退税流程启动成功: 流程实例ID={}, 业务Key={}", 
                processInstance.getProcessInstanceId(), processInstance.getBusinessKey());
            
            // 自动完成第一个任务（departmentSubmit），让流程进入财务初审阶段
            Task firstTask = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(id.toString())
                .taskDefinitionKey("departmentSubmit")
                .singleResult();
            if (firstTask != null) {
                flowableTaskService.complete(firstTask.getId(), variables);
                log.info("已完成部门提交任务，流程进入财务初审阶段: taskId={}", firstTask.getId());
                
                // 显式更新状态为财务初审（状态2）
                application.setStatus(2);
            } else {
                // 如果没有找到任务，保持已提交状态
                application.setStatus(1);
            }
            
            if (taxRefundApplicationService.updateById(application)) {
                return Result.success();
            }
        } catch (Exception e) {
            log.error("启动退税流程失败", e);
            return Result.fail("提交失败: " + e.getMessage());
        }
        
        return Result.fail("提交失败");
    }

    /**
     * 根据当前节点和审核结果获取下一个状态
     * 简化流程状态：0-草稿 1-已提交 2-财务初审 4-退回补充 6-财务复审 7-已完成 8-已拒绝
     */
    private Integer getNextStatus(String taskDefinitionKey, int result) {
        log.debug("计算下一状态: 当前节点={}, 审核结果={}", taskDefinitionKey, result);
        
        switch (taskDefinitionKey) {
            case "financeReview":           // 财务初审
                return result == 1 ? 6 : 8; // 通过进入财务复审(状态6)，拒绝则已拒绝(状态8)
            case "financeFinalReview":      // 财务复审
                return result == 1 ? 7 : 4; // 通过完成(状态7)，拒绝退回补充(状态4)
            default:
                log.warn("未知的任务节点: {}", taskDefinitionKey);
                return null;
        }
    }

    /**
     * 审核退税申请（统一审核接口，与DeclarationFormController保持一致）
     */
    @PostMapping("/{id}/audit")
    @Operation(summary = "审核退税申请")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<String> auditTaxRefund(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody Map<String, Object> auditData) {
        try {
            TaxRefundApplication application = taxRefundApplicationService.getById(id);
            if (application == null) {
                return Result.fail("退税申请不存在");
            }

            Object resultObj = auditData.get("result"); // 1-通过, 2-驳回
            boolean isApproved = resultObj != null && ("1".equals(resultObj.toString()) || Boolean.TRUE.equals(resultObj));
            String opinion = (String) auditData.get("opinion");

            // 查找当前活动任务
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
                        diagnostic += " 该流程已于 " + hpi.getEndTime() + " 结束。";
                    } else {
                        diagnostic += " 流程实例存在但当前节点不是 UserTask。";
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

            // 设置流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", isApproved);
            if (opinion != null && !opinion.isEmpty()) {
                variables.put("approvalOpinion", opinion);
            }

            // 完成任务
            flowableTaskService.complete(activeTask.getId(), variables);
            
            log.info("当前任务信息: ID={}, DefinitionKey={}, Name={}", 
                activeTask.getId(), activeTask.getTaskDefinitionKey(), activeTask.getName());
            
            // 更新业务状态
            String taskDefinitionKey = activeTask.getTaskDefinitionKey();
            Integer newStatus = getNextStatus(taskDefinitionKey, isApproved ? 1 : 2);
            
            if (newStatus != null) {
                application.setStatus(newStatus);
                
                // 获取当前登录用户信息
                Long currentUserId = StpUtil.getLoginIdAsLong();
                User currentUser = userService.getById(currentUserId);
                String reviewerName = currentUser != null ? 
                    (currentUser.getNickname() != null && !currentUser.getNickname().isEmpty() ? 
                        currentUser.getNickname() : currentUser.getUsername()) 
                    : "Unknown";
                
                // 记录审核人信息和审核意见
                if ("financeReview".equals(taskDefinitionKey)) {
                    application.setFirstReviewOpinion(opinion);
                    application.setFirstReviewerId(currentUserId);
                    application.setFirstReviewerName(reviewerName);
                    application.setFirstReviewTime(LocalDateTime.now());
                } else if ("financeFinalReview".equals(taskDefinitionKey)) {
                    application.setFinalReviewOpinion(opinion);
                    application.setFinalReviewerId(currentUserId);
                    application.setFinalReviewerName(reviewerName);
                    application.setFinalReviewTime(LocalDateTime.now());
                }
                
                // 拒绝时记录拒绝原因
                if (!isApproved) {
                    application.setRejectReason(opinion);
                }
                
                taxRefundApplicationService.updateById(application);
                log.info("业务状态已更新为: {}", newStatus);
            } else {
                log.warn("无法确定下一状态，当前节点: {}", taskDefinitionKey);
            }

            log.info("退税申请审核完成: ID={}, 结果={}, 任务ID={}, 新状态={}", 
                id, isApproved ? "通过" : "驳回", activeTask.getId(), newStatus);
            return Result.success("审核成功");
            
        } catch (Exception e) {
            log.error("审核退税申请失败", e);
            return Result.fail("审核失败内部异常: " + e.getMessage());
        }
    }

    /**
     * 审核退税申请（原有接口保持兼容）
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审核退税申请")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Void> approveTaxRefund(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Parameter(description = "审核结果 1-通过 2-拒绝") @RequestParam Integer result,
            @Parameter(description = "审核意见") @RequestParam(required = false) String opinion) {

        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }

        if (result == 1) {
            // 审核通过
            application.setStatus(2); // 财务初审通过
            application.setFirstReviewOpinion(opinion);
        } else {
            // 审核拒绝
            application.setStatus(8); // 已拒绝
            application.setFirstReviewOpinion(opinion);
        }

        if (taxRefundApplicationService.updateById(application)) {
            return Result.success();
        }
        return Result.fail("审核失败");
    }

    /**
     * 关联附件到退税申请
     */
    @PostMapping("/{id}/attach-files")
    @Operation(summary = "关联文件到退税申请")
    @RequiresPermissions("business:tax-refund:update")
    public Result<Boolean> attachFilesToApplication(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @Parameter(description = "文件URL列表") @RequestBody List<String> fileUrls) {
        
        try {
            if (fileUrls == null || fileUrls.isEmpty()) {
                return Result.success(true);
            }
            
            // 为每个文件URL创建附件记录
            for (String fileUrl : fileUrls) {
                TaxRefundAttachment attachment = new TaxRefundAttachment();
                attachment.setApplicationId(id);
                String fileName = extractFileNameFromUrl(fileUrl);
                attachment.setFileName(fileName);
                attachment.setFilePath(fileUrl); // 使用filePath字段
                
                // 尝试从已上传的文件获取实际大小和类型
                long fileSize = 0L;
                String fileType = "application/octet-stream";
                try {
                    // 从URL中提取实际文件路径
                    String actualPath = fileUrl;
                    if (fileUrl.contains("path=")) {
                        actualPath = fileUrl.substring(fileUrl.indexOf("path=") + 5);
                    }
                    File physicalFile = new File("uploads/" + actualPath);
                    if (physicalFile.exists()) {
                        fileSize = physicalFile.length();
                    }
                    // 根据文件扩展名推断MIME类型
                    fileType = getContentTypeByFileName(fileName);
                } catch (Exception e) {
                    log.debug("获取文件元数据失败，使用默认值: {}", e.getMessage());
                }
                attachment.setFileSize(fileSize);
                attachment.setFileType(fileType);
                attachment.setAttachmentType("OTHER"); // 默认为其他类型
                attachment.setDescription("退税申请附件");
                attachment.setUploadTime(LocalDateTime.now());
                attachment.setDeleted(0); // 未删除
                
                // 设置上传人信息
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    attachment.setUploaderId(userId);
                    User currentUser = userService.getById(userId);
                    if (currentUser != null) {
                        attachment.setUploaderName(currentUser.getNickname() != null ? 
                            currentUser.getNickname() : currentUser.getUsername());
                    }
                }
                
                taxRefundAttachmentService.save(attachment);
            }
            
            return Result.success(true);
        } catch (Exception e) {
            log.error("关联文件失败", e);
            return Result.fail("关联文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 从文件名获取内容类型
     */
    private String getContentTypeByFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "application/octet-stream";
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".doc")) return "application/msword";
        if (lowerName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lowerName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".zip")) return "application/zip";
        if (lowerName.endsWith(".rar")) return "application/x-rar-compressed";
        return "application/octet-stream";
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown_file";
        }
        
        // 处理 /api/v1/files/download?path=xxx 格式的URL
        if (fileUrl.contains("path=")) {
            String path = fileUrl.substring(fileUrl.indexOf("path=") + 5);
            return path.substring(path.lastIndexOf("/") + 1);
        }
        
        // 处理普通URL
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    /**
     * 获取可关联的申报单列表
     */
    @GetMapping("/declarations")
    @Operation(summary = "获取可关联的申报单列表")
    @RequiresPermissions("business:tax-refund:view")
    public Result<List<Map<String, Object>>> getAvailableDeclarations() {
        // 获取已完成的申报单（状态为8-已完成）
        LambdaQueryWrapper<DeclarationForm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationForm::getStatus, 8); // 已完成状态
        wrapper.eq(DeclarationForm::getDelFlag, 0); // 未删除

        // 数据隔离逻辑：只能看到自己创建的，或者同组织的单子
        if (StpUtil.isLogin()) {
            Long userId = StpUtil.getLoginIdAsLong();
            Object userOrgIdObj = StpUtil.getSession().get("orgId");
            Long userOrgId = userOrgIdObj != null ? Long.valueOf(userOrgIdObj.toString()) : null;
            boolean isApprover = StpUtil.hasPermission("business:declaration:audit");

            wrapper.and(w -> {
                w.eq(DeclarationForm::getCreateBy, userId);
                if (isApprover) {
                    w.or(orW -> orW.eq(DeclarationForm::getStatus, 8)); // 审批人可以看到所有满足外层条件的
                } else {
                    w.or(orW -> orW.isNotNull(DeclarationForm::getOrgId)
                            .eq(userOrgId != null, DeclarationForm::getOrgId, userOrgId));
                }
            });
        }

        wrapper.orderByDesc(DeclarationForm::getCreateTime);

        List<DeclarationForm> completedForms = declarationFormService.list(wrapper);

        // 转换为前端需要的格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (DeclarationForm form : completedForms) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", form.getId());
            item.put("declarationNo", form.getFormNo());
            item.put("productName", getMainProductName(form.getId()));
            item.put("amount", form.getTotalAmount());
            item.put("createTime", form.getCreateTime());
            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取申报单的主要产品名称
     */
    private String getMainProductName(Long formId) {
        LambdaQueryWrapper<DeclarationProduct> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(DeclarationProduct::getFormId, formId);
        productWrapper.orderByAsc(DeclarationProduct::getId);
        
        List<DeclarationProduct> productList = declarationProductService.list(productWrapper);
        if (productList == null || productList.isEmpty()) {
            return "未知商品";
        }
        
        if (productList.size() == 1) {
            return productList.get(0).getProductName();
        } 
        
        return productList.get(0).getProductName() + "等" + productList.size() + "种商品";
    }

    /**
     * 任务完成后更新业务状态
     */
    private void updateBusinessStatusAfterTaskComplete(Task task, Integer result) {
        try {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).singleResult();
            
            if (processInstance != null && processInstance.getBusinessKey() != null) {
                Long applicationId = Long.valueOf(processInstance.getBusinessKey());
                TaxRefundApplication application = taxRefundApplicationService.getById(applicationId);
                
                if (application != null) {
                    // 根据任务节点和审批结果更新状态
                    Integer newStatus = calculateNextStatus(task.getTaskDefinitionKey(), result);
                    application.setStatus(newStatus);
                    taxRefundApplicationService.updateById(application);
                    
                    log.info("任务完成后更新退税申请状态: ID={}, 原状态={}, 新状态={}", 
                        applicationId, application.getStatus(), newStatus);
                }
            }
        } catch (Exception e) {
            log.error("更新业务状态失败", e);
        }
    }
    
    /**
     * 根据任务节点和审批结果计算下一个状态
     * 简化流程状态：0-草稿 1-已提交 2-财务初审 4-退回补充 6-财务复审 7-已完成 8-已拒绝
     * 已删除旧流程节点: returnDocument, invoiceSubmit
     */
    private Integer calculateNextStatus(String taskDefinitionKey, Integer result) {
        switch (taskDefinitionKey) {
            case "departmentSubmit":        // 部门提交
                return 1; // 已提交
            case "financeReview":           // 财务初审
                return result == 1 ? 6 : 8; // 通过进入财务复审，拒绝则已拒绝
            case "financeFinalReview":      // 财务复审
                return result == 1 ? 7 : 4; // 通过则完成，拒绝则退回补充
            default:
                return 1; // 默认状态
        }
    }
    @GetMapping("/tasks/todo")
    @Operation(summary = "获取当前用户的待办任务")
    @RequiresPermissions("business:tax-refund:list")
    public Result<IPage<Map<String, Object>>> getUserTodoTasks(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            String userId = StpUtil.getLoginIdAsString();
            List<Task> tasks = flowableTaskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();
            
            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Task task : tasks) {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("taskId", task.getId());
                taskInfo.put("taskName", task.getName());
                taskInfo.put("processInstanceId", task.getProcessInstanceId());
                taskInfo.put("createTime", task.getCreateTime());
                
                // 通过流程实例获取业务Key
                ProcessInstance processInstance = 
                    runtimeService.createProcessInstanceQuery()
                        .processInstanceId(task.getProcessInstanceId())
                        .singleResult();
                
                String businessKey = processInstance != null ? processInstance.getBusinessKey() : null;
                taskInfo.put("applicationId", businessKey);
                
                // 获取申请信息
                if (businessKey != null) {
                    try {
                        Long applicationId = Long.valueOf(businessKey);
                        TaxRefundApplication application = taxRefundApplicationService.getById(applicationId);
                        if (application != null) {
                            taskInfo.put("applicationNo", application.getApplicationNo());
                            taskInfo.put("amount", application.getAmount());
                            taskInfo.put("expectedRefundAmount", application.getExpectedRefundAmount());
                        }
                    } catch (NumberFormatException ignored) {}
                }
                
                resultList.add(taskInfo);
            }
            
            int total = resultList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<Map<String, Object>> pagedData = fromIndex < total ? resultList.subList(fromIndex, toIndex) : new ArrayList<>();

            Page<Map<String, Object>> page = new Page<>(pageNum, pageSize, total);
            page.setRecords(pagedData);
            
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取待办任务失败", e);
            return Result.fail("获取待办任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 完成任务
     */
    @PostMapping("/tasks/{taskId}/complete")
    @Operation(summary = "完成任务")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable String taskId,
            @Parameter(description = "审批结果 1-通过 2-拒绝") @RequestParam Integer result,
            @Parameter(description = "审批意见") @RequestParam(required = false) String opinion) {
        
        try {
            Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                return Result.fail("任务不存在");
            }
            
            // 设置审批结果变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("approvalResult", result);
            if (opinion != null && !opinion.isEmpty()) {
                variables.put("approvalOpinion", opinion);
            }
            
            // 完成任务
            flowableTaskService.complete(taskId, variables);
            
            // 更新业务状态
            updateBusinessStatusAfterTaskComplete(task, result);
            
            log.info("任务完成: 任务ID={}, 审批结果={}", taskId, result);
            return Result.success();
        } catch (Exception e) {
            log.error("完成任务失败", e);
            return Result.fail("完成任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取退税附件列表
     */
    @GetMapping("/{id}/attachments")
    @Operation(summary = "获取退税附件列表")
    @RequiresPermissions("business:tax-refund:list")
    public Result<List<TaxRefundAttachment>> getAttachments(@Parameter(description = "申请ID") @PathVariable Long id) {
        LambdaQueryWrapper<TaxRefundAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaxRefundAttachment::getApplicationId, id);
        wrapper.orderByDesc(TaxRefundAttachment::getUploadTime);

        List<TaxRefundAttachment> attachments = taxRefundAttachmentService.list(wrapper);
        return Result.success(attachments);
    }

    /**
     * 财务初审
     */
    @PostMapping("/{id}/first-review")
    @Operation(summary = "财务初审")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Void> firstReview(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody Map<String, Object> reviewData) {
        log.info("进入色和");
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        
        Integer resultCode = reviewData.get("result") != null ? 
            Integer.parseInt(reviewData.get("result").toString()) : null;
        String opinion = reviewData.get("opinion") != null ? reviewData.get("opinion").toString() : "";
        String taskId = reviewData.get("taskId") != null ? reviewData.get("taskId").toString() : null;
        
        // result: 1-通过, 2-拒绝
        boolean approved = resultCode != null && resultCode == 1;
        
        try {
            // 设置审核人信息
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                application.setCurrentApproverId(userId);
                User currentUser = userService.getById(userId);
                if (currentUser != null) {
                    application.setCurrentApproverName(currentUser.getNickname() != null ? 
                        currentUser.getNickname() : currentUser.getUsername());
                }
            }
            
            application.setFirstReviewOpinion(opinion);
            
            // 如果没有提供taskId，尝试查找当前任务
            if (taskId == null || taskId.isEmpty()) {
                Task task = flowableTaskService.createTaskQuery()
                    .processInstanceBusinessKey(id.toString())
                    .active()
                    .singleResult();
                if (task != null) {
                    taskId = task.getId();
                    log.info("找到当前活动任务: taskId={}, taskDefinitionKey={}", taskId, task.getTaskDefinitionKey());
                }
            }
            log.info(taskId);
            if (taskId == null) {
                log.warn("未找到流程任务，可能流程未启动或已完成，直接更新状态");
                // 没有流程任务，直接更新状态
                if (approved) {
                    application.setStatus(2); // 财务初审通过
                }
                taxRefundApplicationService.updateById(application);
                return Result.success();
            }
            
            // 设置流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", approved);
            variables.put("opinion", opinion);
            
            if (approved) {
                // 审核通过，完成任务推进流程
                if (taskId != null) {
                    flowableTaskService.complete(taskId, variables);
                    log.info("财务初审通过，任务已完成: taskId={}", taskId);
                }
                // 状态由监听器自动更新
            } else {
                // 审核拒绝
                application.setStatus(8); // 已拒绝
                application.setRejectReason(opinion);
                taxRefundApplicationService.updateById(application);
                // 终止流程
                if (taskId != null) {
                    Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
                    if (task != null) {
                        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "审核拒绝: " + opinion);
                    }
                }
                log.info("财务初审拒绝，流程已终止");
            }
            
            return Result.success();
        } catch (Exception e) {
            log.error("财务初审失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 财务复审
     */
    @PostMapping("/{id}/final-review")
    @Operation(summary = "财务复审")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Void> finalReview(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody Map<String, Object> reviewData) {
        
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        
        Integer resultCode = reviewData.get("result") != null ? 
            Integer.parseInt(reviewData.get("result").toString()) : null;
        String opinion = reviewData.get("opinion") != null ? reviewData.get("opinion").toString() : "";
        String taskId = reviewData.get("taskId") != null ? reviewData.get("taskId").toString() : null;
                
        // result: 1-通过, 2-拒绝
        boolean approved = resultCode != null && resultCode == 1;
                
        try {
            // 设置审核人信息
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                application.setCurrentApproverId(userId);
                User currentUser = userService.getById(userId);
                if (currentUser != null) {
                    application.setCurrentApproverName(currentUser.getNickname() != null ? 
                        currentUser.getNickname() : currentUser.getUsername());
                }
            }
                    
            application.setFinalReviewOpinion(opinion);
                    
            // 如果没有提供taskId，尝试查找当前任务
            if (taskId == null || taskId.isEmpty()) {
                Task task = flowableTaskService.createTaskQuery()
                    .processInstanceBusinessKey(id.toString())
                    .active()
                    .singleResult();
                if (task != null) {
                    taskId = task.getId();
                    log.info("找到当前活动任务: taskId={}, taskDefinitionKey={}", taskId, task.getTaskDefinitionKey());
                }
            }
            
            if (taskId == null) {
                log.warn("未找到流程任务，可能流程未启动或已完成，直接更新状态");
                // 没有流程任务，直接更新状态
                if (approved) {
                    application.setStatus(7); // 已完成
                }
                taxRefundApplicationService.updateById(application);
                return Result.success();
            }
                    
            // 设置流程变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", approved);
            variables.put("opinion", opinion);
                    
            if (approved) {
                // 审核通过，完成任务推进流程（会自动触发归档服务任务）
                if (taskId != null) {
                    flowableTaskService.complete(taskId, variables);
                    log.info("财务复审通过，任务已完成: taskId={}", taskId);
                }
                // 状态由归档服务任务自动更新为已完成
            } else {
                application.setStatus(8); // 已拒绝
                application.setRejectReason(opinion);
                taxRefundApplicationService.updateById(application);
                // 终止流程
                if (taskId != null) {
                    Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
                    if (task != null) {
                        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "复审拒绝: " + opinion);
                    }
                }
                log.info("财务复审拒绝，流程已终止");
            }
            
            return Result.success();
        } catch (Exception e) {
            log.error("财务复审失败", e);
            return Result.fail("审核失败: " + e.getMessage());
        }
    }

    /**
     * 生成退税文件
     * @deprecated 旧流程接口，简化流程后已不再使用，保留以兼容旧数据
     */
    @Deprecated
    @PostMapping("/{id}/generate-file")
    @Operation(summary = "生成退税文件(已废弃)")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Map<String, String>> generateFile(@Parameter(description = "申请ID") @PathVariable Long id) {
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        
        try {
            // 生成文件路径（实际应该调用文件生成服务）
            String filePath = "/uploads/tax-refund/files/" + application.getApplicationNo() + "_" + 
                System.currentTimeMillis() + ".pdf";
            
            application.setFilePath(filePath);
            application.setStatus(5); // 进入发票提交状态
            
            if (taxRefundApplicationService.updateById(application)) {
                Map<String, String> result = new HashMap<>();
                result.put("filePath", filePath);
                return Result.success(result);
            }
        } catch (Exception e) {
            log.error("生成退税文件失败", e);
            return Result.fail("生成文件失败: " + e.getMessage());
        }
        
        return Result.fail("生成文件失败");
    }

    /**
     * 退回补充材料
     */
    @PostMapping("/{id}/return")
    @Operation(summary = "退回补充材料")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Void> returnForSupplement(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody Map<String, String> data) {
        
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        
        String reason = data.get("reason");
        application.setReturnReason(reason);
        application.setStatus(4); // 退回补充
        
        if (taxRefundApplicationService.updateById(application)) {
            return Result.success();
        }
        return Result.fail("退回失败");
    }

    /**
     * 提交发票
     * @deprecated 旧流程接口，简化流程后已不再使用，保留以兼容旧数据
     */
    @Deprecated
    @PostMapping("/{id}/invoice")
    @Operation(summary = "提交发票(已废弃)")
    @RequiresPermissions("business:tax-refund:update")
    public Result<Void> submitInvoice(
            @Parameter(description = "申请ID") @PathVariable Long id,
            @RequestBody Map<String, Object> invoiceData) {
        
        TaxRefundApplication application = taxRefundApplicationService.getById(id);
        if (application == null) {
            return Result.fail("申请不存在");
        }
        
        try {
            // 更新发票信息
            if (invoiceData.containsKey("invoiceNo")) {
                application.setInvoiceNo((String) invoiceData.get("invoiceNo"));
            }
            if (invoiceData.containsKey("invoiceAmount")) {
                application.setInvoiceAmount(new java.math.BigDecimal(invoiceData.get("invoiceAmount").toString()));
            }
            if (invoiceData.containsKey("taxRate")) {
                application.setTaxRate(new java.math.BigDecimal(invoiceData.get("taxRate").toString()));
            }
            
            taxRefundApplicationService.updateById(application);
            
            // 查找并完成当前的发票提交任务
            Task task = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(id.toString())
                .taskDefinitionKey("submitInvoiceInfo")
                .singleResult();
            
            if (task != null) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("invoiceNo", application.getInvoiceNo());
                variables.put("invoiceAmount", application.getInvoiceAmount());
                variables.put("taxRate", application.getTaxRate());
                
                flowableTaskService.complete(task.getId(), variables);
                log.info("发票提交成功，任务已完成: taskId={}", task.getId());
            } else {
                // 如果没有找到流程任务，直接更新状态
                application.setStatus(6); // 进入财务复审
                taxRefundApplicationService.updateById(application);
            }
            
            return Result.success();
        } catch (Exception e) {
            log.error("提交发票失败", e);
            return Result.fail("提交发票失败: " + e.getMessage());
        }
    }

    /**
     * 获取待审核的退税申请列表
     */
    @GetMapping("/review/pending")
    @Operation(summary = "获取待审核的退税申请列表")
    @RequiresPermissions("business:tax-refund:approve")
    public Result<Page<TaxRefundApplication>> getPendingReviewList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<TaxRefundApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaxRefundApplication> wrapper = new LambdaQueryWrapper<>();
        
        // 获取状态为已提交(1)、财务初审(2)、财务复审(6)的申请
        wrapper.in(TaxRefundApplication::getStatus, 1, 2, 6);
        wrapper.orderByDesc(TaxRefundApplication::getCreateTime);
        
        Page<TaxRefundApplication> result = taxRefundApplicationService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取退税申请的审批历史
     */
    @GetMapping("/{id}/audit-history")
    @Operation(summary = "获取退税申请审批历史")
    @RequiresPermissions("business:tax-refund:detail")
    public Result<List<Map<String, Object>>> getAuditHistory(@Parameter(description = "申请ID") @PathVariable Long id) {
        try {
            TaxRefundApplication application = taxRefundApplicationService.getById(id);
            if (application == null) {
                return Result.fail("申请不存在");
            }
            
            List<Map<String, Object>> historyList = new ArrayList<>();
            
            // 1. 添加部门提交记录
            Map<String, Object> submitRecord = new HashMap<>();
            submitRecord.put("stepName", "部门提交");
            submitRecord.put("operator", application.getInitiatorName());
            submitRecord.put("operateTime", application.getCreateTime());
            submitRecord.put("status", "completed");
            submitRecord.put("opinion", "提交退税申请");
            submitRecord.put("activityId", "departmentSubmit");
            historyList.add(submitRecord);
            
            // 2. 查询Flowable历史任务
            org.flowable.engine.history.HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceBusinessKey(String.valueOf(id))
                    .singleResult();
            
            if (hpi != null) {
                List<org.flowable.task.api.history.HistoricTaskInstance> historicTasks = 
                    historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(hpi.getId())
                        .orderByHistoricTaskInstanceStartTime()
                        .asc()
                        .list();
                
                for (org.flowable.task.api.history.HistoricTaskInstance task : historicTasks) {
                    // 跳过部门提交节点（已单独添加）
                    if ("departmentSubmit".equals(task.getTaskDefinitionKey())) {
                        continue;
                    }
                    
                    Map<String, Object> taskRecord = new HashMap<>();
                    taskRecord.put("stepName", task.getName());
                    taskRecord.put("activityId", task.getTaskDefinitionKey());
                    taskRecord.put("createTime", task.getCreateTime());
                    taskRecord.put("operateTime", task.getEndTime());
                    
                    // 获取处理人
                    String assignee = task.getAssignee();
                    if (assignee != null) {
                        try {
                            User assigneeUser = userService.getById(Long.valueOf(assignee));
                            if (assigneeUser != null) {
                                taskRecord.put("operator", assigneeUser.getNickname() != null ? 
                                    assigneeUser.getNickname() : assigneeUser.getUsername());
                            } else {
                                taskRecord.put("operator", "用户" + assignee);
                            }
                        } catch (NumberFormatException e) {
                            taskRecord.put("operator", assignee);
                        }
                    } else {
                        taskRecord.put("operator", null);
                    }
                    
                    // 判断状态
                    if (task.getEndTime() != null) {
                        taskRecord.put("status", "completed");
                    } else {
                        taskRecord.put("status", "current");
                    }
                    
                    // 获取审批意见（根据节点类型）
                    String opinion = null;
                    if ("financeReview".equals(task.getTaskDefinitionKey())) {
                        opinion = application.getFirstReviewOpinion();
                    } else if ("financeFinalReview".equals(task.getTaskDefinitionKey())) {
                        opinion = application.getFinalReviewOpinion();
                    }
                    taskRecord.put("opinion", opinion);
                    
                    historyList.add(taskRecord);
                }
            }
            
            // 3. 根据状态补充流程节点（对于尚未开始的节点）
            List<String> allSteps = Arrays.asList(
                "departmentSubmit", "financeReview", "financeFinalReview", "archiveDocument"
            );
            List<String> stepNames = Arrays.asList(
                "部门提交", "财务初审", "财务复审", "文件归档"
            );
            
            Set<String> existingSteps = historyList.stream()
                .map(m -> (String) m.get("activityId"))
                .collect(java.util.stream.Collectors.toSet());
            
            for (int i = 0; i < allSteps.size(); i++) {
                if (!existingSteps.contains(allSteps.get(i))) {
                    Map<String, Object> pendingRecord = new HashMap<>();
                    pendingRecord.put("stepName", stepNames.get(i));
                    pendingRecord.put("activityId", allSteps.get(i));
                    pendingRecord.put("operator", null);
                    pendingRecord.put("operateTime", null);
                    pendingRecord.put("status", "pending");
                    pendingRecord.put("opinion", null);
                    historyList.add(pendingRecord);
                }
            }
            
            // 4. 如果申请被拒绝，添加拒绝信息
            if (application.getStatus() == 8 && application.getRejectReason() != null) {
                // 找到最后一个已完成的步骤，标记为rejected
                for (int i = historyList.size() - 1; i >= 0; i--) {
                    Map<String, Object> record = historyList.get(i);
                    if ("completed".equals(record.get("status"))) {
                        record.put("status", "rejected");
                        record.put("opinion", application.getRejectReason());
                        break;
                    }
                }
            }
            
            return Result.success(historyList);
        } catch (Exception e) {
            log.error("获取审批历史失败", e);
            return Result.fail("获取审批历史失败: " + e.getMessage());
        }
    }
}