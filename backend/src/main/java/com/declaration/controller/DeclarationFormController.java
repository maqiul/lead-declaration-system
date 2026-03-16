package com.declaration.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationCarton;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationProduct;
import com.declaration.service.DeclarationCartonProductService;
import com.declaration.service.DeclarationCartonService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 申报单管理控制器 - RESTful API规范
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/declarations")
@RequiredArgsConstructor
@Tag(name = "申报单管理", description = "出口申报单相关接口")
public class DeclarationFormController {

    private final DeclarationFormService declarationFormService;
    private final DeclarationProductService declarationProductService;
    private final DeclarationCartonService declarationCartonService;
    private final DeclarationCartonProductService declarationCartonProductService;

    /**
     * 分页查询申报单列表
     */
    @GetMapping
    @Operation(summary = "分页查询申报单")
    @RequiresPermissions("business:declaration:list")
    public Result<IPage<DeclarationForm>> getDeclarations(
            @Parameter(description = "分页参数") PageParam pageParam,
            @Parameter(description = "申报单号") @RequestParam(required = false) String formNo,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        
        Page<DeclarationForm> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        IPage<DeclarationForm> result = declarationFormService.page(page);
        return Result.success(result);
    }

    /**
     * 获取申报单详情（包含产品和箱子信息）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取申报单详情")
    @RequiresPermissions("business:declaration:query")
    public Result<DeclarationForm> getDeclaration(@Parameter(description = "申报单ID") @PathVariable Long id) {
        DeclarationForm form = declarationFormService.getFullDeclarationForm(id);
        return Result.success(form);
    }

    /**
     * 创建申报单
     */
    @PostMapping
    @Operation(summary = "创建申报单")
    @RequiresPermissions("business:declaration:add")
    public Result<Long> createDeclaration(@Valid @RequestBody DeclarationForm form) {
        // 生成申报单号
        if (form.getFormNo() == null || form.getFormNo().isEmpty()) {
            form.setFormNo(generateFormNo());
        }
        
        // 如果前端未提供发票号，则按规则生成
        if (form.getInvoiceNo() == null || form.getInvoiceNo().isEmpty()) {
            // 生成规则：ZIYI-yy-mmdd
            String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yy-MMdd"));
            form.setInvoiceNo("ZIYI-" + currentDate);
        }
        
        boolean saved = declarationFormService.saveDeclarationForm(form);
        return Result.success(form.getId());
    }

    /**
     * 更新申报单
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新申报单")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> updateDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            @Valid @RequestBody DeclarationForm form) {
        form.setId(id);
        declarationFormService.updateById(form);
        return Result.success();
    }

    /**
     * 删除申报单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除申报单")
    @RequiresPermissions("business:declaration:delete")
    public Result<Void> deleteDeclaration(@Parameter(description = "申报单ID") @PathVariable Long id) {
        declarationFormService.removeById(id);
        return Result.success();
    }

    /**
     * 提交申报单
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交申报单")
    @RequiresPermissions("business:declaration:submit")
    public Result<Void> submitDeclaration(@Parameter(description = "申报单ID") @PathVariable Long id) {
        DeclarationForm form = declarationFormService.getById(id);
        if (form != null) {
            form.setStatus(1); // 已提交状态
            declarationFormService.updateById(form);
        }
        return Result.success();
    }

    /**
     * 获取申报单产品列表
     */
    @GetMapping("/{formId}/products")
    @Operation(summary = "获取申报单产品列表")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationProduct>> getProducts(@Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<DeclarationProduct> products = declarationProductService.lambdaQuery()
                .eq(DeclarationProduct::getFormId, formId)
                .orderByAsc(DeclarationProduct::getSortOrder)
                .list();
        return Result.success(products);
    }

