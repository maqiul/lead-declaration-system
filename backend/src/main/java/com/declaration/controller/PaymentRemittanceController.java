package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.PaymentRemittance;
import com.declaration.service.PaymentRemittanceService;
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
 * 出款水单管理 Controller
 */
@Slf4j
@Tag(name = "出款水单管理接口")
@RestController
@RequestMapping("/v1/payment-remittances")
@RequiredArgsConstructor
public class PaymentRemittanceController {

    private final PaymentRemittanceService paymentRemittanceService;

    @PostMapping
    @Operation(summary = "创建出款水单")
    @RequiresPermissions("business:payment-remittance:create")
    public Result<PaymentRemittance> createRemittance(@RequestBody PaymentRemittance remittance) {
        PaymentRemittance result = paymentRemittanceService.createRemittance(remittance);
        return Result.success(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新出款水单(草稿状态)")
    @RequiresPermissions("business:payment-remittance:update")
    public Result<Void> updateRemittance(
            @Parameter(description = "出款水单ID") @PathVariable Long id,
            @RequestBody PaymentRemittance remittance) {
        PaymentRemittance existing = paymentRemittanceService.getById(id);
        if (existing == null) {
            return Result.fail("出款水单不存在");
        }
        if (existing.getStatus() != 0) {
            return Result.fail("只有草稿状态的出款水单可以修改");
        }
        remittance.setId(id);
        paymentRemittanceService.updateById(remittance);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除出款水单(草稿状态)")
    @RequiresPermissions("business:payment-remittance:delete")
    public Result<Void> deleteRemittance(@Parameter(description = "出款水单ID") @PathVariable Long id) {
        PaymentRemittance existing = paymentRemittanceService.getById(id);
        if (existing == null) {
            return Result.fail("出款水单不存在");
        }
        if (existing.getStatus() != 0) {
            return Result.fail("只有草稿状态的出款水单可以删除");
        }
        paymentRemittanceService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交出款水单审核")
    @RequiresPermissions("business:payment-remittance:submit")
    public Result<Void> submitForAudit(@Parameter(description = "出款水单ID") @PathVariable Long id) {
        paymentRemittanceService.submitForAudit(id);
        return Result.success();
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审核出款水单")
    @RequiresPermissions("business:payment-remittance:audit")
    public Result<Void> auditRemittance(
            @Parameter(description = "出款水单ID") @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String auditRemark) {
        paymentRemittanceService.auditRemittance(id, approved, bankAccountId, auditRemark);
        return Result.success();
    }

    @PostMapping("/{id}/revoke-audit")
    @Operation(summary = "反审核出款水单")
    @RequiresPermissions("business:payment-remittance:revoke-audit")
    public Result<Void> revokeAudit(@Parameter(description = "出款水单ID") @PathVariable Long id) {
        paymentRemittanceService.revokeAudit(id);
        return Result.success();
    }

    @PostMapping("/{id}/relate-form")
    @Operation(summary = "关联申报单")
    @RequiresPermissions("business:payment-remittance:update")
    public Result<Void> relateToForm(
            @Parameter(description = "出款水单ID") @PathVariable Long id,
            @RequestParam Long formId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false, defaultValue = "1") Integer relationType) {
        paymentRemittanceService.relateToForm(id, formId, amount, relationType);
        return Result.success();
    }

    @DeleteMapping("/{id}/unrelate-form")
    @Operation(summary = "取消关联申报单")
    @RequiresPermissions("business:payment-remittance:update")
    public Result<Void> unrelateFromForm(
            @Parameter(description = "出款水单ID") @PathVariable Long id,
            @RequestParam Long formId) {
        paymentRemittanceService.unrelateFromForm(id, formId);
        return Result.success();
    }

    @GetMapping("/{id}/related-forms")
    @Operation(summary = "获取出款水单关联的所有申报单")
    @RequiresPermissions("business:payment-remittance:view")
    public Result<List<Map<String, Object>>> getRelatedForms(@Parameter(description = "出款水单ID") @PathVariable Long id) {
        List<Map<String, Object>> forms = paymentRemittanceService.getRelatedForms(id);
        return Result.success(forms);
    }

    @GetMapping("/form/{formId}")
    @Operation(summary = "获取申报单关联的所有出款水单")
    @RequiresPermissions(value = {"business:payment-remittance:view", "business:declaration:view"}, logical = RequiresPermissions.Logical.OR)
    public Result<List<Map<String, Object>>> getRemittancesByFormId(
            @Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<Map<String, Object>> remittances = paymentRemittanceService.getRemittancesByFormId(formId);
        return Result.success(remittances);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取出款水单详情")
    @RequiresPermissions("business:payment-remittance:view")
    public Result<PaymentRemittance> getRemittance(@Parameter(description = "出款水单ID") @PathVariable Long id) {
        PaymentRemittance remittance = paymentRemittanceService.getById(id);
        if (remittance == null) {
            return Result.fail("出款水单不存在");
        }
        return Result.success(remittance);
    }

    @GetMapping
    @Operation(summary = "分页查询出款水单")
    @RequiresPermissions("business:payment-remittance:view")
    public Result<IPage<PaymentRemittance>> getPage(
            PageParam pageParam,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String paymentNo,
            @RequestParam(required = false) String relationStatus) {
        IPage<PaymentRemittance> page = paymentRemittanceService.getPage(pageParam, status, paymentNo, relationStatus);
        return Result.success(page);
    }
}
