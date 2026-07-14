package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.SysDict;
import com.declaration.entity.SysDictItem;
import com.declaration.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统字典管理控制器
 *
 * @author Administrator
 * @since 2026-07-07
 */
@Slf4j
@RestController
@RequestMapping("/v1/sys-dicts")
@RequiredArgsConstructor
@Tag(name = "系统字典管理", description = "字典类型与字典项的增删改查")
public class SysDictController {

    private final SysDictService sysDictService;

    // ============================================================
    // 字典类型 CRUD
    // ============================================================

    /**
     * 获取字典类型列表
     */
    @GetMapping
    @Operation(summary = "获取字典类型列表")
    @RequiresPermissions("system:dict:view")
    public Result<List<SysDict>> list() {
        return Result.success(sysDictService.listAll());
    }

    /**
     * 创建字典类型
     */
    @PostMapping
    @Operation(summary = "创建字典类型")
    @RequiresPermissions("system:dict:add")
    public Result<Long> create(@RequestBody SysDict dict) {
        if (dict.getDictCode() == null || dict.getDictCode().trim().isEmpty()) {
            return Result.fail("字典编码不能为空");
        }
        if (dict.getDictName() == null || dict.getDictName().trim().isEmpty()) {
            return Result.fail("字典名称不能为空");
        }
        Long id = sysDictService.createDict(dict);
        return Result.success(id);
    }

    /**
     * 更新字典类型
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新字典类型")
    @RequiresPermissions("system:dict:update")
    public Result<Void> update(
            @Parameter(description = "字典ID") @PathVariable Long id,
            @RequestBody SysDict dict) {
        sysDictService.updateDict(id, dict);
        return Result.success();
    }

    /**
     * 删除字典类型（含级联删除项）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除字典类型")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> delete(
            @Parameter(description = "字典ID") @PathVariable Long id) {
        sysDictService.deleteDict(id);
        return Result.success();
    }

    // ============================================================
    // 字典项 CRUD
    // ============================================================

    /**
     * 获取字典项列表（按字典编码）
     */
    @GetMapping("/{code}/items")
    @Operation(summary = "获取字典项列表")
    @RequiresPermissions("system:dict:view")
    public Result<List<SysDictItem>> listItems(
            @Parameter(description = "字典编码") @PathVariable String code) {
        return Result.success(sysDictService.listItemsByDictCode(code));
    }

    /**
     * 创建字典项
     */
    @PostMapping("/{code}/items")
    @Operation(summary = "创建字典项")
    @RequiresPermissions("system:dict:add")
    public Result<Long> createItem(
            @Parameter(description = "字典编码") @PathVariable String code,
            @RequestBody SysDictItem item) {
        item.setDictCode(code);
        if (item.getItemValue() == null || item.getItemValue().trim().isEmpty()) {
            return Result.fail("字典项值不能为空");
        }
        if (item.getItemLabel() == null || item.getItemLabel().trim().isEmpty()) {
            return Result.fail("字典项文本不能为空");
        }
        Long id = sysDictService.createItem(item);
        return Result.success(id);
    }

    /**
     * 更新字典项
     */
    @PutMapping("/items/{id}")
    @Operation(summary = "更新字典项")
    @RequiresPermissions("system:dict:update")
    public Result<Void> updateItem(
            @Parameter(description = "字典项ID") @PathVariable Long id,
            @RequestBody SysDictItem item) {
        sysDictService.updateItem(id, item);
        return Result.success();
    }

    /**
     * 删除字典项
     */
    @DeleteMapping("/items/{id}")
    @Operation(summary = "删除字典项")
    @RequiresPermissions("system:dict:delete")
    public Result<Void> deleteItem(
            @Parameter(description = "字典项ID") @PathVariable Long id) {
        sysDictService.deleteItem(id);
        return Result.success();
    }

    // ============================================================
    // 公开接口：按字典编码获取启用项（用于前端下拉选择）
    // ============================================================

    /**
     * 公开接口：按字典编码获取启用状态的字典项
     */
    @GetMapping("/items")
    @Operation(summary = "按字典编码获取启用状态的字典项（公开接口）")
    public Result<List<SysDictItem>> getEnabledItems(
            @Parameter(description = "字典编码") @RequestParam String code) {
        return Result.success(sysDictService.listEnabledItems(code));
    }
}
