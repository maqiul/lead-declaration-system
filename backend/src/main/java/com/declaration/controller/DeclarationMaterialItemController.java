package com.declaration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.annotation.RequiresPermissions;
import com.declaration.common.Result;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.MaterialAttachment;
import com.declaration.entity.MaterialSupplement;
import com.declaration.service.DeclarationAttachmentService;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.DeclarationMaterialTemplateService;
import com.declaration.service.FinancialSupplementService;
import com.declaration.service.MaterialAttachmentService;
import com.declaration.service.MaterialSupplementService;
import com.declaration.service.PdfInvoiceAmountParser;
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
    private final MaterialAttachmentService materialAttachmentService;
    private final FinancialSupplementService financialSupplementService;
    private final PdfInvoiceAmountParser pdfInvoiceAmountParser;
    private final DeclarationMaterialTemplateService templateService;
    private final MaterialSupplementService materialSupplementService;

    /** 查询表单当前补交单（在途 status=0 或草稿 status=-1，无则 null），用于存量资料只增不改锁定 */
    private MaterialSupplement activeSupplement(Long formId) {
        if (formId == null) return null;
        return materialSupplementService.getCurrentByFormId(formId);
    }

    /** 按环节获取启用的资料模板（未保存草稿时前端预览资料项用，不需要模板管理权限） */
    @GetMapping("/template-preview")
    @Operation(summary = "按环节获取启用的资料模板（草稿预览）")
    public Result<List<DeclarationMaterialTemplate>> templatePreview(@RequestParam String stage) {
        return Result.success(templateService.listByStage(stage));
    }

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
        // 补交模式：在途补交时新增项必须携带有效的 supplementId（补交增量），否则拒绝
        MaterialSupplement active = activeSupplement(entity.getFormId());
        if (active != null) {
            if (entity.getSupplementId() == null || !active.getId().equals(entity.getSupplementId())) {
                return Result.fail("当前存在在途资料补交，新增资料项请通过补交模式提交");
            }
        } else {
            entity.setSupplementId(null);
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
        // 补交锁定：在途补交时，存量资料项（非增量）不可修改
        if (old.getSupplementId() == null && activeSupplement(old.getFormId()) != null) {
            return Result.fail("当前存在在途资料补交，存量资料只增不改，不可修改");
        }
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
        // 补交锁定：在途补交时，存量资料项（非增量）不可删除
        if (item.getSupplementId() == null && activeSupplement(item.getFormId()) != null) {
            return Result.fail("当前存在在途资料补交，存量资料只增不改，不可删除");
        }
        return Result.success(itemService.removeById(id));
    }

    /** 上传附件到指定资料项
     *  兼容懒创建：如果 id 查不到但带了 formId+templateId，则先按模板 ensure 一条实例再上传
     *  补交模式：携带 supplementId 时新附件打补交增量标记
     */
    @PostMapping("/{id}/upload")
    @Operation(summary = "上传资料项附件（追加模式，支持多文件）")
    public Result<DeclarationMaterialItem> upload(@PathVariable Long id,
                                                  @RequestParam("file") MultipartFile file,
                                                  @RequestParam(value = "formId", required = false) Long formId,
                                                  @RequestParam(value = "templateId", required = false) Long templateId,
                                                  @RequestParam(value = "uploadStage", required = false) String uploadStage,
                                                  @RequestParam(value = "supplementId", required = false) Long supplementId) {
        // 补交上传校验：supplementId 必须是草稿/在途补交且与目标资料项同单
        if (supplementId != null) {
            MaterialSupplement supplement = materialSupplementService.getById(supplementId);
            if (supplement == null || supplement.getStatus() == null
                    || (supplement.getStatus() != -1 && supplement.getStatus() != 0)) {
                return Result.fail("补交单不存在或已审核，无法上传补交文件");
            }
            DeclarationMaterialItem target = id == null || id <= 0 ? null : itemService.getById(id);
            Long targetFormId = target != null ? target.getFormId() : formId;
            if (targetFormId != null && !targetFormId.equals(supplement.getFormId())) {
                return Result.fail("补交单与资料项不属于同一申报单");
            }
        }
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
        // 补交锁定：补交状态下允许向同一资料项追加增量文件（附件打补交增量标记，原文件不可删除/修改）；
        // 但上传必须携带补交单增量标记，防止绕过审核
        if (supplementId == null && activeSupplement(item.getFormId()) != null) {
            return Result.fail("当前存在资料补交，请通过补交模式上传增量资料");
        }
        try {
            // 上传文件并保存到附件子表（记录上传时所处环节；补交模式打增量标记）
            MaterialAttachment att = materialAttachmentService.uploadForItem(item.getId(), file, uploadStage, supplementId);
            // 同步主表冗余字段（指向最新文件）
            item.setFileName(att.getFileName());
            item.setFileUrl(att.getFileUrl());
            item.setStatus(1);
            if (StpUtil.isLogin()) {
                Long uid = StpUtil.getLoginIdAsLong();
                item.setUploadBy(uid);
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

    /** 删除已上传的附件（保留资料项，清空所有附件） */
    @DeleteMapping("/{id}/file")
    @Operation(summary = "清除资料项附件（传 stage 时仅清除本环节上传及无环节标记的附件）")
    public Result<Boolean> clearFile(@PathVariable Long id,
                                     @RequestParam(value = "stage", required = false) String stage) {
        DeclarationMaterialItem item = itemService.getById(id);
        if (item == null) return Result.fail("资料项不存在");
        // 补交锁定：在途补交时，存量资料项（非增量）的附件不可清除
        if (item.getSupplementId() == null && activeSupplement(item.getFormId()) != null) {
            return Result.fail("当前存在在途资料补交，存量资料只增不改，不可清除附件");
        }
        if (stage == null || stage.isEmpty()) {
            // 删除子表所有附件
            materialAttachmentService.removeAllByItemId(id);
        } else {
            // 仅清除本环节上传的附件（含无环节标记的历史附件），前序环节文件保留
            for (MaterialAttachment a : materialAttachmentService.listByItemId(id)) {
                if (a.getStage() == null || a.getStage().isEmpty() || stage.equals(a.getStage())) {
                    materialAttachmentService.removeAttachment(a.getId());
                }
            }
        }
        syncItemAfterAttachmentChange(item);
        return Result.success(true);
    }

    /** 删除单个附件 */
    @DeleteMapping("/{id}/file/{attachmentId}")
    @Operation(summary = "删除资料项的单个附件")
    public Result<Boolean> deleteAttachment(@PathVariable Long id,
                                            @PathVariable Long attachmentId,
                                            @RequestParam(value = "stage", required = false) String stage) {
        DeclarationMaterialItem item = itemService.getById(id);
        if (item == null) return Result.fail("资料项不存在");
        // 环节保护：前序环节上传的附件，后续环节不可删除（无环节标记的历史附件放行）
        MaterialAttachment target = materialAttachmentService.getById(attachmentId);
        if (target == null) return Result.fail("附件不存在");
        // 补交锁定：在途补交时，存量附件（非增量）不可删除
        if (target.getSupplementId() == null && activeSupplement(item.getFormId()) != null) {
            return Result.fail("当前存在在途资料补交，存量资料只增不改，不可删除附件");
        }
        if (stage != null && !stage.isEmpty()
                && target.getStage() != null && !target.getStage().isEmpty()
                && !stage.equals(target.getStage())) {
            return Result.fail("该附件由前序环节上传，当前环节不可删除");
        }
        materialAttachmentService.removeAttachment(attachmentId);
        syncItemAfterAttachmentChange(item);
        return Result.success(true);
    }

    /** 附件增删后同步主表冗余字段（无附件时清空，否则指向最新附件） */
    private void syncItemAfterAttachmentChange(DeclarationMaterialItem item) {
        long remaining = materialAttachmentService.countByItemId(item.getId());
        if (remaining == 0) {
            // 清空主表冗余字段
            item.setFileName(null);
            item.setFileUrl(null);
            item.setStatus(0);
            item.setUploadBy(null);
            item.setUploadTime(null);
            if (StpUtil.isLogin()) {
                item.setUpdateBy(StpUtil.getLoginIdAsLong());
            }
            item.setUpdateTime(LocalDateTime.now());
            itemService.updateById(item);
        } else {
            // 更新主表冗余字段为最新附件
            List<MaterialAttachment> atts = materialAttachmentService.listByItemId(item.getId());
            if (!atts.isEmpty()) {
                MaterialAttachment latest = atts.get(0);
                item.setFileName(latest.getFileName());
                item.setFileUrl(latest.getFileUrl());
                if (StpUtil.isLogin()) {
                    item.setUpdateBy(StpUtil.getLoginIdAsLong());
                }
                item.setUpdateTime(LocalDateTime.now());
                itemService.updateById(item);
            }
        }
    }

    /** 获取某资料项的附件列表 */
    @GetMapping("/{id}/files")
    @Operation(summary = "获取资料项附件列表")
    public Result<List<MaterialAttachment>> listAttachments(@PathVariable Long id) {
        return Result.success(materialAttachmentService.listByItemId(id));
    }

    /** 更新附件的结构化字段（金额/发票号/开票日期等） */
    @PutMapping("/{id}/file/{attachmentId}")
    @Operation(summary = "更新附件结构化字段")
    public Result<MaterialAttachment> updateAttachmentFields(
            @PathVariable Long id,
            @PathVariable Long attachmentId,
            @RequestBody MaterialAttachment body) {
        MaterialAttachment att = materialAttachmentService.getById(attachmentId);
        if (att == null || !att.getItemId().equals(id)) {
            return Result.fail("附件不存在");
        }
        // 补交锁定：补交状态下，存量附件（非增量）的结构化字段不可修改
        DeclarationMaterialItem attItem = itemService.getById(id);
        if (att.getSupplementId() == null && attItem != null && activeSupplement(attItem.getFormId()) != null) {
            return Result.fail("当前存在资料补交，存量资料只增不改，不可修改附件信息");
        }
        // 允许更新结构化字段（包括设置为 null 以清空字段）
        att.setAmount(body.getAmount());
        att.setCurrency(body.getCurrency());
        att.setInvoiceNo(body.getInvoiceNo());
        att.setInvoiceDate(body.getInvoiceDate());
        att.setExtraData(body.getExtraData());
        materialAttachmentService.updateById(att);
        // 同步主表冗余字段（取第一个附件的值，保持兼容）
        syncItemFromFirstAttachment(id);
        return Result.success(att);
    }

    /** 提交资料（完成 materialSubmit 任务） */
    @PostMapping("/submit")
    @Operation(summary = "提交资料")
    @RequiresPermissions("business:declaration:material:submit")
    public Result<String> submit(@RequestParam Long formId,
                                 @RequestParam(required = false, defaultValue = "false") boolean skipRequiredCheck) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submit(formId, userId, skipRequiredCheck);
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

    /** 提交补充资料（完成 supplementSubmit 任务） */
    @PostMapping("/supplement/submit")
    @Operation(summary = "提交补充资料")
    @RequiresPermissions("business:declaration:supplement:submit")
    public Result<String> submitSupplement(@RequestParam Long formId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submitSupplement(formId, userId);
            return Result.success("补充资料提交成功");
        } catch (Exception e) {
            log.warn("补充资料提交失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 补充资料审核（完成 supplementAudit 任务） */
    @PostMapping("/supplement/audit")
    @Operation(summary = "补充资料审核")
    @RequiresPermissions("business:declaration:audit:supplement")
    public Result<String> auditSupplement(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.auditSupplement(formId, approved, remark, auditorId);
            return Result.success("补充资料审核" + (approved ? "通过" : "驳回") + "成功");
        } catch (Exception e) {
            log.warn("补充资料审核失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 提交申请开票金额（完成 invoiceAmountSubmit 任务） */
    @PostMapping("/invoice-amount/submit")
    @Operation(summary = "提交申请开票金额")
    @RequiresPermissions("business:declaration:invoice-amount:submit")
    public Result<String> submitInvoiceAmount(@RequestParam Long formId) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submitInvoiceAmount(formId, userId);
            return Result.success("申请开票金额提交成功");
        } catch (Exception e) {
            log.warn("申请开票金额失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 开票金额审核（完成 invoiceAmountAudit 任务） */
    @PostMapping("/invoice-amount/audit")
    @Operation(summary = "开票金额审核")
    @RequiresPermissions("business:declaration:audit:invoice-amount")
    public Result<String> auditInvoiceAmount(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.auditInvoiceAmount(formId, approved, remark, auditorId);
            return Result.success("开票金额审核" + (approved ? "通过" : "驳回") + "成功");
        } catch (Exception e) {
            log.warn("开票金额审核失败 formId={} : {}", formId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 获取开票金额（瘦身版：仅返回开票金额，计算明细不再下发） */
    @GetMapping("/invoice-amount/calculate")
    @Operation(summary = "获取开票金额")
    public Result<Map<String, Object>> getInvoiceAmountDetail(@RequestParam Long formId) {
        try {
            Map<String, Object> detail = financialSupplementService.getCalculationDetail(formId);
            Map<String, Object> slim = new java.util.LinkedHashMap<>();
            slim.put("invoiceAmount", detail.get("invoiceAmount"));
            return Result.success(slim);
        } catch (Exception e) {
            log.warn("获取开票金额失败 formId={} : {}", formId, e.getMessage());
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

    /** 通用阶段提交（字典驱动，stage = form_section 字典的 submitKey） */
    @PostMapping("/stage/submit")
    @Operation(summary = "通用阶段提交")
    public Result<String> submitStage(@RequestParam Long formId, @RequestParam String stage,
                                       @RequestParam(required = false, defaultValue = "false") boolean skipRequiredCheck) {
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.submitStage(formId, stage, userId, skipRequiredCheck);
            return Result.success("提交成功");
        } catch (Exception e) {
            log.warn("阶段提交失败 formId={} stage={} : {}", formId, stage, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /** 通用阶段审核（字典驱动，stage = form_section 字典的 auditTaskKey） */
    @PostMapping("/stage/audit")
    @Operation(summary = "通用阶段审核")
    public Result<String> auditStage(@RequestBody Map<String, Object> body) {
        Object formIdObj = body.get("formId");
        if (formIdObj == null) return Result.fail("formId 不能为空");
        Long formId = Long.valueOf(formIdObj.toString());
        String stage = body.get("stage") == null ? null : body.get("stage").toString();
        if (stage == null || stage.isEmpty()) return Result.fail("stage 不能为空");
        Object resultObj = body.get("result"); // 1=通过 2=驳回
        boolean approved = resultObj != null && "1".equals(resultObj.toString());
        String remark = body.get("remark") == null ? "" : body.get("remark").toString();
        Long auditorId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        try {
            itemService.auditStage(formId, stage, approved, remark, auditorId);
            return Result.success(approved ? "审核通过" : "审核驳回");
        } catch (Exception e) {
            log.warn("阶段审核失败 formId={} stage={} : {}", formId, stage, e.getMessage());
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

    /**
     * OCR 状态诊断接口（查看 OCR 引擎是否就绪）。
     */
    @GetMapping("/ocr-status")
    @Operation(summary = "OCR 状态诊断")
    public Result<Map<String, Object>> ocrStatus() {
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("ocrReady", pdfInvoiceAmountParser.isOcrReady());
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        log.info("[OCR] 状态检查请求, ocrReady={}", pdfInvoiceAmountParser.isOcrReady());
        return Result.success(status);
    }

    /**
     * 解析发票 PDF 中的金额（仅解析返回，不保存任何数据）。
     * 前端在用户上传货代/报关代理发票 PDF 时调用，用于与手填金额进行比对校验。
     */
    @PostMapping("/parse-invoice-pdf")
    @Operation(summary = "解析发票 PDF 中的金额")
    @RequiresPermissions("business:declaration:update")
    public Result<PdfInvoiceAmountParser.PdfParseResult> parseInvoicePdf(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            PdfInvoiceAmountParser.PdfParseResult r = new PdfInvoiceAmountParser.PdfParseResult();
            r.setSuccess(false);
            r.setErrorMsg("未上传文件");
            return Result.success(r);
        }
        try (var input = file.getInputStream()) {
            PdfInvoiceAmountParser.PdfParseResult r = pdfInvoiceAmountParser.parseAmount(input);
            return Result.success(r);
        } catch (Exception e) {
            log.warn("读取发票 PDF 失败: {}", e.getMessage());
            PdfInvoiceAmountParser.PdfParseResult r = new PdfInvoiceAmountParser.PdfParseResult();
            r.setSuccess(false);
            r.setErrorMsg("读取 PDF 文件失败");
            return Result.success(r);
        }
    }

    /**
     * 同步主表冗余字段（取最新附件的值），保持与单文件模式的兼容性。
     * 当附件全部删除后主表 status 已由其他逻辑处理，此处仅同步结构化字段。
     */
    private void syncItemFromFirstAttachment(Long itemId) {
        DeclarationMaterialItem item = itemService.getById(itemId);
        if (item == null) return;
        List<MaterialAttachment> atts = materialAttachmentService.listByItemId(itemId);
        if (!atts.isEmpty()) {
            MaterialAttachment latest = atts.get(0);
            item.setAmount(latest.getAmount());
            item.setCurrency(latest.getCurrency());
            item.setInvoiceNo(latest.getInvoiceNo());
            item.setInvoiceDate(latest.getInvoiceDate());
            item.setExtraData(latest.getExtraData());
            if (StpUtil.isLogin()) {
                item.setUpdateBy(StpUtil.getLoginIdAsLong());
            }
            item.setUpdateTime(LocalDateTime.now());
            itemService.updateById(item);
        }
    }
}
