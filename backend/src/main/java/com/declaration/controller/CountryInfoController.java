package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.CountryInfo;
import com.declaration.service.CountryInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 国家信息管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/countries")
@RequiredArgsConstructor
@Tag(name = "国家信息管理", description = "国家信息相关接口")
public class CountryInfoController {

    private final CountryInfoService countryInfoService;

    /**
     * 分页查询国家信息
     */
    @GetMapping
    @Operation(summary = "分页查询国家信息")
    @RequiresPermissions("system:country:list")
    public Result<Page<CountryInfo>> getCountries(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        
        Page<CountryInfo> page = new Page<>(current, size);
        LambdaQueryWrapper<CountryInfo> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(CountryInfo::getCountryCode, keyword)
                   .or()
                   .like(CountryInfo::getChineseName, keyword)
                   .or()
                   .like(CountryInfo::getEnglishName, keyword)
                   .or()
                   .like(CountryInfo::getAbbreviation, keyword);
        }
        
        wrapper.eq(CountryInfo::getStatus, 1)
               .orderByAsc(CountryInfo::getSort)
               .orderByDesc(CountryInfo::getCreateTime);
        
        Page<CountryInfo> result = countryInfoService.page(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有启用的国家信息（用于下拉选择）
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的国家信息")
    public Result<List<CountryInfo>> getEnabledCountries() {
        List<CountryInfo> countries = countryInfoService.getEnabledList();
        return Result.success(countries);
    }

    /**
     * 根据ID获取国家信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取国家信息")
    @RequiresPermissions("system:country:view")
    public Result<CountryInfo> getCountryById(@Parameter(description = "国家ID") @PathVariable Long id) {
        CountryInfo country = countryInfoService.getById(id);
        if (country == null) {
            return Result.fail("国家信息不存在");
        }
        return Result.success(country);
    }

    /**
     * 新增国家信息
     */
    @PostMapping
    @Operation(summary = "新增国家信息")
    @RequiresPermissions("system:country:add")
    public Result<Void> addCountry(@RequestBody CountryInfo country) {
        // 检查国家代码是否重复
        boolean exists = countryInfoService.lambdaQuery()
                .eq(CountryInfo::getCountryCode, country.getCountryCode())
                .exists();
        if (exists) {
            return Result.fail("国家代码已存在");
        }
        
        // 检查简称是否重复
        if (country.getAbbreviation() != null && !country.getAbbreviation().isEmpty()) {
            boolean abbrExists = countryInfoService.lambdaQuery()
                    .eq(CountryInfo::getAbbreviation, country.getAbbreviation())
                    .exists();
            if (abbrExists) {
                return Result.fail("国家简称已存在");
            }
        }
        
        // 验证国家代码格式（3位大写字母）
        if (country.getCountryCode() == null || !country.getCountryCode().matches("^[A-Z]{3}$")) {
            return Result.fail("国家代码必须是3位大写字母");
        }
        
        // 验证简称格式（3位大写字母）
        if (country.getAbbreviation() != null && !country.getAbbreviation().isEmpty() 
            && !country.getAbbreviation().matches("^[A-Z]{3}$")) {
            return Result.fail("国家简称必须是3位大写字母");
        }
        
        boolean saved = countryInfoService.save(country);
        if (saved) {
            return Result.success();
        } else {
            return Result.fail("新增失败");
        }
    }

    /**
     * 修改国家信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改国家信息")
    @RequiresPermissions("system:country:edit")
    public Result<Void> updateCountry(
            @Parameter(description = "国家ID") @PathVariable Long id,
            @RequestBody CountryInfo country) {
        
        CountryInfo existing = countryInfoService.getById(id);
        if (existing == null) {
            return Result.fail("国家信息不存在");
        }
        
        // 检查国家代码是否重复（排除自己）
        boolean exists = countryInfoService.lambdaQuery()
                .eq(CountryInfo::getCountryCode, country.getCountryCode())
                .ne(CountryInfo::getId, id)
                .exists();
        if (exists) {
            return Result.fail("国家代码已存在");
        }
        
        // 检查简称是否重复（排除自己）
        if (country.getAbbreviation() != null && !country.getAbbreviation().isEmpty()) {
            boolean abbrExists = countryInfoService.lambdaQuery()
                    .eq(CountryInfo::getAbbreviation, country.getAbbreviation())
                    .ne(CountryInfo::getId, id)
                    .exists();
            if (abbrExists) {
                return Result.fail("国家简称已存在");
            }
        }
        
        country.setId(id);
        boolean updated = countryInfoService.updateById(country);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("修改失败");
        }
    }

    /**
     * 删除国家信息
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除国家信息")
    @RequiresPermissions("system:country:delete")
    public Result<Void> deleteCountry(@Parameter(description = "国家ID") @PathVariable Long id) {
        CountryInfo country = countryInfoService.getById(id);
        if (country == null) {
            return Result.fail("国家信息不存在");
        }
        
        boolean removed = countryInfoService.removeById(id);
        if (removed) {
            return Result.success();
        } else {
            return Result.fail("删除失败");
        }
    }

    /**
     * 启用/禁用国家信息
     */
    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "启用/禁用国家信息")
    @RequiresPermissions("system:country:edit")
    public Result<Void> toggleStatus(
            @Parameter(description = "国家ID") @PathVariable Long id,
            @Parameter(description = "状态 0-禁用 1-启用") @RequestParam Integer status) {
        
        CountryInfo country = countryInfoService.getById(id);
        if (country == null) {
            return Result.fail("国家信息不存在");
        }
        
        country.setStatus(status);
        boolean updated = countryInfoService.updateById(country);
        if (updated) {
            return Result.success();
        } else {
            return Result.fail("操作失败");
        }
    }
}