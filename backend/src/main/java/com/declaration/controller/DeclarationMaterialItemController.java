package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.DeclarationMaterialItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 申报资料项实例（每申报单）
 */
@Slf4j
@RestController
@RequestMapping("v1/material/items")
@RequiredArgsConstructor
@Tag(name = "资料项实例", description = "申报单资料项提交/审核接口")
public class DeclarationMaterialItemController {

    private final DeclarationMaterialItemService itemService;
    private final DeclarationAttachmentService attachmentService;

    /** 获取某申报单的资料项视图（懒创建：未操作过的资料项以虚拟项 id=null 返回，不落库） */
    @GetMapping
    @Operation(summary = "获取申报单的资料项列表（懒创建视图）")
    public Result<List<DeclarationMaterialItem>> listByFormId(@RequestParam Long formId) {
        return Result.success(itemService.viewByFormId(formId));
    }

    /** 幂等确保模板对应的资料项已落库，返回带 id 的实例
     *  用于前端在"上传附件/编辑字段"时从虚拟项升格为真实记录 */
    @PostMapping("/ensure")
    @Operation(summary = "确保模板资料项已落库")
    public Result<DeclarationMaterialItem> ensureFromTemplate(@RequestParam Long formId,
                                                              @RequestParam Long templateId) {
        try {
            return Result.success(itemService.ensureItemFromTemplate(formId, templateId));
        } catch (Exception e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 单据内手动新增一项（不入全局模板） */
    @PostMapping
    @Operation(summary = "单据内新增资料项")
    public Result<DeclarationMaterialItem> add(@RequestBody DeclarationMaterialItem entity) {
        if (entity.getFormId() == null) {
            return Result.fail("申报单ID不能为空");
        }
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            return Result.fail("资料名称不能为空");
        }
        if (entity.getRequired() == null) entity.setRequired(1);
        if (entity.getSort() == null) entity.setSort(0);
        entity.setStatus(0);
        entity.setTemplateId(null); // 手动新增无模板来源
        // 让 MetaObjectHandler 自动填充当前用户为 createBy/updateBy
        entity.setCreateBy(null);
        entity.setUpdateBy(null);
        itemService.save(entity);
        return Result.success(entity);
    }

    /** 修改资料项（名称/必填/排序/说明/结构化字段值） */
    @PutMapping
    @Operation(summary = "修改资料项")
    public Result<Boolean> update(@RequestBody DeclarationMaterialItem entity) {
        if (entity.getId() == null) {
            return Result.fail("ID不能为空");
        }
        // 不允许通过本接口修改附件状态字段，防止误覆盖
        DeclarationMaterialItem old = itemService.getById(entity.getId());
        if (old == null) return Result.fail("资料项不存在");
        old.setName(entity.getName());
        old.setRequired(entity.getRequired() == null ? old.getRequired() : entity.getRequired());
        old.setSort(entity.getSort() == null ? old.getSort() : entity.getSort());
        old.setRemark(entity.getRemark());
        // 结构化字段值
        old.setAmount(entity.getAmount());
        old.setCurrency(entity.getCurrency());
        old.setInvoiceNo(entity.getInvoiceNo());
        old.setInvoiceDate(entity.getInvoiceDate());
        old.setExtraData(entity.getExtraData());
        // form_schema 一般不在提交阶段修改，但允许管理员单据内覆盖
        if (entity.getFormSchema() != null) {
            old.setFormSchema(entity.getFormSchema());
        }
        // 显式刷新更新人与更新时间
        // （从 DB 查出的 old.updateBy/updateTime 非 null，MP strictUpdateFill 不会覆盖旧值，必须手动设置）
        if (StpUtil.isLogin()) {
            old.setUpdateBy(StpUtil.getLoginIdAsLong());
        }
        old.setUpdateTime(LocalDateTime.now());
        return Result.success(itemService.updateById(old));
    }

    /** 删除资料项（仅手动新增的允许删除） */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除资料项")
    public Result<Boolean> delete(@PathVariable Long id) {
        DeclarationMaterialItem item = itemService.getById(id);
        if (item == null) return Result.fail("资料项不存在");
        if (item.getTemplateId() != null) {
            return Result.fail("该资料项由模板生成，不允许删除（可取消必填）");
        }
        return Result.success(itemService.removeById(id));
    }

    /** 上传附件到指定资料项
     *  兼容懒创建：如果 id 查不到但带了 formId+templateId，则先按模板 ensure 一条实例再上传
     */
    @PostMapping("/{id}/upload")
    @Operation(summary = "上传资料项附件")
    public Result<DeclarationMaterialItem> upload(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "formId", required = false) Long formId,
                                                  @RequestParam(value = "templateId", required = false) Long templateId) {
        DeclarationMaterialItem item = id == null || id <= 0 ? null : itemService.getById(id);
        if (item == null) {
            log.warn("上传时未找到资料项实例 id={} formId={} templateId={}", id, formId, templateId);
            if (formId != null && templateId != null) {
                try {
                    item = itemService.ensureItemFromTemplate(formId, templateId);
                    log.info("upload fallback: ensure material item itemId={} formId={} templateId={}",
                            item == null ? null : item.getId(), formId, templateId);
                } catch (Exception e) {
                    log.error("上传时按模板 ensure 失败 formId={} templateId={}", formId, templateId, e);
                }
            }
        }
        if (item == null) return Result.fail("资料项不存在");
        try {
            DeclarationAttachment att = attachmentService.uploadFile(file, "MaterialItem");
            item.setFileName(att.getFileName());
            item.setFileUrl(att.getFileUrl());
            item.setStatus(1);
            if (StpUtil.isLogin()) {
                Long uid = StpUtil.getLoginIdAsLong();
                item.setUploadBy(uid);
                // 显式刷新更新人，避免旧值被 strictUpdateFill 跳过
                item.setUpdateBy(uid);
            }
            item.setUploadTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            itemService.updateById(item);
            return Result.success(item);
        } catch (Exception e) {
            log.error("上传资料附件失败", e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    /** 删除已上传的附件（保留资料项） */
    @DeleteMapping("/{id}/file")
    @Operation(summary = "清除资料项附件")
    public Result<Boolean> clearFile(@PathVariable Long id) {
        DeclarationMaterialItem item = itemService.getById(id);
        if (item == null) return Result.fail("资料项不存在");
        item.setFileName(null);
        item.setFileUrl(null);
        item.setStatus(0);
        item.setUploadBy(null);
        item.setUploadTime(null);
        if (StpUtil.isLogin()) {
            item.setUpdateBy(StpUtil.getLoginIdAsLong());
        }
        item.setUpdateTime(LocalDateTime.now());
        return Result.success(itemService.updateById(item));
    }

    /** 提交资料（完成 materialSubmit 任务） */
    @PostMapping("/submit")
    @Operation(summary = "提交资料")
    @RequiresPermissions("business:declaration:material:submit")
    public Result<String> submit(@RequestParam Long formId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submit(formId, userId);
            return Result.success("资料提交成功");
        } catch (Exception e) {
            log.warn("资料提交失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 资料审核（完成 materialAudit 任务） */
    @PostMapping("/audit")
    @Operation(summary = "资料审核")
    @RequiresPermissions("business:declaration:audit:material")
    public Result<String> audit(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.audit(formId, approved, remark, auditorId);
            return Result.success("资料审核" + (approved ? "通过" : "驳回") + "成功");
        } catch (Exception e) {
            log.warn("资料审核失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 提交业务发票（完成 invoiceSubmit 任务） */
    @PostMapping("/invoice/submit")
    @Operation(summary = "提交业务发票")
    @RequiresPermissions("business:declaration:invoice:submit")
    public Result<String> submitInvoice(@RequestParam Long formId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submitInvoice(formId, userId);
            return Result.success("发票提交成功");
        } catch (Exception e) {
            log.warn("发票提交失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 业务发票审核（完成 invoiceAudit 任务） */
    @PostMapping("/invoice/audit")
    @Operation(summary = "业务发票审核")
    @RequiresPermissions("business:declaration:audit:invoice")
    public Result<String> auditInvoice(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.auditInvoice(formId, approved, remark, auditorId);
            return Result.success("发票审核" + (approved ? "通过" : "驳回") + "成功");
        } catch (Exception e) {
            log.warn("发票审核失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }
}
