package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.CurrencyInfo;
import com.declaration.service.CurrencyInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货币信息管理控制器
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Slf4j
@RestController
@RequestMapping("/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "货币信息管理", description = "货币信息相关接口")
public class CurrencyInfoController {

    private final CurrencyInfoService currencyInfoService;

    /**
     * 分页查询货币信息
     */
    @GetMapping
    @Operation(summary = "分页查询货币信息")
    @RequiresPermissions("system:currency:view")
    public Result<Page<CurrencyInfo>> getCurrencies(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<CurrencyInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<CurrencyInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(CurrencyInfo::getCurrencyCode, keyword)
                   .or()
                   .like(CurrencyInfo::getCurrencyName, keyword)
                   .or()
                   .like(CurrencyInfo::getChineseName, keyword));
        }
        
        if (status != null) {
            wrapper.eq(CurrencyInfo::getStatus, status);
        }
        
        wrapper.orderByAsc(CurrencyInfo::getSort)
               .orderByDesc(CurrencyInfo::getCreateTime);
        
        Page<CurrencyInfo> result = currencyInfoService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的货币信息（用于下拉选择）
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的货币信息")
    public Result<List<CurrencyInfo>> getEnabledCurrencies() {
        List<CurrencyInfo> currencies = currencyInfoService.getEnabledList();
        return Result.success(currencies);
    }

    /**
     * 根据ID获取货币信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取货币信息")
    @RequiresPermissions("system:currency:view")
    public Result<CurrencyInfo> getCurrencyById(@Parameter(description = "货币ID") @PathVariable Long id) {
        CurrencyInfo currency = currencyInfoService.getById(id);
        if (currency == null) {
            return Result.fail("货币信息不存在");
        }
        return Result.success(currency);
    }

    /**
     * 新增货币信息
     */
    @PostMapping
    @Operation(summary = "新增货币信息")
    @RequiresPermissions("system:currency:create")
    public Result<Void> addCurrency(@RequestBody CurrencyInfo currency) {
        // 检查货币代码是否重复
        boolean exists = currencyInfoService.lambdaQuery()
                .eq(CurrencyInfo::getCurrencyCode, currency.getCurrencyCode())
                .exists();
        if (exists) {
            return Result.fail("货币代码已存在");
        }
        
        // 验证货币代码格式（3位大写字母，符合ISO 4217标准）
        if (currency.getCurrencyCode() == null || !currency.getCurrencyCode().matches("^[A-Z]{3}$")) {
            return Result.fail("货币代码必须是3位大写字母(ISO 4217标准)");
        }
        
        // 设置默认值
        if (currency.getStatus() == null) {
            currency.setStatus(1);
        }
        if (currency.getSort() == null) {
            currency.setSort(0);
        }
        
        boolean saved = currencyInfoService.save(currency);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    /**
     * 修改货币信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改货币信息")
    @RequiresPermissions("system:currency:update")
    public Result<Void> updateCurrency(
            @Parameter(description = "货币ID") @PathVariable Long id,
            @RequestBody CurrencyInfo currency) {
        
        CurrencyInfo existing = currencyInfoService.getById(id);
        if (existing == null) {
            return Result.fail("货币信息不存在");
        }
        
        // 检查货币代码是否重复（排除自己）
        boolean exists = currencyInfoService.lambdaQuery()
                .eq(CurrencyInfo::getCurrencyCode, currency.getCurrencyCode())
                .ne(CurrencyInfo::getId, id)
                .exists();
        if (exists) {
            return Result.fail("货币代码已存在");
        }
        
        // 验证货币代码格式（3位大写字母，符合ISO 4217标准）
        if (currency.getCurrencyCode() != null && !currency.getCurrencyCode().matches("^[A-Z]{3}$")) {
            return Result.fail("货币代码必须是3位大写字母(ISO 4217标准)");
        }
        
        currency.setId(id);
        boolean updated = currencyInfoService.updateById(currency);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    /**
     * 删除货币信息
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除货币信息")
    @RequiresPermissions("system:currency:delete")
    public Result<Void> deleteCurrency(@Parameter(description = "货币ID") @PathVariable Long id) {
        CurrencyInfo currency = currencyInfoService.getById(id);
        if (currency == null) {
            return Result.fail("货币信息不存在");
        }
        
        boolean removed = currencyInfoService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 启用/禁用货币信息（自动取反）
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用货币信息")
    @RequiresPermissions("system:currency:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "货币ID") @PathVariable Long id) {
        
        CurrencyInfo currency = currencyInfoService.getById(id);
        if (currency == null) {
            return Result.fail("货币信息不存在");
        }
        
        // 自动取反：0变1，1变0
        Integer newStatus = (currency.getStatus() == null || currency.getStatus() == 1) ? 0 : 1;
        currency.setStatus(newStatus);
        boolean updated = currencyInfoService.updateById(currency);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }
}
