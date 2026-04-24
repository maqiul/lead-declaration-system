package com.declaration.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.PageParam;
import com.declaration.common.Result;
import com.declaration.entity.Organization;
import com.declaration.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 组织管理控制器
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Tag(name = "组织管理")
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @Operation(summary = "获取组织列表")
    @RequiresPermissions("org:view")
    public Result<List<Organization>> getOrgList() {
        List<Organization> orgs = organizationService.list();
        return Result.success(orgs);
    }

    @GetMapping("/tree")
    @Operation(summary = "获取组织树结构")
    @RequiresPermissions("org:view")
    public Result<List<Organization>> getOrgTree() {
        List<Organization> orgTree = organizationService.getOrgTree();
        return Result.success(orgTree);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询组织列表")
    @RequiresPermissions("org:view")
    public Result<IPage<Organization>> getOrgPage(
            @Valid PageParam pageParam,
            Organization organization) {
        IPage<Organization> page = organizationService.getOrgPage(pageParam, organization);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取组织详情")
    @RequiresPermissions("org:view")
    public Result<Organization> getOrg(
            @Parameter(description = "组织ID", required = true) @PathVariable Long id) {
        Organization organization = organizationService.getById(id);
        if (organization == null) {
            return Result.fail("组织不存在");
        }
        return Result.success(organization);
    }

    @PostMapping
    @Operation(summary = "创建组织")
    @RequiresPermissions("org:create")
    public Result<Organization> createOrg(@Valid @RequestBody Organization organization) {
        boolean result = organizationService.saveOrg(organization);
        if (result) {
            return Result.success("创建成功", organization);
        } else {
            return Result.fail("创建失败");
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新组织")
    @RequiresPermissions("org:update")
    public Result<Organization> updateOrg(@PathVariable Long id, @Valid @RequestBody Organization organization) {
        organization.setId(id);
        boolean result = organizationService.updateOrg(organization);
        if (result) {
            return Result.success("更新成功", organization);
        } else {
            return Result.fail("更新失败");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除组织")
    @RequiresPermissions("org:delete")
    public Result<Void> deleteOrg(@Parameter(description = "组织ID") @PathVariable Long id) {
        boolean result = organizationService.deleteOrg(id);
        if (result) {
            return new Result<Void>().setCode(200).setMessage("删除成功");
        } else {
            return Result.fail("删除失败");
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "获取用户所属组织")
    @RequiresPermissions("org:user")
    public Result<List<Organization>> getUserOrgs(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<Organization> orgs = organizationService.getUserOrgs(userId);
        return Result.success(orgs);
    }
}