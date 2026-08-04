package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.MaterialSupplement;
import com.declaration.service.MaterialSupplementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资料补交流程（独立于申报主流程的轻量状态机）
 * 申报人发起 → 审核人（资料审核权限）审核 → 通过则增量资料转正，驳回则清除增量
 */
@Slf4j
@RestController
@RequestMapping("v1/material-supplement")
@RequiredArgsConstructor
@Tag(name = "资料补交流程", description = "资料补交发起/审核接口")
public class MaterialSupplementController {

    private final MaterialSupplementService supplementService;

    /** 发起资料补交 */
    @PostMapping
    @Operation(summary = "发起资料补交")
    @RequiresPermissions("business:declaration:supplement:initiate")
    public Result<MaterialSupplement> start(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        String reason = body.get("reason") == null ? "" : body.get("reason").toString();
        if (!StpUtil.isLogin()) return Result.fail("未登录");
        try {
            MaterialSupplement supplement = supplementService.start(formId, reason, StpUtil.getLoginIdAsLong());
            return Result.success(supplement);
        } catch (Exception e) {
            log.warn("发起资料补交失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 更新补交原因（仅草稿态）：发起补交免弹窗，原因可在上传资料过程中内联补填 */
    @PostMapping("/{id}/reason")
    @Operation(summary = "更新补交原因（仅草稿态）")
    @RequiresPermissions("business:declaration:supplement:initiate")
    public Result<String> updateReason(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object reasonObj = body.get("reason");
        String reason = reasonObj == null ? "" : reasonObj.toString();
        Long operatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            supplementService.updateReason(id, reason, operatorId);
            return Result.success("补交原因已更新");
        } catch (Exception e) {
            log.warn("更新补交原因失败 supplementId={} : {}", id, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 查询某申报单在途的补交单（前端进入补交模式用） */
    @GetMapping("/active")
    @Operation(summary = "查询在途补交单")
    public Result<MaterialSupplement> active(@RequestParam Long formId) {
        return Result.success(supplementService.getActiveByFormId(formId));
    }

    /** 查询某申报单当前补交单（优先在途，其次草稿，前端补交模式用） */
    @GetMapping("/current")
    @Operation(summary = "查询当前补交单（含草稿）")
    public Result<MaterialSupplement> current(@RequestParam Long formId) {
        return Result.success(supplementService.getCurrentByFormId(formId));
    }

    /** 提交补交审核：草稿转补交中，此时才启动流程，审核人才可见 */
    @PostMapping("/{id}/submit")
    @Operation(summary = "提交补交审核")
    @RequiresPermissions("business:declaration:supplement:initiate")
    public Result<String> submit(@PathVariable Long id) {
        Long operatorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            supplementService.submitForAudit(id, operatorId);
            return Result.success("补交资料已提交审核");
        } catch (Exception e) {
            log.warn("提交补交审核失败 supplementId={} : {}", id, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 批量查询在途补交单（列表页展示补交中状态与审核入口用） */
    @GetMapping("/batch-active")
    @Operation(summary = "批量查询在途补交单")
    public Result<Map<Long, Long>> batchActive(@RequestParam String ids) {
        List<Long> formIds = new java.util.ArrayList<>();
        for (String part : ids.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                try { formIds.add(Long.valueOf(trimmed)); } catch (NumberFormatException ignored) { }
            }
        }
        return Result.success(supplementService.mapActiveByFormIds(formIds));
    }

    /** 审核人待审补交列表（支持按申报类型过滤：SELF-内部/EXTERNAL-外部） */
    @GetMapping("/pending-list")
    @Operation(summary = "待审补交列表")
    @RequiresPermissions("business:declaration:audit:material")
    public Result<List<MaterialSupplement>> pendingList(
            @Parameter(description = "申报类型（SELF/EXTERNAL），为空查全部") @RequestParam(required = false) String declarationType) {
        return Result.success(supplementService.listPending(declarationType));
    }

    /** 补交增量明细（supplement_id 命中的资料项 + 附件） */
    @GetMapping("/{id}/increments")
    @Operation(summary = "补交增量明细")
    public Result<Map<String, Object>> increments(@PathVariable Long id) {
        try {
            return Result.success(supplementService.getIncrements(id));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 补交历史：某申报单每一次补交的记录与文件快照（哪一次补交了哪些文件） */
    @GetMapping("/history")
    @Operation(summary = "补交历史（含每次补交的文件快照）")
    public Result<List<MaterialSupplement>> history(@RequestParam Long formId) {
        return Result.success(supplementService.listHistoryByFormId(formId));
    }

    /** 审核补交：approved=true 增量转正；false 删除增量 */
    @PostMapping("/{id}/audit")
    @Operation(summary = "审核补交")
    @RequiresPermissions("business:declaration:audit:material")
    public Result<String> audit(@PathVariable Long id,
                                @RequestParam boolean approved,
                                @RequestParam(required = false) String remark) {
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            supplementService.audit(id, approved, remark, auditorId);
            return Result.success(approved ? "补交审核通过" : "补交审核驳回");
        } catch (Exception e) {
            log.warn("补交审核失败 supplementId={} : {}", id, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }
}
