package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.CustomerConfig;
import com.declaration.service.CustomerConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 常用客户配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "常用客户管理", description = "常用客户配置相关接口")
public class CustomerConfigController {

    private final CustomerConfigService customerConfigService;

    /**
     * 分页查询当前用户的客户
     */
    @GetMapping
    @Operation(summary = "分页查询当前用户的客户")
    @RequiresPermissions("customer:config:view")
    public Result<Page<CustomerConfig>> getCustomers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {

        Long userId = StpUtil.getLoginIdAsLong();
        Page<CustomerConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<CustomerConfig> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(CustomerConfig::getUserId, userId);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(CustomerConfig::getCustomerName, keyword);
        }

        wrapper.orderByAsc(CustomerConfig::getSort)
               .orderByDesc(CustomerConfig::getCreateTime);

        Page<CustomerConfig> result = customerConfigService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取当前用户所有启用客户（表单下拉用）
     */
    @GetMapping("/all")
    @Operation(summary = "获取当前用户所有启用客户")
    public Result<List<CustomerConfig>> getAllEnabled() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<CustomerConfig> list = customerConfigService.getEnabledListByUserId(userId);
        return Result.success(list);
    }

    /**
     * 获取客户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取客户详情")
    @RequiresPermissions("customer:config:view")
    public Result<CustomerConfig> getById(@Parameter(description = "客户ID") @PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CustomerConfig config = customerConfigService.getById(id);
        if (config == null || !config.getUserId().equals(userId)) {
            return Result.fail("客户不存在");
        }
        return Result.success(config);
    }

    /**
     * 新增客户
     */
    @PostMapping
    @Operation(summary = "新增客户")
    @RequiresPermissions("customer:config:add")
    public Result<Void> add(@RequestBody CustomerConfig config) {
        Long userId = StpUtil.getLoginIdAsLong();
        config.setUserId(userId);

        boolean saved = customerConfigService.save(config);
        return saved ? Result.success() : Result.fail("新增失败");
    }

    /**
     * 修改客户
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改客户")
    @RequiresPermissions("customer:config:update")
    public Result<Void> update(
            @Parameter(description = "客户ID") @PathVariable Long id,
            @RequestBody CustomerConfig config) {

        Long userId = StpUtil.getLoginIdAsLong();
        CustomerConfig existing = customerConfigService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail("客户不存在");
        }

        config.setId(id);
        boolean updated = customerConfigService.updateById(config);
        return updated ? Result.success() : Result.fail("修改失败");
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户")
    @RequiresPermissions("customer:config:delete")
    public Result<Void> delete(@Parameter(description = "客户ID") @PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        CustomerConfig existing = customerConfigService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail("客户不存在");
        }

        boolean removed = customerConfigService.removeById(id);
        return removed ? Result.success() : Result.fail("删除失败");
    }
}
