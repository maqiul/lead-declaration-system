package com.declaration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.PaymentMethod;
import com.declaration.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 支付方式配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/payment-methods")
@RequiredArgsConstructor
@Tag(name = "支付方式配置管理", description = "支付方式配置相关接口")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    @Operation(summary = "分页查询支付方式")
    @RequiresPermissions("system:payment:list")
    public Result<Page<PaymentMethod>> getPaymentMethods(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<PaymentMethod> result = paymentMethodService.getPage(page, size, keyword, status);
        return Result.success(result);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的支付方式")
    public Result<List<PaymentMethod>> getEnabledPaymentMethods() {
        List<PaymentMethod> list = paymentMethodService.getEnabledList();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取支付方式")
    @RequiresPermissions("system:payment:view")
    public Result<PaymentMethod> getPaymentMethodById(@Parameter(description = "支付方式ID") @PathVariable Long id) {
        PaymentMethod paymentMethod = paymentMethodService.getById(id);
        if (paymentMethod == null) {
            return Result.fail("支付方式不存在");
        }
        return Result.success(paymentMethod);
    }

    @PostMapping
    @Operation(summary = "新增支付方式")
    @RequiresPermissions("system:payment:add")
    public Result<Void> addPaymentMethod(@RequestBody PaymentMethod paymentMethod) {
        // 检查名称是否重复
        boolean nameExists = paymentMethodService.lambdaQuery()
                .eq(PaymentMethod::getName, paymentMethod.getName())
                .exists();
        if (nameExists) {
            return Result.fail("支付方式名称(英文)已存在");
        }
        
        // 检查代码是否重复
        if (paymentMethod.getCode() != null && !paymentMethod.getCode().isEmpty()) {
            boolean codeExists = paymentMethodService.lambdaQuery()
                    .eq(PaymentMethod::getCode, paymentMethod.getCode())
                    .exists();
            if (codeExists) {
                return Result.fail("支付方式代码已存在");
            }
        }
        
        boolean saved = paymentMethodService.save(paymentMethod);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改支付方式")
    @RequiresPermissions("system:payment:edit")
    public Result<Void> updatePaymentMethod(
            @Parameter(description = "支付方式ID") @PathVariable Long id,
            @RequestBody PaymentMethod paymentMethod) {
        
        PaymentMethod existing = paymentMethodService.getById(id);
        if (existing == null) {
            return Result.fail("支付方式不存在");
        }
        
        // 检查名称是否重复（排除自己）
        boolean nameExists = paymentMethodService.lambdaQuery()
                .eq(PaymentMethod::getName, paymentMethod.getName())
                .ne(PaymentMethod::getId, id)
                .exists();
        if (nameExists) {
            return Result.fail("支付方式名称(英文)已存在");
        }
        
        // 检查代码是否重复（排除自己）
        if (paymentMethod.getCode() != null && !paymentMethod.getCode().isEmpty()) {
            boolean codeExists = paymentMethodService.lambdaQuery()
                    .eq(PaymentMethod::getCode, paymentMethod.getCode())
                    .ne(PaymentMethod::getId, id)
                    .exists();
            if (codeExists) {
                return Result.fail("支付方式代码已存在");
            }
        }
        
        paymentMethod.setId(id);
        boolean updated = paymentMethodService.updateById(paymentMethod);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除支付方式")
    @RequiresPermissions("system:payment:delete")
    public Result<Void> deletePaymentMethod(@Parameter(description = "支付方式ID") @PathVariable Long id) {
        PaymentMethod paymentMethod = paymentMethodService.getById(id);
        if (paymentMethod == null) {
            return Result.fail("支付方式不存在");
        }
        
        boolean removed = paymentMethodService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用支付方式")
    @RequiresPermissions("system:payment:edit")
    public Result<Void> toggleStatus(
            @Parameter(description = "支付方式ID") @PathVariable Long id,
            @Parameter(description = "状态 0-禁用 1-启用") @RequestParam Integer status) {
        
        PaymentMethod paymentMethod = paymentMethodService.getById(id);
        if (paymentMethod == null) {
            return Result.fail("支付方式不存在");
        }
        
        paymentMethod.setStatus(status);
        boolean updated = paymentMethodService.updateById(paymentMethod);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }
}
