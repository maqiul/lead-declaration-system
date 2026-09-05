package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.PartyBConfig;
import com.declaration.service.PartyBConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 乙方配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/party-b-configs")
@RequiredArgsConstructor
@Tag(name = "乙方配置管理", description = "乙方配置相关接口")
public class PartyBConfigController {

    private final PartyBConfigService partyBConfigService;

    /**
     * 分页查询当前用户的乙方
     */
    @GetMapping
    @Operation(summary = "分页查询当前用户的乙方")
    @RequiresPermissions("party-b:config:view")
    public Result<Page<PartyBConfig>> getPartyBList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {

        Long userId = StpUtil.getLoginIdAsLong();
        Page<PartyBConfig> page = new Page<>(current, size);
        LambdaQueryWrapper<PartyBConfig> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(PartyBConfig::getUserId, userId);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(PartyBConfig::getPartyBName, keyword);
        }

        wrapper.orderByAsc(PartyBConfig::getSort)
               .orderByDesc(PartyBConfig::getCreateTime);

        Page<PartyBConfig> result = partyBConfigService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取当前用户所有启用乙方（表单下拉用）
     */
    @GetMapping("/all")
    @Operation(summary = "获取当前用户所有启用乙方")
    public Result<List<PartyBConfig>> getAllEnabled() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<PartyBConfig> list = partyBConfigService.getEnabledListByUserId(userId);
        return Result.success(list);
    }

    /**
     * 获取乙方详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取乙方详情")
    @RequiresPermissions("party-b:config:view")
    public Result<PartyBConfig> getById(@Parameter(description = "乙方ID") @PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        PartyBConfig config = partyBConfigService.getById(id);
        if (config == null || !config.getUserId().equals(userId)) {
            return Result.fail("乙方不存在");
        }
        return Result.success(config);
    }

    /**
     * 新增乙方
     */
    @PostMapping
    @Operation(summary = "新增乙方")
    @RequiresPermissions("party-b:config:add")
    public Result<Void> add(@RequestBody PartyBConfig config) {
        Long userId = StpUtil.getLoginIdAsLong();
        config.setUserId(userId);

        boolean saved = partyBConfigService.save(config);
        return saved ? Result.success() : Result.fail("新增失败");
    }

    /**
     * 修改乙方
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改乙方")
    @RequiresPermissions("party-b:config:update")
    public Result<Void> update(
            @Parameter(description = "乙方ID") @PathVariable Long id,
            @RequestBody PartyBConfig config) {

        Long userId = StpUtil.getLoginIdAsLong();
        PartyBConfig existing = partyBConfigService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail("乙方不存在");
        }

        config.setId(id);
        boolean updated = partyBConfigService.updateById(config);
        return updated ? Result.success() : Result.fail("修改失败");
    }

    /**
     * 删除乙方
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除乙方")
    @RequiresPermissions("party-b:config:delete")
    public Result<Void> delete(@Parameter(description = "乙方ID") @PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        PartyBConfig existing = partyBConfigService.getById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return Result.fail("乙方不存在");
        }

        boolean removed = partyBConfigService.removeById(id);
        return removed ? Result.success() : Result.fail("删除失败");
    }
}
