package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.BankAccountConfig;
import com.declaration.service.BankAccountConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行账户配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "银行账户配置管理", description = "银行账户配置相关接口")
public class BankAccountConfigController {

    private final BankAccountConfigService bankAccountConfigService;

    /**
     * 分页查询银行账户配置
     */
    @GetMapping
    @Operation(summary = "分页查询银行账户配置")
    @RequiresPermissions("system:bank-account:query")
    public Result<Page<BankAccountConfig>> getBankAccounts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "币种") @RequestParam(required = false) String currency,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<BankAccountConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(BankAccountConfig::getAccountName, keyword)
                   .or()
                   .like(BankAccountConfig::getBankName, keyword)
                   .or()
                   .like(BankAccountConfig::getAccountHolder, keyword)
                   .or()
                   .like(BankAccountConfig::getAccountNumber, keyword);
        }
        
        if (currency != null && !currency.isEmpty()) {
            wrapper.eq(BankAccountConfig::getCurrency, currency);
        }
        
        if (status != null) {
            wrapper.eq(BankAccountConfig::getStatus, status);
        }
        
        wrapper.orderByAsc(BankAccountConfig::getSort)
               .orderByDesc(BankAccountConfig::getCreateTime);
        
        Page<BankAccountConfig> result = bankAccountConfigService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的银行账户（用于下拉选择）
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的银行账户")
    public Result<List<BankAccountConfig>> getEnabledBankAccounts(
            @Parameter(description = "币种") @RequestParam(required = false) String currency) {
        LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccountConfig::getStatus, 1);
        
        if (currency != null && !currency.isEmpty()) {
            wrapper.eq(BankAccountConfig::getCurrency, currency);
        }
        
        wrapper.orderByDesc(BankAccountConfig::getIsDefault)
               .orderByAsc(BankAccountConfig::getSort)
               .orderByAsc(BankAccountConfig::getAccountName);
        
        List<BankAccountConfig> accounts = bankAccountConfigService.list(wrapper);
        return Result.success(accounts);
    }

    /**
     * 获取默认银行账户
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认银行账户")
    public Result<BankAccountConfig> getDefaultBankAccount(
            @Parameter(description = "币种") @RequestParam(required = false) String currency) {
        LambdaQueryWrapper<BankAccountConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BankAccountConfig::getStatus, 1)
               .eq(BankAccountConfig::getIsDefault, 1);
        
        if (currency != null && !currency.isEmpty()) {
            wrapper.eq(BankAccountConfig::getCurrency, currency);
        }
        
        BankAccountConfig account = bankAccountConfigService.getOne(wrapper);
        if (account == null) {
            // 如果没有默认账户，返回第一个启用的账户
            LambdaQueryWrapper<BankAccountConfig> fallbackWrapper = new LambdaQueryWrapper<>();
            fallbackWrapper.eq(BankAccountConfig::getStatus, 1);
            if (currency != null && !currency.isEmpty()) {
                fallbackWrapper.eq(BankAccountConfig::getCurrency, currency);
            }
            fallbackWrapper.orderByAsc(BankAccountConfig::getSort)
                          .last("LIMIT 1");
            account = bankAccountConfigService.getOne(fallbackWrapper);
        }
        
        return Result.success(account);
    }

    /**
     * 根据ID获取银行账户配置
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取银行账户配置")
    @RequiresPermissions("system:bank-account:view")
    public Result<BankAccountConfig> getBankAccountById(@Parameter(description = "账户ID") @PathVariable Long id) {
        BankAccountConfig account = bankAccountConfigService.getById(id);
        if (account == null) {
            return Result.fail("银行账户不存在");
        }
        return Result.success(account);
    }

    /**
     * 新增银行账户配置
     */
    @PostMapping
    @Operation(summary = "新增银行账户配置")
    @RequiresPermissions("system:bank-account:add")
    public Result<Void> addBankAccount(@RequestBody BankAccountConfig account) {
        // 检查银行账号+银行名称+币种的组合是否重复
        if (account.getAccountNumber() != null && !account.getAccountNumber().isEmpty() && 
            account.getBankName() != null && !account.getBankName().isEmpty() &&
            account.getCurrency() != null && !account.getCurrency().isEmpty()) {
            
            boolean exists = bankAccountConfigService.lambdaQuery()
                    .eq(BankAccountConfig::getAccountNumber, account.getAccountNumber())
                    .eq(BankAccountConfig::getBankName, account.getBankName())
                    .eq(BankAccountConfig::getCurrency, account.getCurrency())
                    .exists();
            if (exists) {
                return Result.fail("该银行账号、银行名称和币种的组合已存在");
            }
        }
        
        // 如果设置为默认账户，需要取消其他默认账户
        if (account.getIsDefault() != null && account.getIsDefault() == 1) {
            bankAccountConfigService.lambdaUpdate()
                    .set(BankAccountConfig::getIsDefault, 0)
                    .update();
        }
        
        boolean saved = bankAccountConfigService.save(account);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    /**
     * 修改银行账户配置
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改银行账户配置")
    @RequiresPermissions("system:bank-account:update")
    public Result<Void> updateBankAccount(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @RequestBody BankAccountConfig account) {
        
        BankAccountConfig existing = bankAccountConfigService.getById(id);
        if (existing == null) {
            return Result.fail("银行账户不存在");
        }
        
        // 检查银行账号+银行名称+币种的组合是否重复（排除自己）
        if (account.getAccountNumber() != null && !account.getAccountNumber().isEmpty() && 
            account.getBankName() != null && !account.getBankName().isEmpty() &&
            account.getCurrency() != null && !account.getCurrency().isEmpty()) {
            
            boolean exists = bankAccountConfigService.lambdaQuery()
                    .eq(BankAccountConfig::getAccountNumber, account.getAccountNumber())
                    .eq(BankAccountConfig::getBankName, account.getBankName())
                    .eq(BankAccountConfig::getCurrency, account.getCurrency())
                    .ne(BankAccountConfig::getId, id)
                    .exists();
            if (exists) {
                return Result.fail("该银行账号、银行名称和币种的组合已存在");
            }
        }
        
        // 如果设置为默认账户，需要取消其他默认账户
        if (account.getIsDefault() != null && account.getIsDefault() == 1) {
            bankAccountConfigService.lambdaUpdate()
                    .set(BankAccountConfig::getIsDefault, 0)
                    .ne(BankAccountConfig::getId, id)
                    .update();
        }
        
        account.setId(id);
        boolean updated = bankAccountConfigService.updateById(account);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    /**
     * 删除银行账户配置
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除银行账户配置")
    @RequiresPermissions("system:bank-account:delete")
    public Result<Void> deleteBankAccount(@Parameter(description = "账户ID") @PathVariable Long id) {
        BankAccountConfig account = bankAccountConfigService.getById(id);
        if (account == null) {
            return Result.fail("银行账户不存在");
        }
        
        boolean removed = bankAccountConfigService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 启用/禁用银行账户配置
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用银行账户配置")
    @RequiresPermissions("system:bank-account:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "状态 0-禁用 1-启用") @RequestParam Integer status) {
        
        BankAccountConfig account = bankAccountConfigService.getById(id);
        if (account == null) {
            return Result.fail("银行账户不存在");
        }
        
        account.setStatus(status);
        boolean updated = bankAccountConfigService.updateById(account);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }

    /**
     * 设置默认银行账户
     */
    @PostMapping("/{id}/set-default")
    @Operation(summary = "设置默认银行账户")
    @RequiresPermissions("system:bank-account:update")
    public Result<Void> setDefault(@Parameter(description = "账户ID") @PathVariable Long id) {
        BankAccountConfig account = bankAccountConfigService.getById(id);
        if (account == null) {
            return Result.fail("银行账户不存在");
        }
        
        // 取消所有默认账户
        bankAccountConfigService.lambdaUpdate()
                .set(BankAccountConfig::getIsDefault, 0)
                .update();
        
        // 设置当前账户为默认
        account.setIsDefault(1);
        boolean updated = bankAccountConfigService.updateById(account);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("设置失败");
        }
    }
}