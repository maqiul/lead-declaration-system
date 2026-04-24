package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.MeasurementUnit;
import com.declaration.service.MeasurementUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 计量单位配置控制器
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Slf4j
@RestController
@RequestMapping("v1/system/measurement-units")
@RequiredArgsConstructor
@Tag(name = "计量单位管理", description = "计量单位配置管理接口")
public class MeasurementUnitController {

    private final MeasurementUnitService measurementUnitService;

    @GetMapping()
    @Operation(summary = "获取计量单位列表（分页）")
    @RequiresPermissions("system:measurement-unit:view")
    public Result<Page<MeasurementUnit>> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<MeasurementUnit> page = new Page<>(pageNum, pageSize);
        
        LambdaQueryWrapper<MeasurementUnit> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(MeasurementUnit::getUnitCode, keyword)
                .or()
                .like(MeasurementUnit::getUnitName, keyword)
                .or()
                .like(MeasurementUnit::getUnitNameEn, keyword)
                .or()
                .like(MeasurementUnit::getUnitNameEnSingular, keyword)
            );
        }
        
        // 单位类型筛选
        if (StringUtils.hasText(unitType)) {
            wrapper.eq(MeasurementUnit::getUnitType, unitType);
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(MeasurementUnit::getStatus, status);
        }
        
        // 按排序字段升序
        wrapper.orderByAsc(MeasurementUnit::getSort);
        
        Page<MeasurementUnit> resultPage = measurementUnitService.page(page, wrapper);
        return Result.success(resultPage);
    }

    @GetMapping("/active")
    @Operation(summary = "获取启用的计量单位")
    public Result<List<MeasurementUnit>> getActiveList() {
        List<MeasurementUnit> units = measurementUnitService.getActiveUnits();
        return Result.success(units);
    }

    @GetMapping("/code/{unitCode}")
    @Operation(summary = "根据代码获取计量单位")
    public Result<MeasurementUnit> getByCode(@PathVariable String unitCode) {
        MeasurementUnit unit = measurementUnitService.getByUnitCode(unitCode);
        return Result.success(unit);
    }

    @PostMapping
    @Operation(summary = "新增计量单位")
    @RequiresPermissions("system:measurement-unit:create")
    public Result<Boolean> add(@RequestBody MeasurementUnit unit) {
        // 校验单位代码全系统唯一
        if (StringUtils.hasText(unit.getUnitCode())) {
            long codeCount = measurementUnitService.count(new LambdaQueryWrapper<MeasurementUnit>()
                    .eq(MeasurementUnit::getUnitCode, unit.getUnitCode()));
            if (codeCount > 0) {
                return Result.fail("单位代码已存在");
            }
        }
        
        // 校验中文名称全系统唯一
        if (StringUtils.hasText(unit.getUnitName())) {
            long nameCount = measurementUnitService.count(new LambdaQueryWrapper<MeasurementUnit>()
                    .eq(MeasurementUnit::getUnitName, unit.getUnitName()));
            if (nameCount > 0) {
                return Result.fail("单位中文名称已存在");
            }
        }

        boolean success = measurementUnitService.save(unit);
        log.info("新增计量单位：{}", unit);
        return Result.success(success);
    }

    @PutMapping
    @Operation(summary = "更新计量单位")
    @RequiresPermissions("system:measurement-unit:update")
    public Result<Boolean> update(@RequestBody MeasurementUnit unit) {
        // 校验单位代码，排除自身
        if (StringUtils.hasText(unit.getUnitCode()) && unit.getId() != null) {
            long codeCount = measurementUnitService.count(new LambdaQueryWrapper<MeasurementUnit>()
                    .eq(MeasurementUnit::getUnitCode, unit.getUnitCode())
                    .ne(MeasurementUnit::getId, unit.getId()));
            if (codeCount > 0) {
                return Result.fail("单位代码已存在");
            }
        }
        
        // 校验中文名称，排除自身
        if (StringUtils.hasText(unit.getUnitName()) && unit.getId() != null) {
            long nameCount = measurementUnitService.count(new LambdaQueryWrapper<MeasurementUnit>()
                    .eq(MeasurementUnit::getUnitName, unit.getUnitName())
                    .ne(MeasurementUnit::getId, unit.getId()));
            if (nameCount > 0) {
                return Result.fail("单位中文名称已存在");
            }
        }

        boolean success = measurementUnitService.updateById(unit);
        log.info("更新计量单位：{}", unit);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除计量单位")
    @RequiresPermissions("system:measurement-unit:delete")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean success = measurementUnitService.removeById(id);
        log.info("删除计量单位：id={}", id);
        return Result.success(success);
    }
}
