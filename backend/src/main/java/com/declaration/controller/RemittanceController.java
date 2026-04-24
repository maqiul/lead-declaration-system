package com.declaration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.service.DeclarationRemittanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 水单管理Controller(独立流程)
 */
@Slf4j
@Tag(name = "水单管理接口")
@RestController
@RequestMapping("/v1/remittances")
@RequiredArgsConstructor
public class RemittanceController {

    private final DeclarationRemittanceService remittanceService;

    @PostMapping
    @Operation(summary = "创建水单")
    @RequiresPermissions("business:remittance:create")
    public Result<DeclarationRemittance> createRemittance(@RequestBody DeclarationRemittance remittance) {
        DeclarationRemittance result = remittanceService.createRemittance(remittance);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新水单(草稿状态)")
    @RequiresPermissions("business:remittance:update")
    public Result<Void> updateRemittance(
            @Parameter(description = "水单ID") @PathVariable Long id,
            @RequestBody DeclarationRemittance remittance) {
        DeclarationRemittance existing = remittanceService.getById(id);
        if (existing == null) {
            return Result.fail("水单不存在");
        }
        if (existing.getStatus() != 0) {
            return Result.fail("只有草稿状态的水单可以修改");
        }
        remittance.setId(id);
        remittanceService.updateById(remittance);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除水单(草稿状态)")
    @RequiresPermissions("business:remittance:delete")
    public Result<Void> deleteRemittance(@Parameter(description = "水单ID") @PathVariable Long id) {
        DeclarationRemittance existing = remittanceService.getById(id);
        if (existing == null) {
            return Result.fail("水单不存在");
        }
        if (existing.getStatus() != 0) {
            return Result.fail("只有草稿状态的水单可以删除");
        }
        remittanceService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交水单审核")
    @RequiresPermissions("business:remittance:submit")
    public Result<Void> submitForAudit(@Parameter(description = "水单ID") @PathVariable Long id) {
        remittanceService.submitForAudit(id);
        return Result.success();
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审核水单")
    @RequiresPermissions("business:remittance:audit")
    public Result<Void> auditRemittance(
            @Parameter(description = "水单ID") @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) BigDecimal taxRate,
            @RequestParam(required = false) String auditRemark) {
        remittanceService.auditRemittance(id, approved, bankAccountId, taxRate, auditRemark);
        return Result.success();
    }

    @PostMapping("/{id}/relate-form")
    @Operation(summary = "关联申报单")
    @RequiresPermissions("business:remittance:update")
    public Result<Void> relateToForm(
            @Parameter(description = "水单ID") @PathVariable Long id,
            @RequestParam Long formId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false, defaultValue = "1") Integer relationType) {
        remittanceService.relateToForm(id, formId, amount, relationType);
        return Result.success();
    }

    @DeleteMapping("/{id}/unrelate-form")
    @Operation(summary = "取消关联申报单")
    @RequiresPermissions("business:remittance:update")
    public Result<Void> unrelateFromForm(
            @Parameter(description = "水单ID") @PathVariable Long id,
            @RequestParam Long formId) {
        remittanceService.unrelateFromForm(id, formId);
        return Result.success();
    }

    @GetMapping("/{id}/related-forms")
    @Operation(summary = "获取水单关联的所有申报单")
    @RequiresPermissions("business:remittance:view")
    public Result<List<Map<String, Object>>> getRelatedForms(@Parameter(description = "水单ID") @PathVariable Long id) {
        List<Map<String, Object>> forms = remittanceService.getRelatedForms(id);
        return Result.success(forms);
    }

    @GetMapping("/form/{formId}")
    @Operation(summary = "获取申报单关联的所有水单")
    @RequiresPermissions("business:remittance:view")
    public Result<List<Map<String, Object>>> getRemittancesByFormId(
            @Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<Map<String, Object>> remittances = remittanceService.getRemittancesByFormId(formId);
        return Result.success(remittances);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取水单详情")
    @RequiresPermissions("business:remittance:view")
    public Result<DeclarationRemittance> getRemittance(@Parameter(description = "水单ID") @PathVariable Long id) {
        DeclarationRemittance remittance = remittanceService.getById(id);
        if (remittance == null) {
            return Result.fail("水单不存在");
        }
        return Result.success(remittance);
    }

    @GetMapping
    @Operation(summary = "分页查询水单")
    @RequiresPermissions("business:remittance:view")
    public Result<IPage<DeclarationRemittance>> getPage(
            PageParam pageParam,
            @RequestParam(required = false) Integer remittanceType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String remittanceNo) {
        
        // 普通用户只能查看自己创建的水单
        // 管理员和财务经理可以查看所有水单
        IPage<DeclarationRemittance> page = remittanceService.getPage(pageParam, remittanceType, status, remittanceNo);
        return Result.success(page);
    }

    @GetMapping("/calculate-fee")
    @Operation(summary = "计算银行手续费")
    @RequiresPermissions("business:remittance:view")
    public Result<BigDecimal> calculateBankFee(
            @RequestParam Long bankAccountId,
            @RequestParam BigDecimal amount) {
        BigDecimal fee = remittanceService.calculateBankFee(bankAccountId, amount);
        return Result.success(fee);
    }
}
