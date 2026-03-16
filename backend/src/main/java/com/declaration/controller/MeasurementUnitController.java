package com.declaration.controller;

import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.MeasurementUnit;
import com.declaration.service.MeasurementUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 计量单位配置控制器
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("/system/measurement-units")
@RequiredArgsConstructor
@Tag(name = "计量单位管理", description = "计量单位配置管理接口")
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    @GetMapping("/list")
    @Operation(summary = "获取计量单位列表")
    @RequiresPermissions("system:measurement-unit:list")
    public Result<List<MeasurementUnit>> getList() {
        List<MeasurementUnit> units = measurementUnitService.list();
        return Result.success(units);
    }

    @GetMapping("/active")
    @Operation(summary = "获取启用的计量单位")
    @RequiresPermissions("system:measurement-unit:list")
    public Result<List<MeasurementUnit>> getActiveList() {
        List<MeasurementUnit> units = measurementUnitService.getActiveUnits();
        return Result.success(units);
    }

    @GetMapping("/code/{unitCode}")
    @Operation(summary = "根据代码获取计量单位")
    @RequiresPermissions("system:measurement-unit:query")
    public Result<MeasurementUnit> getByCode(@PathVariable String unitCode) {
        MeasurementUnit unit = measurementUnitService.getByUnitCode(unitCode);
        return Result.success(unit);
    }

    @PostMapping
    @Operation(summary = "新增计量单位")
    @RequiresPermissions("system:measurement-unit:add")
    public Result<Boolean> add(@RequestBody MeasurementUnit unit) {
        boolean success = measurementUnitService.save(unit);
        return Result.success(success);
    }

    @PutMapping
    @Operation(summary = "更新计量单位")
    @RequiresPermissions("system:measurement-unit:update")
    public Result<Boolean> update(@RequestBody MeasurementUnit unit) {
        boolean success = measurementUnitService.updateById(unit);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除计量单位")
    @RequiresPermissions("system:measurement-unit:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = measurementUnitService.removeById(id);
        return Result.success(success);
    }
}