package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.ProductTypeConfig;
import com.declaration.service.ProductTypeConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * HS商品类型配置控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/system/product")
@RequiredArgsConstructor
@Tag(name = "HS商品维护", description = "HS商品类型配置接口")
public class ProductTypeConfigController {

    private final ProductTypeConfigService productTypeConfigService;
    private final ObjectMapper objectMapper;

    @GetMapping("/list")
    @Operation(summary = "获取商品类型列表")
    @RequiresPermissions("system:product:list")
    public Result<IPage<ProductTypeConfig>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        try {
            IPage<ProductTypeConfig> page = productTypeConfigService.getPage(pageNum, pageSize, keyword);
            return Result.success(page);
        } catch (Exception e) {
            log.error("获取商品类型列表失败", e);
            return Result.fail("获取列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的商品类型")
    @RequiresPermissions("system:product:list")
    public Result<List<ProductTypeConfig>> getEnabledList() {
        try {
            List<ProductTypeConfig> list = productTypeConfigService.getEnabledList();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取启用商品类型列表失败", e);
            return Result.fail("获取列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/types")
    @Operation(summary = "获取商品类型列表（用于下拉选择）")
    @RequiresPermissions("system:product:list")
    public Result<List<ProductTypeConfig>> getProductTypes() {
        try {
            List<ProductTypeConfig> list = productTypeConfigService.lambdaQuery()
                    .eq(ProductTypeConfig::getStatus, 1)
                    .orderByAsc(ProductTypeConfig::getSort)
                    .list();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取商品类型列表失败", e);
            return Result.fail("获取列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取商品类型详情")
    @RequiresPermissions("system:product:query")
    public Result<ProductTypeConfig> getById(@PathVariable Long id) {
        ProductTypeConfig config = productTypeConfigService.getById(id);
        if (config == null) {
            return Result.fail("数据不存在");
        }
        // 解析申报要素
        if (config.getElementsConfig() != null) {
            try {
                List<?> elements = objectMapper.readValue(config.getElementsConfig(), List.class);
                config.setElements((List) elements);
            } catch (JsonProcessingException e) {
                log.error("解析申报要素失败", e);
            }
        }
        return Result.success(config);
    }

    @PostMapping
    @Operation(summary = "新增商品类型")
    @RequiresPermissions("system:product:add")
    public Result<String> add(@RequestBody ProductTypeConfig config) {
        try {
            // 序列化申报要素
            if (config.getElements() != null && !config.getElements().isEmpty()) {
                config.setElementsConfig(objectMapper.writeValueAsString(config.getElements()));
            }
            
            // 检查HS编码是否已存在
            ProductTypeConfig existing = productTypeConfigService.getByHsCode(config.getHsCode());
            if (existing != null) {
                return Result.fail("HS编码已存在");
            }
            
            config.setCreateBy(StpUtil.getLoginIdAsLong());
            boolean result = productTypeConfigService.save(config);
            if (result) {
                return Result.success("新增成功", "新增成功");
            } else {
                return Result.fail("新增失败");
            }
        } catch (Exception e) {
            log.error("新增商品类型失败", e);
            return Result.fail("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新商品类型")
    @RequiresPermissions("system:product:update")
    public Result<String> update(@PathVariable Long id, @RequestBody ProductTypeConfig config) {
        try {
            config.setId(id);
            
            // 序列化申报要素
            if (config.getElements() != null && !config.getElements().isEmpty()) {
                config.setElementsConfig(objectMapper.writeValueAsString(config.getElements()));
            }
            
            config.setUpdateBy(StpUtil.getLoginIdAsLong());
            boolean result = productTypeConfigService.updateById(config);
            if (result) {
                return Result.success("更新成功", "更新成功");
            } else {
                return Result.fail("更新失败");
            }
        } catch (Exception e) {
            log.error("更新商品类型失败", e);
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品类型")
    @RequiresPermissions("system:product:delete")
    public Result<String> delete(@PathVariable Long id) {
        try {
            boolean result = productTypeConfigService.removeById(id);
            if (result) {
                return Result.success("删除成功", "删除成功");
            } else {
                return Result.fail("删除失败");
            }
        } catch (Exception e) {
            log.error("删除商品类型失败", e);
            return Result.fail("删除失败：" + e.getMessage());
        }
    }
}
