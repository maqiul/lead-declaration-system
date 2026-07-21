package com.declaration.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.TradeTerm;
import com.declaration.service.TradeTermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 贸易方式(Incoterms)配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/trade-terms")
@RequiredArgsConstructor
@Tag(name = "贸易方式配置管理", description = "贸易方式(Incoterms)配置相关接口")
public class TradeTermController {

    private final TradeTermService tradeTermService;

    @GetMapping
    @Operation(summary = "分页查询贸易方式")
    @RequiresPermissions("system:tradeterm:view")
    public Result<Page<TradeTerm>> getTradeTerms(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        Page<TradeTerm> result = tradeTermService.getPage(page, size, keyword, status);
        return Result.success(result);
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的贸易方式")
    public Result<List<TradeTerm>> getEnabledTradeTerms() {
        List<TradeTerm> list = tradeTermService.getEnabledList();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取贸易方式")
    @RequiresPermissions("system:tradeterm:view")
    public Result<TradeTerm> getTradeTermById(@Parameter(description = "贸易方式ID") @PathVariable Long id) {
        TradeTerm tradeTerm = tradeTermService.getById(id);
        if (tradeTerm == null) {
            return Result.fail("贸易方式不存在");
        }
        return Result.success(tradeTerm);
    }

    @PostMapping
    @Operation(summary = "新增贸易方式")
    @RequiresPermissions("system:tradeterm:create")
    public Result<Void> addTradeTerm(@RequestBody TradeTerm tradeTerm) {
        boolean codeExists = tradeTermService.lambdaQuery()
                .eq(TradeTerm::getCode, tradeTerm.getCode())
                .exists();
        if (codeExists) {
            return Result.fail("贸易方式代码已存在");
        }

        boolean saved = tradeTermService.saveTradeTerm(tradeTerm);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改贸易方式")
    @RequiresPermissions("system:tradeterm:update")
    public Result<Void> updateTradeTerm(
            @Parameter(description = "贸易方式ID") @PathVariable Long id,
            @RequestBody TradeTerm tradeTerm) {

        TradeTerm existing = tradeTermService.getById(id);
        if (existing == null) {
            return Result.fail("贸易方式不存在");
        }

        boolean codeExists = tradeTermService.lambdaQuery()
                .eq(TradeTerm::getCode, tradeTerm.getCode())
                .ne(TradeTerm::getId, id)
                .exists();
        if (codeExists) {
            return Result.fail("贸易方式代码已存在");
        }

        tradeTerm.setId(id);
        tradeTerm.setCode(existing.getCode()); // code 不允许修改
        boolean updated = tradeTermService.updateTradeTerm(tradeTerm);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除贸易方式")
    @RequiresPermissions("system:tradeterm:delete")
    public Result<Void> deleteTradeTerm(@Parameter(description = "贸易方式ID") @PathVariable Long id) {
        TradeTerm tradeTerm = tradeTermService.getById(id);
        if (tradeTerm == null) {
            return Result.fail("贸易方式不存在");
        }

        boolean removed = tradeTermService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用贸易方式")
    @RequiresPermissions("system:tradeterm:update")
    public Result<Void> toggleStatus(
            @Parameter(description = "贸易方式ID") @PathVariable Long id) {

        TradeTerm tradeTerm = tradeTermService.getById(id);
        if (tradeTerm == null) {
            return Result.fail("贸易方式不存在");
        }

        tradeTerm.setStatus(tradeTerm.getStatus() == 1 ? 0 : 1);
        boolean updated = tradeTermService.updateById(tradeTerm);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }
}
