package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationCarton;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationProduct;
import com.declaration.service.DeclarationCartonProductService;
import com.declaration.service.DeclarationCartonService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private final DeclarationProductService declarationProductService;
    private final DeclarationCartonService declarationCartonService;
    private final DeclarationCartonProductService declarationCartonProductService;
    private final com.declaration.service.ExcelExportService excelExportService;
    private final com.declaration.service.DeclarationAttachmentService attachmentService;
    private final com.declaration.service.DeclarationRemittanceService remittanceService;
    private final com.declaration.service.ProcessInstanceService processInstanceService;
    private final com.declaration.service.DeclarationDraftService declarationDraftService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * 保存水单信息
     */
    @PostMapping("/{id}/remittance")
    @Operation(summary = "保存水单信息")
    @RequiresPermissions("business:declaration:edit")
    public Result<Void> saveRemittance(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody com.declaration.entity.DeclarationRemittance remittance) {
        
        DeclarationForm form = declarationFormService.getById(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }
        
        remittance.setFormId(id);
        remittanceService.saveOrUpdate(remittance);
        
        try {
            // 自动生成水单记录导出文件
            com.declaration.entity.DeclarationAttachment attachment = excelExportService.generateAndSaveRemittanceReport(remittance, form);
            if (attachment != null) {
                attachmentService.save(attachment);
            }
        } catch (java.io.IOException e) {
            log.error("生成水单文件失败", e);
        }
        
        return Result.success();
    }

    /**
     * 审核申报单
     */
    @PostMapping("/{id}/audit")
    @Operation(summary = "审核申报单")
    @RequiresPermissions("business:declaration:audit")
    public Result<Void> auditDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> auditData) throws java.io.IOException {
        
        DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
        if (form == null) {
            return Result.fail("申报单不存在");
        }

        Integer result = (Integer) auditData.get("result"); // 1-通过, 2-驳回
        Integer currentStatus = form.getStatus();

        if (Integer.valueOf(1).equals(result)) {
            // 根据当前状态决定下一步状态
            if (currentStatus == 1) {
                // 初审通过 -> 定金待提交
                form.setStatus(2);
                log.info("申报单 {} 初审通过，开始自动生成全套单证", form.getFormNo());
                generateAndSaveExport(form);
            } else if (currentStatus == 3) {
                // 定金审核通过 -> 尾款待提交
                form.setStatus(4);
                log.info("申报单 {} 定金审核通过", form.getFormNo());
                // 这里水单文件在 saveRemittance 时已生成，如果需要聚合生成可以调相关逻辑
            } else if (currentStatus == 5) {
                // 尾款审核通过 -> 已完成
                form.setStatus(6);
                log.info("申报单 {} 尾款审核通过，流程正式结束", form.getFormNo());
            } else {
                return Result.fail("当前状态不支持审核操作");
            }
            
            declarationFormService.updateById(form);
        } else {
            // 驳回逻辑：根据当前状态回退
            if (currentStatus == 1) {
                form.setStatus(0); // 驳回到草稿
            } else if (currentStatus == 3) {
                form.setStatus(2); // 驳回到待提交定金
            } else if (currentStatus == 5) {
                form.setStatus(4); // 驳回到待提交尾款
            }
            declarationFormService.updateById(form);
        }
        
        return Result.success();
    }

    /**
     * 辅助方法：生成并保存全套单证
     */
    private void generateAndSaveExport(DeclarationForm form) {
        try {
            com.declaration.entity.DeclarationAttachment attachment = excelExportService.generateAndSaveExportDocuments(form);
            if (attachment != null) {
                attachmentService.save(attachment);
            }
        } catch (Exception e) {
            log.error("申报单 {} 自动生成导出文件失败", form.getFormNo(), e);
        }
    }

    /**
     * 提交到下一步审核
     * @param id 申报单ID
     * @param auditType 提交类型: deposit-定金, balance-尾款
     */
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

        if ("deposit".equals(auditType)) {
            if (form.getStatus() != 2) return Result.fail("当前状态无法提交定金审核");
            form.setStatus(3);
        } else if ("balance".equals(auditType)) {
            if (form.getStatus() != 4) return Result.fail("当前状态无法提交尾款审核");
            form.setStatus(5);
        } else {
            return Result.fail("未知的提交类型");
        }

        declarationFormService.updateById(form);
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
        
        if (cn.dev33.satoken.stp.StpUtil.isLogin()) {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            Object userOrgIdObj = cn.dev33.satoken.stp.StpUtil.getSession().get("orgId");
            Long userOrgId = userOrgIdObj != null ? Long.valueOf(userOrgIdObj.toString()) : null;

            // 如果明确查草稿 (status == 0)
            if (status != null && status == 0) {
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.declaration.entity.DeclarationDraft> draftWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                draftWrapper.eq(com.declaration.entity.DeclarationDraft::getUserId, userId);
                if (formNo != null && !formNo.isEmpty()) {
                    draftWrapper.like(com.declaration.entity.DeclarationDraft::getFormNo, formNo);
                }
                draftWrapper.orderByDesc(com.declaration.entity.DeclarationDraft::getUpdateTime);
                
                Page<com.declaration.entity.DeclarationDraft> draftPage = new Page<>(pageParam.getCurrent(), pageParam.getSize());
                IPage<com.declaration.entity.DeclarationDraft> draftResult = declarationDraftService.page(draftPage, draftWrapper);
                
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
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeclarationForm> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            if (formNo != null && !formNo.isEmpty()) {
                queryWrapper.like(DeclarationForm::getFormNo, formNo);
            }
            if (status != null) {
                queryWrapper.eq(DeclarationForm::getStatus, status);
            }
            
            boolean isApprover = cn.dev33.satoken.stp.StpUtil.hasPermission("business:declaration:audit");
            queryWrapper.and(wrapper -> {
                wrapper.eq(DeclarationForm::getCreateBy, userId); 
                if (isApprover) {
                    wrapper.or(w -> w.ne(DeclarationForm::getStatus, 0));
                } else {
                    wrapper.or(w -> w.isNotNull(DeclarationForm::getOrgId)
                        .eq(userOrgId != null, DeclarationForm::getOrgId, userOrgId)
                        .ne(DeclarationForm::getStatus, 0));
                }
            });
            
            IPage<DeclarationForm> result = declarationFormService.page(page, queryWrapper);
            return Result.success(result);
        }

        return Result.success(declarationFormService.page(page));
    }

    @PostMapping("/draft")
    @Operation(summary = "保存草稿")
    @RequiresPermissions("business:declaration:add")
    public Result<Long> saveDraft(@RequestBody DeclarationForm form) {
        try {
            Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            Object orgIdObj = cn.dev33.satoken.stp.StpUtil.getSession().get("orgId");
            Long orgId = orgIdObj != null ? Long.valueOf(orgIdObj.toString()) : null;

            com.declaration.entity.DeclarationDraft draft = new com.declaration.entity.DeclarationDraft();
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
            com.declaration.entity.DeclarationDraft draft = declarationDraftService.getById(id);
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
            com.declaration.entity.DeclarationDraft draft = declarationDraftService.getById(id);
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
    @Operation(summary = "创建申报单")
    @RequiresPermissions("business:declaration:add")
    public Result<Long> createDeclaration(@Valid @RequestBody DeclarationForm form) {
        // 生成申报单号
        if (form.getFormNo() == null || form.getFormNo().isEmpty()) {
            form.setFormNo(generateFormNo());
        }
        
        // 如果前端未提供发票号，则按规则生成
        if (form.getInvoiceNo() == null || form.getInvoiceNo().isEmpty()) {
            // 生成规则：ZIYI-yy-mmdd
            String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yy-MMdd"));
            form.setInvoiceNo("ZIYI-" + currentDate);
        }
        
        boolean saved = declarationFormService.saveDeclarationForm(form);
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
    @Operation(summary = "提交申报单")
    @RequiresPermissions("business:declaration:submit")
    public Result<Void> submitDeclaration(@Parameter(description = "申报单ID") @PathVariable Long id) {
        DeclarationForm form = declarationFormService.getById(id);
        if (form != null) {
            if (form.getStatus() != 0) {
                return Result.fail("当前状态不支持提交操作");
            }
            
            form.setStatus(1); // 已提交状态
            declarationFormService.updateById(form);
            
            // 在提交时生成Excel文件
            try {
                DeclarationForm fullForm = declarationFormService.getFullDeclarationForm(id);
                if (fullForm != null) {
                    com.declaration.entity.DeclarationAttachment attachment = excelExportService.generateAndSaveExportDocuments(fullForm);
                    if (attachment != null) {
                        attachmentService.save(attachment);
                        log.info("申报单 {} 提交时自动生成全套单证成功", fullForm.getFormNo());
                    }
                }
            } catch (Exception e) {
                log.error("申报单 {} 提交时自动生成导出文件失败", form.getFormNo(), e);
                // 即使生成失败也不影响提交流程
            }
            
            // 启动 Flowable 流程
            try {
                java.util.Map<String, Object> variables = new java.util.HashMap<>();
                variables.put("starterId", form.getCreateBy());
                variables.put("orgId", form.getOrgId());
                variables.put("formNo", form.getFormNo());
                
                processInstanceService.startProcessInstance("declarationProcess", String.valueOf(id), variables);
                log.info("申报单 {} 流程启动成功", form.getFormNo());
            } catch (Exception e) {
                log.error("申报单 {} 流程启动失败", form.getFormNo(), e);
                // 流程启动失败可以考虑回滚状态或者记录告警
            }
        }
        return Result.success();
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
        // TODO: 实现导出功能
        response.setHeader("Content-Type", "application/octet-stream");
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