    /**
     * 保存申报单产品
     */
    @PostMapping("/{formId}/products")
    @Operation(summary = "保存申报单产品")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveProducts(
            @Parameter(description = "申报单ID") @PathVariable Long formId,
            @Valid @RequestBody List<DeclarationProduct> products) {
        
        // 先删除原有产品
        declarationProductService.lambdaUpdate()
                .eq(DeclarationProduct::getFormId, formId)
                .remove();
        
        // 保存新产品
        if (products != null && !products.isEmpty()) {
            for (int i = 0; i < products.size(); i++) {
                DeclarationProduct product = products.get(i);
                product.setFormId(formId);
                product.setSortOrder(i);
                declarationProductService.save(product);
            }
        }
        
        return Result.success();
    }

    /**
     * 获取申报单箱子列表
     */
    @GetMapping("/{formId}/cartons")
    @Operation(summary = "获取申报单箱子列表")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationCarton>> getCartons(@Parameter(description = "申报单ID") @PathVariable Long formId) {
        List<DeclarationCarton> cartons = declarationCartonService.lambdaQuery()
                .eq(DeclarationCarton::getFormId, formId)
                .orderByAsc(DeclarationCarton::getSortOrder)
                .list();
        return Result.success(cartons);
    }

    /**
     * 保存申报单箱子
     */
    @PostMapping("/{formId}/cartons")
    @Operation(summary = "保存申报单箱子")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveCartons(
            @Parameter(description = "申报单ID") @PathVariable Long formId,
            @Valid @RequestBody List<DeclarationCarton> cartons) {
        
        // 先删除原有箱子
        declarationCartonService.lambdaUpdate()
                .eq(DeclarationCarton::getFormId, formId)
                .remove();
        
        // 保存新箱子
        if (cartons != null && !cartons.isEmpty()) {
            for (int i = 0; i < cartons.size(); i++) {
                DeclarationCarton carton = cartons.get(i);
                carton.setFormId(formId);
                carton.setSortOrder(i);
                declarationCartonService.save(carton);
            }
        }
        
        return Result.success();
    }

    /**
     * 获取箱子产品关联
     */
    @GetMapping("/carton-products/{cartonId}")
    @Operation(summary = "获取箱子产品关联")
    @RequiresPermissions("business:declaration:list")
    public Result<List<DeclarationCartonProduct>> getCartonProducts(
            @Parameter(description = "箱子ID") @PathVariable Long cartonId) {
        List<DeclarationCartonProduct> cartonProducts = declarationCartonProductService.lambdaQuery()
                .eq(DeclarationCartonProduct::getCartonId, cartonId)
                .list();
        return Result.success(cartonProducts);
    }

    /**
     * 保存箱子产品关联
     */
    @PostMapping("/carton-products")
    @Operation(summary = "保存箱子产品关联")
    @RequiresPermissions("business:declaration:update")
    public Result<Void> saveCartonProducts(@Valid @RequestBody List<DeclarationCartonProduct> cartonProducts) {
        
        // 先删除原有关联
        if (cartonProducts != null && !cartonProducts.isEmpty()) {
            Long cartonId = cartonProducts.get(0).getCartonId();
            declarationCartonProductService.lambdaUpdate()
                    .eq(DeclarationCartonProduct::getCartonId, cartonId)
                    .remove();
        }
        
        // 保存新关联
        if (cartonProducts != null && !cartonProducts.isEmpty()) {
            for (DeclarationCartonProduct cartonProduct : cartonProducts) {
                declarationCartonProductService.save(cartonProduct);
            }
        }
        
        return Result.success();
    }

    /**
     * 导出申报单
     */
    @GetMapping("/{id}/export")
    @Operation(summary = "导出申报单")
    @RequiresPermissions("business:declaration:export")
    public void exportDeclaration(
            @Parameter(description = "申报单ID") @PathVariable Long id,
            HttpServletResponse response) {
        // TODO: 实现导出功能
        response.setHeader("Content-Type", "application/octet-stream");
    }

    /**
     * 生成申报单号
     */
    private String generateFormNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        return "DEC" + datePrefix + randomSuffix;
    }
}