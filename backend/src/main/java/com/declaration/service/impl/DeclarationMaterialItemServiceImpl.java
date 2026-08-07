package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.dao.DeclarationMaterialItemDao;
import com.declaration.entity.BusinessAuditRecord;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationMaterialExemption;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.FinancialSupplement;
import com.declaration.entity.MaterialAttachment;
import com.declaration.entity.MaterialSupplement;
import com.declaration.entity.MaterialTemplateBinding;
import com.declaration.entity.SysDictItem;
import com.declaration.entity.User;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationMaterialExemptionService;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.DeclarationMaterialTemplateService;
import com.declaration.service.DeclarationRemittanceService;
import com.declaration.service.FinancialSupplementService;
import com.declaration.service.InvoiceService;
import com.declaration.service.MaterialAttachmentService;
import com.declaration.service.MaterialSupplementService;
import com.declaration.service.SysDictService;
import com.declaration.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 申报资料项 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationMaterialItemServiceImpl
        extends ServiceImpl<DeclarationMaterialItemDao, DeclarationMaterialItem>
        implements DeclarationMaterialItemService {

    private final DeclarationMaterialTemplateService templateService;
    private final TaskService flowableTaskService;
    private final UserService userService;
    private final InvoiceService invoiceService;
    private final BusinessAuditRecordDao auditRecordDao;
    private final DeclarationFormService declarationFormService;
    private final MaterialAttachmentService materialAttachmentService;
    @Lazy
    @Autowired
    private FinancialSupplementService financialSupplementService;
    @Lazy
    @Autowired
    private MaterialSupplementService materialSupplementService;
    private final DeclarationRemittanceService remittanceService;
    private final SysDictService sysDictService;
    private final DeclarationMaterialExemptionService exemptionService;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 在绑定规则中查找匹配项，返回第一条匹配的规则（用于读取 required 等覆盖字段）。
     * 匹配条件：flow匹配或空 AND transport匹配或空
     * 未匹配返回 null
     */
    private MaterialTemplateBinding findMatchingBinding(List<MaterialTemplateBinding> bindings, String formFlowCode, String formTransportMode) {
        if (bindings == null || bindings.isEmpty()) return null;
        for (MaterialTemplateBinding b : bindings) {
            boolean flowMatch = (b.getFlowTemplateCode() == null) ||
                    (formFlowCode != null && b.getFlowTemplateCode().equals(formFlowCode));
            boolean transportMatch = (b.getTransportModeCode() == null) ||
                    (formTransportMode != null && b.getTransportModeCode().equals(formTransportMode));
            if (flowMatch && transportMatch) return b;
        }
        return null;
    }

    /**
     * 获取资料项的 required 值：优先使用匹配绑定规则的 required；
     * 其次按环节必填配置 required_stages（命中当前环节则必填）；否则回退到模板默认值
     * @param stage 当前环节（视图类调用无环节上下文时传 null，回退模板默认 required）
     */
    private int resolveRequired(List<MaterialTemplateBinding> bindings, String formFlowCode, String formTransportMode, DeclarationMaterialTemplate tpl, String stage) {
        MaterialTemplateBinding matched = findMatchingBinding(bindings, formFlowCode, formTransportMode);
        if (matched != null && matched.getRequired() != null) {
            return matched.getRequired();
        }
        // 必填按环节配置：required_stages 非空时以环节命中与否判定
        if (tpl.getRequiredStages() != null && !tpl.getRequiredStages().isEmpty()) {
            if (stage == null || stage.isEmpty()) {
                // 无环节上下文（视图展示）：非空即视为必填，与 required 兼容语义一致
                return 1;
            }
            return hasStage(tpl.getRequiredStages(), stage) ? 1 : 0;
        }
        return tpl.getRequired() == null ? 1 : tpl.getRequired();
    }

    /**
     * 解析视图层透传的必填环节配置：绑定规则显式覆盖 required 时返回 null（回退 item.required），
     * 否则返回模板的 required_stages，由前端按当前环节命中与否判定必填
     */
    private String resolveRequiredStages(List<MaterialTemplateBinding> bindings, String formFlowCode, String formTransportMode, DeclarationMaterialTemplate tpl) {
        MaterialTemplateBinding matched = findMatchingBinding(bindings, formFlowCode, formTransportMode);
        if (matched != null && matched.getRequired() != null) {
            return null;
        }
        return tpl.getRequiredStages();
    }

    /** 审核记录 business_type 常量（提交与审核共用一个类型，同一条记录的两个阶段）*/
    public static final String BT_MATERIAL_AUDIT  = "DECLARATION_MATERIAL_AUDIT";
    public static final String BT_SUPPLEMENT_AUDIT = "DECLARATION_SUPPLEMENT_AUDIT";
    public static final String BT_INVOICE_AMOUNT_AUDIT = "DECLARATION_INVOICE_AMOUNT_AUDIT";
    public static final String BT_INVOICE_AUDIT   = "DECLARATION_INVOICE_AUDIT";

    /** 不属于资料提交阶段的环节（基础资料/补充资料/业务发票各自独立校验） */
    private static final Set<String> NON_MATERIAL_SUBMIT_STAGES = new HashSet<>(Arrays.asList("BASIC", "SUPPLEMENT", "INVOICE"));

    /**
     * stage 多环节支持：逗号分隔的 stage 值是否包含目标环节（空值兑底 MATERIAL_SUBMIT）
     */
    public static boolean hasStage(String stageValue, String target) {
        String v = (stageValue == null || stageValue.isEmpty()) ? "MATERIAL_SUBMIT" : stageValue;
        for (String s : v.split(",")) {
            if (target.equals(s.trim())) return true;
        }
        return false;
    }

    /**
     * 是否参与资料提交阶段校验：拆分后存在任意一个不属于 BASIC/SUPPLEMENT/INVOICE 的环节即参与（空值默认参与）
     */
    public static boolean inMaterialSubmitStage(String stageValue) {
        if (stageValue == null || stageValue.isEmpty()) return true;
        for (String s : stageValue.split(",")) {
            if (!NON_MATERIAL_SUBMIT_STAGES.contains(s.trim())) return true;
        }
        return false;
    }

    /**
     * 提交时插入一条待审核记录（auditStatus=0）
     */
    private void insertPendingAuditRecord(Long formId, String businessType, Long applicantId, String applyReason) {
        try {
            DeclarationForm form = declarationFormService.getById(formId);
            BusinessAuditRecord r = new BusinessAuditRecord();
            r.setBusinessId(formId);
            r.setBusinessType(businessType);
            r.setApplicantId(applicantId);
            r.setApplyReason(applyReason != null ? applyReason : labelOfType(businessType));
            r.setApplyTime(LocalDateTime.now());
            r.setAuditStatus(0);
            r.setPreStatus(form == null ? null : form.getStatus());
            auditRecordDao.insert(r);
        } catch (Exception e) {
            log.error("插入待审核记录失败 formId={} businessType={} : {}", formId, businessType, e.getMessage(), e);
        }
    }

    /**
     * 审核时更新同单同类型的最新一条 auditStatus=0 记录为审核结果；查不到则兜底插入一条完整记录
     */
    private void finishAuditRecord(Long formId, String businessType, boolean approved, String remark, Long auditorId) {
        try {
            BusinessAuditRecord pending = auditRecordDao.selectOne(
                new LambdaQueryWrapper<BusinessAuditRecord>()
                    .eq(BusinessAuditRecord::getBusinessId, formId)
                    .eq(BusinessAuditRecord::getBusinessType, businessType)
                    .eq(BusinessAuditRecord::getAuditStatus, 0)
                    .orderByDesc(BusinessAuditRecord::getApplyTime)
                    .last("LIMIT 1")
            );
            LocalDateTime now = LocalDateTime.now();
            if (pending != null) {
                pending.setAuditorId(auditorId);
                pending.setAuditStatus(approved ? 1 : 2);
                pending.setAuditRemark(remark);
                pending.setAuditTime(now);
                auditRecordDao.updateById(pending);
            } else {
                // 兜底：没查到提交记录（旧数据/特殊路径），直接插入一条完整审核记录
                DeclarationForm form = declarationFormService.getById(formId);
                BusinessAuditRecord r = new BusinessAuditRecord();
                r.setBusinessId(formId);
                r.setBusinessType(businessType);
                r.setApplicantId(form == null ? null : form.getCreateBy());
                r.setApplyReason(labelOfType(businessType));
                r.setApplyTime(now);
                r.setAuditorId(auditorId);
                r.setAuditStatus(approved ? 1 : 2);
                r.setAuditRemark(remark);
                r.setAuditTime(now);
                r.setPreStatus(form == null ? null : form.getStatus());
                auditRecordDao.insert(r);
            }
        } catch (Exception e) {
            log.error("更新审核记录失败 formId={} businessType={} : {}", formId, businessType, e.getMessage(), e);
        }
    }

    private String labelOfType(String businessType) {
        switch (businessType) {
            case BT_MATERIAL_AUDIT:  return "资料审核";
            case BT_SUPPLEMENT_AUDIT: return "补充资料审核";
            case BT_INVOICE_AMOUNT_AUDIT: return "开票金额审核";
            case BT_INVOICE_AUDIT:   return "业务发票审核";
            default: return businessType;
        }
    }

    @Override
    public List<DeclarationMaterialItem> listByFormId(Long formId) {
        if (formId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<DeclarationMaterialItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationMaterialItem::getFormId, formId)
               .orderByAsc(DeclarationMaterialItem::getSort)
               .orderByAsc(DeclarationMaterialItem::getId);
        List<DeclarationMaterialItem> list = this.list(wrapper);
        // 存量流程兼容：对 form_schema 为空但关联模板有 schema 的行，自动回填并持久化
        backfillFormSchemaIfNeeded(list);
        // 回填创建人/更新人昵称
        fillUserNames(list);
        return list;
    }

    /**
     * 批量回填 createByName / updateByName，减少 N+1 查询
     */
    private void fillUserNames(List<DeclarationMaterialItem> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> userIds = new HashSet<>();
        for (DeclarationMaterialItem it : list) {
            if (it.getCreateBy() != null) userIds.add(it.getCreateBy());
            if (it.getUpdateBy() != null) userIds.add(it.getUpdateBy());
        }
        if (userIds.isEmpty()) return;
        Map<Long, String> nameMap = new HashMap<>();
        try {
            List<User> users = userService.listByIds(userIds);
            if (users != null) {
                for (User u : users) {
                    String display = StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
                    nameMap.put(u.getId(), display);
                }
            }
        } catch (Exception e) {
            log.warn("批量查用户失败，昵称回填跳过: {}", e.getMessage());
        }
        for (DeclarationMaterialItem it : list) {
            if (it.getCreateBy() != null) {
                it.setCreateByName(nameMap.get(it.getCreateBy()));
            }
            if (it.getUpdateBy() != null) {
                it.setUpdateByName(nameMap.get(it.getUpdateBy()));
            }
        }
    }

    /**
     * 对缺少 form_schema 的存量实例从模板回填（只处理 template_id 非空的行）。
     * 避免每次请求都查：回填后写回 DB，之后直接走记录字段。
     */
    private void backfillFormSchemaIfNeeded(List<DeclarationMaterialItem> list) {
        if (list == null || list.isEmpty()) return;
        Set<Long> templateIds = new HashSet<>();
        for (DeclarationMaterialItem it : list) {
            if (!StringUtils.hasText(it.getFormSchema()) && it.getTemplateId() != null) {
                templateIds.add(it.getTemplateId());
            }
        }
        if (templateIds.isEmpty()) return;
        Map<Long, String> tplSchemaMap = new HashMap<>();
        for (Long tid : templateIds) {
            DeclarationMaterialTemplate tpl = templateService.getById(tid);
            if (tpl != null && StringUtils.hasText(tpl.getFormSchema())) {
                tplSchemaMap.put(tid, tpl.getFormSchema());
            }
        }
        if (tplSchemaMap.isEmpty()) return;
        for (DeclarationMaterialItem it : list) {
            if (StringUtils.hasText(it.getFormSchema())) continue;
            String schema = tplSchemaMap.get(it.getTemplateId());
            if (!StringUtils.hasText(schema)) continue;
            it.setFormSchema(schema);
            // 写回 DB，让后续提交/审核也能直接使用
            try {
                DeclarationMaterialItem update = new DeclarationMaterialItem();
                update.setId(it.getId());
                update.setFormSchema(schema);
                this.updateById(update);
            } catch (Exception e) {
                log.warn("回填 form_schema 失败，itemId={}", it.getId(), e);
            }
        }
    }

    /**
     * 合并视图：模板虚拟项（id=null） + 已有实例。
     * - 已存在实例的模板行直接用实例（带 id，有 createBy/updateBy）
     * - 仅在模板中出现、用户从未操作的资料项，构造虚拟项（id=null, templateId=xxx）
     * - 单据内手动新增的实例（templateId=null）追加在后
     * - 根据申报单的流程+运输方式过滤模板（无绑定=全适用，有绑定=任一行匹配即可）
     */
    @Override
    public List<DeclarationMaterialItem> viewByFormId(Long formId) {
        List<DeclarationMaterialItem> result = new ArrayList<>();
        if (formId == null) return result;

        // 获取申报单的流程和运输方式
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = (form != null) ? form.getTemplateCode() : null;
        String formTransportMode = (form != null) ? form.getTransportMode() : null;

        List<DeclarationMaterialItem> items = listByFormId(formId);
        Map<String, DeclarationMaterialItem> itemByCode = new HashMap<>();
        List<DeclarationMaterialItem> manualItems = new ArrayList<>();
        for (DeclarationMaterialItem it : items) {
            if (it.getTemplateId() != null && it.getCode() != null) {
                itemByCode.put(it.getCode(), it);
            } else {
                manualItems.add(it);
            }
        }

        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        // 批量查询模板的绑定规则
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = Collections.emptyMap();
        if (templates != null && !templates.isEmpty()) {
            List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            tplBindingMap = templateService.batchGetBindings(tplIds);
        }

        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
                // 检查是否已有实例（有实例则始终显示，不受绑定规则过滤）
                DeclarationMaterialItem existed = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                if (existed != null) {
                    // stage 是模板属性，实例上仅为建单时克隆的缓存；同步模板最新 stage，
                    // 使老单据实例也能跟随模板的多环节配置正确归入各环节（如后加的 BASIC）
                    existed.setStage(tpl.getStage());
                    // 必填判定跟随模板最新配置重算（建单克隆的快照在模板变更后已失真），
                    // 并透传必填环节配置供前端按环节展示
                    existed.setRequired(resolveRequired(bindings, formFlowCode, formTransportMode, tpl, null));
                    existed.setRequiredStages(resolveRequiredStages(bindings, formFlowCode, formTransportMode, tpl));
                    result.add(existed);
                    continue;
                }
                // 无实例时，创建虚拟项前检查绑定规则：无绑定=全适用；有绑定=任一行匹配即可
                if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                    continue;
                }
                DeclarationMaterialItem virtual = new DeclarationMaterialItem();
                virtual.setId(null); // 虚拟标识
                virtual.setFormId(formId);
                virtual.setTemplateId(tpl.getId());
                virtual.setStage(tpl.getStage());
                virtual.setInvoiceMode(tpl.getInvoiceMode() != null ? tpl.getInvoiceMode() : 0);
                virtual.setInvoiceCategory(tpl.getInvoiceCategory());
                virtual.setCode(tpl.getCode());
                virtual.setName(tpl.getName());
                // 视图无环节上下文，传 null 回退模板默认 required（前端优先按 requiredStages 分环节展示）
                virtual.setRequired(resolveRequired(bindings, formFlowCode, formTransportMode, tpl, null));
                virtual.setRequiredStages(resolveRequiredStages(bindings, formFlowCode, formTransportMode, tpl));
                virtual.setSort(tpl.getSort() == null ? 0 : tpl.getSort());
                virtual.setRemark(tpl.getRemark());
                virtual.setFormSchema(tpl.getFormSchema());
                virtual.setStatus(0);
                // 虚拟项未落库，无 createBy/updateBy，前端按 id==null 判定显示为"—"
                result.add(virtual);
            }
        }
        result.addAll(manualItems);

        // 批量加载附件（避免 N+1）
        List<Long> realIds = result.stream()
                .map(DeclarationMaterialItem::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (!realIds.isEmpty()) {
            Map<Long, List<MaterialAttachment>> attMap = materialAttachmentService.listByItemIds(realIds);
            for (DeclarationMaterialItem it : result) {
                if (it.getId() != null) {
                    it.setAttachments(attMap.getOrDefault(it.getId(), Collections.emptyList()));
                } else {
                    it.setAttachments(Collections.emptyList());
                }
            }
        } else {
            for (DeclarationMaterialItem it : result) {
                it.setAttachments(Collections.emptyList());
            }
        }

        // 净化补交标记：只有指向当前补交单的标记才视为增量，
        // 指向已终结（通过/驳回）、已删除或废弃补交单的历史脏标记不展示
        sanitizeSupplementMarks(formId, result);

        return result;
    }

    /**
     * 批量统计资料上传进度：只统计申报单当前所处环节的资料项（列表页菜单按状态限定，
     * 状态→环节映射见 resolveStageByStatus），口径与表单页环节进度一致：
     * 分母取该环节必填项，分子取其中已上传附件（status=1）的项
     */
    @Override
    public Map<Long, Map<String, Object>> mapUploadProgress(List<Long> formIds) {
        Map<Long, Map<String, Object>> result = new LinkedHashMap<>();
        if (formIds == null || formIds.isEmpty()) return result;
        for (Long formId : formIds) {
            DeclarationForm form = declarationFormService.getById(formId);
            String stage = resolveStageByStatus(form == null ? null : form.getStatus());
            Map<String, Object> stat = new LinkedHashMap<>();
            if (stage == null) {
                // 当前状态无资料上传环节（待初审/开票金额/已完成/退回待审等）
                stat.put("total", 0);
                stat.put("required", 0);
                stat.put("uploaded", 0);
                stat.put("percent", 0);
                stat.put("stage", null);
                result.put(formId, stat);
                continue;
            }
            List<DeclarationMaterialItem> items = viewByFormId(formId);
            int total = 0;
            int required = 0;
            int uploaded = 0;
            for (DeclarationMaterialItem it : items) {
                // 仅统计归属当前环节的资料项（stage 逗号分隔多环节，空值兑底 MATERIAL_SUBMIT）
                if (!hasStage(it.getStage(), stage)) continue;
                total++;
                // 必填判定与表单页一致：优先 requiredStages 分环节配置，回退 required 字段
                boolean req = StringUtils.hasText(it.getRequiredStages())
                        ? hasStage(it.getRequiredStages(), stage)
                        : (it.getRequired() != null && it.getRequired() == 1);
                if (!req) continue;
                required++;
                if (it.getStatus() != null && it.getStatus() == 1) uploaded++;
            }
            int percent = required > 0 ? Math.round(uploaded * 100f / required) : (total > 0 ? 100 : 0);
            stat.put("total", total);
            stat.put("required", required);
            stat.put("uploaded", uploaded);
            stat.put("percent", percent);
            stat.put("stage", stage);
            result.put(formId, stat);
        }
        return result;
    }

    /** 状态 → 当前资料环节：列表页菜单按状态限定，直接映射即可 */
    private String resolveStageByStatus(Integer status) {
        if (status == null) return null;
        switch (status) {
            case 0: return "BASIC";                  // 草稿：基础资料
            case 2: case 3: return "MATERIAL_SUBMIT"; // 待资料提交/待资料审核
            case 4: case 5: return "SUPPLEMENT";      // 待补充资料提交/审核
            case 8: case 9: return "INVOICE";         // 待发票提交/审核
            default: return null;                     // 待初审/开票金额环节/已完成/退回待审：无资料上传环节
        }
    }

    /**
     * 净化补交标记：资料项/附件的 supplement_id 若未指向当前补交单（在途优先，无则最新草稿）则置空。
     * 一张申报单同一时刻只有一张有效补交单，指向其他补交单的标记均为历史脏数据
     */
    private void sanitizeSupplementMarks(Long formId, List<DeclarationMaterialItem> items) {
        MaterialSupplement current = materialSupplementService.getCurrentByFormId(formId);
        Long currentId = current != null ? current.getId() : null;
        for (DeclarationMaterialItem item : items) {
            if (item.getSupplementId() != null && !item.getSupplementId().equals(currentId)) {
                item.setSupplementId(null);
            }
            if (item.getAttachments() == null) continue;
            for (MaterialAttachment att : item.getAttachments()) {
                if (att.getSupplementId() != null && !att.getSupplementId().equals(currentId)) {
                    att.setSupplementId(null);
                }
            }
        }
    }

    /**
     * 幂等确保模板对应的实例已落库。
     * 已存在则直接返回；不存在则按模板克隆一条，创建人/更新人交由 MetaObjectHandler 填充为当前登录用户。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeclarationMaterialItem ensureItemFromTemplate(Long formId, Long templateId) {
        if (formId == null || templateId == null) {
            throw new RuntimeException("formId / templateId 不能为空");
        }
        DeclarationMaterialTemplate tpl = templateService.getById(templateId);
        if (tpl == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }
        LambdaQueryWrapper<DeclarationMaterialItem> q = new LambdaQueryWrapper<>();
        q.eq(DeclarationMaterialItem::getFormId, formId)
         .eq(DeclarationMaterialItem::getTemplateId, templateId)
         .last("limit 1");
        DeclarationMaterialItem existed = this.getOne(q);
        if (existed == null && StringUtils.hasText(tpl.getCode())) {
            LambdaQueryWrapper<DeclarationMaterialItem> q2 = new LambdaQueryWrapper<>();
            q2.eq(DeclarationMaterialItem::getFormId, formId)
              .eq(DeclarationMaterialItem::getCode, tpl.getCode())
              .last("limit 1");
            existed = this.getOne(q2);
        }
        if (existed != null) {
            return existed;
        }
        DeclarationMaterialItem item = new DeclarationMaterialItem();
        item.setFormId(formId);
        item.setTemplateId(tpl.getId());
        item.setStage(tpl.getStage());
        item.setInvoiceMode(tpl.getInvoiceMode() != null ? tpl.getInvoiceMode() : 0);
        item.setInvoiceCategory(tpl.getInvoiceCategory());
        item.setCode(tpl.getCode());
        item.setName(tpl.getName());
        item.setRequired(tpl.getRequired() == null ? 1 : tpl.getRequired());
        item.setSort(tpl.getSort() == null ? 0 : tpl.getSort());
        item.setRemark(tpl.getRemark());
        item.setFormSchema(tpl.getFormSchema());
        item.setStatus(0);
        this.save(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromTemplate(Long formId) {
        if (formId == null) {
            return 0;
        }
        // 获取申报单的流程和运输方式
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = (form != null) ? form.getTemplateCode() : null;
        String formTransportMode = (form != null) ? form.getTransportMode() : null;

        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        if (templates == null || templates.isEmpty()) {
            return 0;
        }
        // 批量查询模板的绑定规则
        List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = templateService.batchGetBindings(tplIds);

        // 已存在的 code 集合，幂等去重
        List<DeclarationMaterialItem> exists = listByFormId(formId);
        Set<String> existCodes = new HashSet<>();
        for (DeclarationMaterialItem it : exists) {
            if (it.getCode() != null) existCodes.add(it.getCode());
        }

        int inserted = 0;
        for (DeclarationMaterialTemplate tpl : templates) {
            // 绑定过滤：无绑定=全适用；有绑定=任一行匹配即可
            List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
            if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                continue;
            }

            if (tpl.getCode() != null && existCodes.contains(tpl.getCode())) {
                continue;
            }
            DeclarationMaterialItem item = new DeclarationMaterialItem();
            item.setFormId(formId);
            item.setTemplateId(tpl.getId());
            item.setStage(tpl.getStage());
            item.setInvoiceMode(tpl.getInvoiceMode() != null ? tpl.getInvoiceMode() : 0);
            item.setInvoiceCategory(tpl.getInvoiceCategory());
            item.setCode(tpl.getCode());
            item.setName(tpl.getName());
            // 建单克隆无环节上下文，传 null 回退模板默认 required
            item.setRequired(resolveRequired(bindings, formFlowCode, formTransportMode, tpl, null));
            item.setSort(tpl.getSort() == null ? 0 : tpl.getSort());
            item.setRemark(tpl.getRemark());
            item.setFormSchema(tpl.getFormSchema());
            item.setStatus(0);
            this.save(item);
            inserted++;
        }
        log.info("申报单 {} 同步资料项模板 (flow={}, transport={}) 新增 {} 条", formId, formFlowCode, formTransportMode, inserted);
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long formId, Long currentUserId, boolean skipRequiredCheck) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        // 懒创建模式下，必填校验必须基于“模板 + 手动项”而非仅仅已落库的实例
        // 只校验资料提交阶段的项，不包含补充资料(SUPPLEMENT)和发票(INVOICE)阶段
        List<DeclarationMaterialItem> items = listByFormId(formId);
        Map<String, DeclarationMaterialItem> itemByCode = new HashMap<>();
        List<DeclarationMaterialItem> manualItems = new ArrayList<>();
        for (DeclarationMaterialItem it : items) {
            if (it.getTemplateId() != null && it.getCode() != null) {
                itemByCode.put(it.getCode(), it);
            } else {
                // 手动新增项：只校验资料提交阶段的（排除基础资料/补充/发票阶段，支持多环节逗号分隔）
                if (inMaterialSubmitStage(it.getStage())) {
                    manualItems.add(it);
                }
            }
        }
        // 获取申报单的流程和运输方式
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = (form != null) ? form.getTemplateCode() : null;
        String formTransportMode = (form != null) ? form.getTransportMode() : null;
    
        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        // 批量查询模板的绑定规则
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = Collections.emptyMap();
        if (templates != null && !templates.isEmpty()) {
            List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            tplBindingMap = templateService.batchGetBindings(tplIds);
        }
    
        // 收集缺失的必填项
        List<Map<String, Object>> missingItems = new ArrayList<>();
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                // 跳过不参与资料提交阶段的模板（基础资料在草稿阶段上传，多环节时任一环节属于资料提交即参与）
                if (!inMaterialSubmitStage(tpl.getStage())) {
                    continue;
                }
                // 绑定过滤：无绑定=全适用；有绑定=任一行匹配即可
                List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
                if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                    continue;
                }
                boolean required = resolveRequired(bindings, formFlowCode, formTransportMode, tpl, "MATERIAL_SUBMIT") == 1;
                DeclarationMaterialItem it = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                boolean uploaded = it != null && it.getStatus() != null && it.getStatus() == 1
                        && it.getFileUrl() != null && !it.getFileUrl().isEmpty();
                if (required && !uploaded) {
                    if (!skipRequiredCheck) {
                        throw new RuntimeException("资料「" + tpl.getName() + "」为必填项，请先上传附件");
                    }
                    Map<String, Object> miss = new HashMap<>();
                    miss.put("code", tpl.getCode());
                    miss.put("name", tpl.getName());
                    miss.put("invoiceMode", tpl.getInvoiceMode() != null ? tpl.getInvoiceMode() : 0);
                    missingItems.add(miss);
                }
                if (it != null) {
                    String missing = validateSchemaFields(it);
                    if (missing != null) {
                        throw new RuntimeException("资料「" + tpl.getName() + "」的「" + missing + "」为必填项");
                    }
                }
            }
        }
        // 单据内手动新增的项直接校验（已过滤非补充/发票阶段）
        for (DeclarationMaterialItem it : manualItems) {
            boolean required = it.getRequired() != null && it.getRequired() == 1;
            boolean uploaded = it.getStatus() != null && it.getStatus() == 1
                    && it.getFileUrl() != null && !it.getFileUrl().isEmpty();
            if (required && !uploaded) {
                if (!skipRequiredCheck) {
                    throw new RuntimeException("资料「" + it.getName() + "」为必填项，请先上传附件");
                }
                Map<String, Object> miss = new HashMap<>();
                miss.put("code", it.getCode());
                miss.put("name", it.getName());
                miss.put("invoiceMode", it.getInvoiceMode() != null ? it.getInvoiceMode() : 0);
                missingItems.add(miss);
            }
            String missing = validateSchemaFields(it);
            if (missing != null) {
                throw new RuntimeException("资料「" + it.getName() + "」的「" + missing + "」为必填项");
            }
        }
    
        // 如果有缺失必填项且 skipRequiredCheck=true，走豁免流程
        if (!missingItems.isEmpty()) {
            // 查找当前 materialSubmit 任务（用于判断豁免时效性）
            Task currentMaterialTask = findTask(formId, "materialSubmit");
            if (currentMaterialTask == null) {
                throw new RuntimeException("当前申报单没有待提交的资料任务");
            }

            // 检查是否已有通过的豁免记录，且是在当前提交周期内通过的
            DeclarationMaterialExemption approved = exemptionService.getApprovedExemption(formId, "materialSubmit");
            boolean exemptionStillValid = false;
            if (approved != null && approved.getAuditTime() != null) {
                // 豁免通过时间必须晚于当前任务的创建时间（即本轮提交周期内通过的）
                java.util.Date taskCreateTime = currentMaterialTask.getCreateTime();
                if (taskCreateTime != null) {
                    exemptionStillValid = !approved.getAuditTime().isBefore(
                            taskCreateTime.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                }
            }

            if (exemptionStillValid) {
                // 本轮已通过豁免，继续正常提交
                log.info("申报单 {} 本轮已有通过的豁免记录 exemptionId={}，继续提交", formId, approved.getId());
            } else {
                // 检查是否已有待审核的豁免记录
                DeclarationMaterialExemption pending = exemptionService.getPendingExemption(formId, "materialSubmit");
                if (pending != null) {
                    throw new RuntimeException("已有待审核的豁免申请，请等待审核结果");
                }
                // 创建豁免记录，阻塞主流程
                String exemptionType = determineExemptionType(missingItems);
                String missingJson;
                try { missingJson = MAPPER.writeValueAsString(missingItems); }
                catch (Exception e) { missingJson = "[]"; }
                exemptionService.createExemption(formId, "materialSubmit", missingJson, exemptionType, currentMaterialTask.getId(), currentUserId);
                log.info("申报单 {} 必填不全，已创建豁免记录并阻塞主流程 taskId={}", formId, currentMaterialTask.getId());
                return; // 不 complete 任务，主流程阻塞
            }
        }
    
        // 完成 Flowable 任务 materialSubmit
        Task task = findTask(formId, "materialSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待提交的资料任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        claimIfNeeded(task, currentUserId);
        flowableTaskService.complete(task.getId(), variables);
        // 插入待审核记录（同一条，审核时 update）
        insertPendingAuditRecord(formId, BT_MATERIAL_AUDIT, currentUserId, "资料提交待审核");
        log.info("申报单 {} 资料提交完成，操作人={}", formId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long formId, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        Task task = findTask(formId, "materialAudit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待资料审核任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        if (remark != null) {
            variables.put("auditRemark", remark);
        }
        flowableTaskService.complete(task.getId(), variables);
        // 更新同单的待审核记录为审核结果
        finishAuditRecord(formId, BT_MATERIAL_AUDIT, approved, remark, auditorId);
        log.info("申报单 {} 资料审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSupplement(Long formId, Long currentUserId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }

        // 获取申报单的流程和运输方式
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = (form != null) ? form.getTemplateCode() : null;
        String formTransportMode = (form != null) ? form.getTransportMode() : null;

        // 已落库的补充资料实例（stage 支持多环节逗号分隔，内存包含匹配）
        List<DeclarationMaterialItem> supplementItems = lambdaQuery()
                .eq(DeclarationMaterialItem::getFormId, formId)
                .list().stream()
                .filter(it -> hasStage(it.getStage(), "SUPPLEMENT"))
                .collect(Collectors.toList());
        Map<String, DeclarationMaterialItem> itemByCode = new HashMap<>();
        List<DeclarationMaterialItem> manualItems = new ArrayList<>();
        for (DeclarationMaterialItem it : supplementItems) {
            if (it.getTemplateId() != null && it.getCode() != null) {
                itemByCode.put(it.getCode(), it);
            } else {
                manualItems.add(it);
            }
        }

        // 按绑定规则校验模板必填项（与 viewByFormId 前端展示逻辑一致）
        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = Collections.emptyMap();
        if (templates != null && !templates.isEmpty()) {
            List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            tplBindingMap = templateService.batchGetBindings(tplIds);
        }
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                if (!hasStage(tpl.getStage(), "SUPPLEMENT")) continue;
                // 绑定过滤：无绑定=全适用；有绑定=任一行匹配即可
                List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
                if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                    continue;
                }
                boolean required = resolveRequired(bindings, formFlowCode, formTransportMode, tpl, "SUPPLEMENT") == 1;
                DeclarationMaterialItem it = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                boolean uploaded = it != null && it.getStatus() != null && it.getStatus() == 1
                        && materialAttachmentService.countByItemId(it.getId()) > 0;
                if (required && !uploaded) {
                    throw new RuntimeException("补充资料「" + tpl.getName() + "」为必填项，请先上传附件");
                }
            }
        }
        // 手动新增项校验
        for (DeclarationMaterialItem it : manualItems) {
            if (it.getRequired() != null && it.getRequired() == 1) {
                long attCount = materialAttachmentService.countByItemId(it.getId());
                if (attCount == 0) {
                    throw new RuntimeException("补充资料「" + it.getName() + "」为必填项，请先上传附件");
                }
            }
        }

        Task task = findTask(formId, "supplementSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待补充资料提交任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        claimIfNeeded(task, currentUserId);
        flowableTaskService.complete(task.getId(), variables);
        insertPendingAuditRecord(formId, BT_SUPPLEMENT_AUDIT, currentUserId, "补充资料提交待审核");
        log.info("申报单 {} 补充资料提交完成，操作人={}", formId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditSupplement(Long formId, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        Task task = findTask(formId, "supplementAudit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待补充资料审核任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        if (remark != null) {
            variables.put("auditRemark", remark);
        }
        flowableTaskService.complete(task.getId(), variables);
        finishAuditRecord(formId, BT_SUPPLEMENT_AUDIT, approved, remark, auditorId);
        log.info("申报单 {} 补充资料审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoiceAmount(Long formId, Long currentUserId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        // 前置校验：外汇水单已关联（通过关联表检查）
        List<Map<String, Object>> relatedRemittances = remittanceService.getRemittancesByFormId(formId);
        if (relatedRemittances == null || relatedRemittances.isEmpty()) {
            throw new RuntimeException("请先提交外汇水单后再申请开票金额");
        }
        // 前置校验：退税率已由商品配置驱动，无需手动设置
        // 只需确保财务补充记录存在（发票等数据源）
        FinancialSupplement supplement = financialSupplementService.lambdaQuery()
                .eq(FinancialSupplement::getFormId, formId)
                .one();
        // 商品级退税率无需前置校验，未配置则按0%计算
        // 自动计算开票金额
        Map<String, Object> calcDetail = financialSupplementService.getCalculationDetail(formId);
        Object invoiceAmountObj = calcDetail.get("invoiceAmount");
        if (invoiceAmountObj == null) {
            throw new RuntimeException("开票金额计算失败，请检查收汇、退税、货代、报关等数据");
        }
        java.math.BigDecimal invoiceAmount = new java.math.BigDecimal(invoiceAmountObj.toString());
        // 保存到 declaration_form
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null) {
            throw new RuntimeException("申报单不存在");
        }
        form.setRequestedInvoiceAmount(invoiceAmount);
        declarationFormService.updateById(form);
        // 完成 Flowable 任务
        Task task = findTask(formId, "invoiceAmountSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待申请开票金额任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        claimIfNeeded(task, currentUserId);
        flowableTaskService.complete(task.getId(), variables);
        insertPendingAuditRecord(formId, BT_INVOICE_AMOUNT_AUDIT, currentUserId, "申请开票金额待审核");
        log.info("申报单 {} 申请开票金额完成，金额={} 操作人={}", formId, invoiceAmount, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInvoiceAmount(Long formId, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        Task task = findTask(formId, "invoiceAmountAudit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待开票金额审核任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        if (remark != null) {
            variables.put("auditRemark", remark);
        }
        flowableTaskService.complete(task.getId(), variables);
        finishAuditRecord(formId, BT_INVOICE_AMOUNT_AUDIT, approved, remark, auditorId);
        log.info("申报单 {} 开票金额审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long formId, Long currentUserId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }

        // 获取申报单的流程和运输方式
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = (form != null) ? form.getTemplateCode() : null;
        String formTransportMode = (form != null) ? form.getTransportMode() : null;

        // 按绑定规则过滤 INVOICE 阶段模板项
        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = Collections.emptyMap();
        if (templates != null && !templates.isEmpty()) {
            List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            tplBindingMap = templateService.batchGetBindings(tplIds);
        }
        // 收集绑定匹配的 INVOICE 模板 code 集合
        Set<String> applicableCodes = new HashSet<>();
        boolean hasApplicableTemplate = false;
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                if (!hasStage(tpl.getStage(), "INVOICE")) continue;
                List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
                if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                    continue;
                }
                hasApplicableTemplate = true;
                if (tpl.getCode() != null) applicableCodes.add(tpl.getCode());
            }
        }
        if (!hasApplicableTemplate) {
            throw new RuntimeException("没有业务发票资料项，请先在资料模板中配置");
        }

        // 已落库的 INVOICE 实例（stage 支持多环节逗号分隔，内存包含匹配）
        List<DeclarationMaterialItem> invoiceItems = lambdaQuery()
                .eq(DeclarationMaterialItem::getFormId, formId)
                .list().stream()
                .filter(it -> hasStage(it.getStage(), "INVOICE"))
                .collect(Collectors.toList());
        // 检查绑定匹配的资料项是否已上传附件
        boolean hasAttachment = invoiceItems.stream().anyMatch(item -> {
            // 模板项须在适用集合中，手动项始终参与
            if (item.getTemplateId() != null && item.getCode() != null && !applicableCodes.contains(item.getCode())) {
                return false;
            }
            return materialAttachmentService.countByItemId(item.getId()) > 0;
        });
        if (!hasAttachment) {
            throw new RuntimeException("请至少上传一份业务发票附件后再提交");
        }

        Task task = findTask(formId, "invoiceSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待发票提交任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        claimIfNeeded(task, currentUserId);
        flowableTaskService.complete(task.getId(), variables);
        insertPendingAuditRecord(formId, BT_INVOICE_AUDIT, currentUserId, "业务发票提交待审核");
        log.info("申报单 {} 发票提交完成，操作人={}", formId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInvoice(Long formId, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        // 审核通过时校验是否已关联至少一条已审核的收汇水单
        if (approved && !remittanceService.hasApprovedRemittance(formId)) {
            throw new RuntimeException("发票审核通过前，请先关联并审核至少一条收汇水单");
        }
        Task task = findTask(formId, "invoiceAudit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待发票审核任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        if (remark != null) {
            variables.put("auditRemark", remark);
        }
        flowableTaskService.complete(task.getId(), variables);
        // 更新同单的待审核记录为审核结果
        finishAuditRecord(formId, BT_INVOICE_AUDIT, approved, remark, auditorId);
        log.info("申报单 {} 发票审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    // ==================== 通用阶段提交（字典驱动） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStage(Long formId, String stage, Long currentUserId, boolean skipRequiredCheck) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        if (stage == null) {
            throw new RuntimeException("提交阶段不能为空");
        }

        // 从 form_section 字典动态读取配置
        JsonNode sectionConfig = findSectionConfig(stage);
        if (sectionConfig == null) {
            throw new RuntimeException("未找到阶段配置，请检查 form_section 字典 submitKey=" + stage);
        }

        String templateStage = sectionConfig.path("templateStage").asText("");
        String auditBt = sectionConfig.path("auditBt").asText(BT_MATERIAL_AUDIT);
        String attachmentMode = sectionConfig.path("attachmentMode").asText("fileUrl");
        boolean requireAnyAttachment = sectionConfig.path("requireAnyAttachment").asBoolean(false);
        boolean checkSchema = sectionConfig.path("checkSchema").asBoolean(false);
        String labelPrefix = sectionConfig.path("btnText").asText("提交").replace("提交", "").replace("审核", "");
        if (labelPrefix.isEmpty()) labelPrefix = "资料";

        // 模板阶段过滤器：完全由字典 templateStage 驱动（stage 支持多环节逗号分隔，包含匹配）
        if (templateStage.isEmpty()) {
            throw new RuntimeException("字典 form_section 配置缺少 templateStage，submitKey=" + stage);
        }
        Predicate<String> stageFilter = s -> hasStage(s, templateStage);

        // 获取该阶段的已落库实例
        List<DeclarationMaterialItem> allItems = listByFormId(formId);
        List<DeclarationMaterialItem> stageItems = new ArrayList<>();
        for (DeclarationMaterialItem it : allItems) {
            if (stageFilter.test(it.getStage() != null ? it.getStage() : "")) {
                stageItems.add(it);
            }
        }

        // 附件判断：由字典 attachmentMode 驱动
        //   fileUrl   = 单文件模式（检查 fileUrl 字段）
        //   attachment = 多附件模式（检查 material_attachment 表计数）
        Function<DeclarationMaterialItem, Boolean> attachmentChecker;
        if ("attachment".equals(attachmentMode)) {
            attachmentChecker = it -> materialAttachmentService.countByItemId(it.getId()) > 0;
        } else {
            attachmentChecker = it -> it.getStatus() != null && it.getStatus() == 1
                    && it.getFileUrl() != null && !it.getFileUrl().isEmpty();
        }

        // 通用模板绑定校验（必填按本环节 templateStage 判定）
        List<Map<String, Object>> missingItems = new ArrayList<>();
        validateStageTemplates(formId, stageFilter, stageItems, labelPrefix, attachmentChecker, skipRequiredCheck, missingItems, templateStage);

        // 如果有缺失必填项且 skipRequiredCheck=true，走豁免流程
        if (!missingItems.isEmpty()) {
            Task currentStageTask = findTask(formId, stage);
            if (currentStageTask == null) {
                throw new RuntimeException("当前申报单没有待" + labelPrefix + "提交任务");
            }

            DeclarationMaterialExemption approved = exemptionService.getApprovedExemption(formId, stage);
            boolean exemptionStillValid = false;
            if (approved != null && approved.getAuditTime() != null) {
                java.util.Date taskCreateTime = currentStageTask.getCreateTime();
                if (taskCreateTime != null) {
                    exemptionStillValid = !approved.getAuditTime().isBefore(
                            taskCreateTime.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                }
            }

            if (exemptionStillValid) {
                log.info("申报单 {} stage={} 本轮已有通过的豁免记录，继续提交", formId, stage);
            } else {
                DeclarationMaterialExemption pending = exemptionService.getPendingExemption(formId, stage);
                if (pending != null) {
                    throw new RuntimeException("已有待审核的豁免申请，请等待审核结果");
                }
                String exemptionType = determineExemptionType(missingItems);
                String missingJson;
                try { missingJson = MAPPER.writeValueAsString(missingItems); }
                catch (Exception e) { missingJson = "[]"; }
                exemptionService.createExemption(formId, stage, missingJson, exemptionType, currentStageTask.getId(), currentUserId);
                log.info("申报单 {} stage={} 必填不全，已创建豁免记录并阻塞主流程", formId, stage);
                return;
            }
        }

        // 至少一个附件校验：由字典 requireAnyAttachment 驱动
        if (requireAnyAttachment && !stageItems.isEmpty()) {
            boolean hasAny = stageItems.stream().anyMatch(it ->
                    materialAttachmentService.countByItemId(it.getId()) > 0);
            if (!hasAny) {
                throw new RuntimeException("请至少上传一份" + labelPrefix + "附件后再提交");
            }
        }

        // 结构化字段校验：由字典 checkSchema 驱动
        if (checkSchema) {
            for (DeclarationMaterialItem it : stageItems) {
                String missing = validateSchemaFields(it);
                if (missing != null) {
                    throw new RuntimeException("资料「" + it.getName() + "」的「" + missing + "」为必填项");
                }
            }
        }

        // 完成 Flowable 任务（stage 即 taskKey）
        Task task = findTask(formId, stage);
        if (task == null) {
            throw new RuntimeException("当前申报单没有待" + labelPrefix + "提交任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        claimIfNeeded(task, currentUserId);
        flowableTaskService.complete(task.getId(), variables);

        // 插入审核记录
        String desc = labelPrefix + "提交待审核";
        insertPendingAuditRecord(formId, auditBt, currentUserId, desc);
        log.info("申报单 {} 通用阶段提交 stage={} 操作人={}", formId, stage, currentUserId);
    }

    @Override
    public void validateBasicRequired(Long formId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        List<DeclarationMaterialItem> allItems = listByFormId(formId);
        List<DeclarationMaterialItem> basicItems = new ArrayList<>();
        for (DeclarationMaterialItem it : allItems) {
            if (hasStage(it.getStage() != null ? it.getStage() : "", "BASIC")) {
                basicItems.add(it);
            }
        }
        // 基础资料为多附件模式：按附件计数判定是否已上传；必填不全直接报错（不走豁免）
        Function<DeclarationMaterialItem, Boolean> checker =
                it -> it.getId() != null && materialAttachmentService.countByItemId(it.getId()) > 0;
        validateStageTemplates(formId, s -> hasStage(s, "BASIC"), basicItems, "基础资料", checker, false, null, "BASIC");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditStage(Long formId, String stage, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        if (stage == null) {
            throw new RuntimeException("审核阶段不能为空");
        }

        // 从 form_section 字典动态读取配置（按 auditTaskKey 查找）
        JsonNode sectionConfig = findSectionConfigByAuditKey(stage);
        if (sectionConfig == null) {
            throw new RuntimeException("未找到审核阶段配置，请检查 form_section 字典 auditTaskKey=" + stage);
        }

        String auditBt = sectionConfig.path("auditBt").asText("");
        boolean checkApprovedRemittance = sectionConfig.path("checkApprovedRemittance").asBoolean(false);
        String labelPrefix = sectionConfig.path("btnText").asText("提交").replace("提交", "").replace("审核", "");
        if (labelPrefix.isEmpty()) labelPrefix = "资料";

        // 审核通过前置校验：由字典 checkApprovedRemittance 驱动
        if (approved && checkApprovedRemittance) {
            if (!remittanceService.hasApprovedRemittance(formId)) {
                throw new RuntimeException(labelPrefix + "审核通过前，请先关联并审核至少一条收汇水单");
            }
        }

        // 完成 Flowable 审核任务（stage 即 auditTaskKey）
        Task task = findTask(formId, stage);
        if (task == null) {
            throw new RuntimeException("当前申报单没有待" + labelPrefix + "审核任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        if (remark != null) {
            variables.put("auditRemark", remark);
        }
        flowableTaskService.complete(task.getId(), variables);

        // 更新审核记录
        if (!auditBt.isEmpty()) {
            finishAuditRecord(formId, auditBt, approved, remark, auditorId);
        }
        log.info("申报单 {} 通用阶段审核 stage={} approved={} 审核人={}", formId, stage, approved, auditorId);
    }

    /**
     * 从 form_section 字典查找匹配 submitKey 的区块配置（解析 remark JSON）
     */
    private JsonNode findSectionConfig(String submitKey) {
        return findSectionConfigByField("submitKey", submitKey);
    }

    /**
     * 从 form_section 字典查找匹配 auditTaskKey 的区块配置（解析 remark JSON）
     */
    private JsonNode findSectionConfigByAuditKey(String auditTaskKey) {
        return findSectionConfigByField("auditTaskKey", auditTaskKey);
    }

    /**
     * 通用：按 remark JSON 中某个字段查找匹配的区块配置
     */
    private JsonNode findSectionConfigByField(String fieldName, String fieldValue) {
        List<SysDictItem> items = sysDictService.listEnabledItems("form_section");
        if (items == null) return null;
        for (SysDictItem item : items) {
            if (!StringUtils.hasText(item.getRemark())) continue;
            try {
                JsonNode config = MAPPER.readTree(item.getRemark());
                if (fieldValue.equals(config.path(fieldName).asText(""))) {
                    return config;
                }
            } catch (Exception e) {
                log.warn("解析 form_section remark 失败 itemValue={} : {}", item.getItemValue(), e.getMessage());
            }
        }
        return null;
    }

    /**
     * 通用模板绑定校验：按阶段 + 绑定规则过滤模板，用 resolveRequired 判定必填，校验附件。
     * 与 viewByFormId 前端展示逻辑一致。
     */
    private Set<String> validateStageTemplates(Long formId, Predicate<String> stageMatch,
                                                List<DeclarationMaterialItem> items, String labelPrefix,
                                                Function<DeclarationMaterialItem, Boolean> attachmentChecker) {
        return validateStageTemplates(formId, stageMatch, items, labelPrefix, attachmentChecker, false, null, null);
    }

    private Set<String> validateStageTemplates(Long formId, Predicate<String> stageMatch,
                                                List<DeclarationMaterialItem> items, String labelPrefix,
                                                Function<DeclarationMaterialItem, Boolean> attachmentChecker,
                                                boolean skipRequiredCheck, List<Map<String, Object>> missingCollector) {
        return validateStageTemplates(formId, stageMatch, items, labelPrefix, attachmentChecker, skipRequiredCheck, missingCollector, null);
    }

    /**
     * @param requiredStage 必填判定环节（字典 templateStage），null 时回退模板默认 required
     */
    private Set<String> validateStageTemplates(Long formId, Predicate<String> stageMatch,
                                                List<DeclarationMaterialItem> items, String labelPrefix,
                                                Function<DeclarationMaterialItem, Boolean> attachmentChecker,
                                                boolean skipRequiredCheck, List<Map<String, Object>> missingCollector,
                                                String requiredStage) {
        DeclarationForm form = declarationFormService.getById(formId);
        String formFlowCode = form != null ? form.getTemplateCode() : null;
        String formTransportMode = form != null ? form.getTransportMode() : null;

        // 实例按 code 索引
        Map<String, DeclarationMaterialItem> itemByCode = new HashMap<>();
        if (items != null) {
            for (DeclarationMaterialItem it : items) {
                if (it.getTemplateId() != null && it.getCode() != null) {
                    itemByCode.put(it.getCode(), it);
                }
            }
        }

        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        Map<Long, List<MaterialTemplateBinding>> tplBindingMap = Collections.emptyMap();
        if (templates != null && !templates.isEmpty()) {
            List<Long> tplIds = templates.stream().map(DeclarationMaterialTemplate::getId).collect(Collectors.toList());
            tplBindingMap = templateService.batchGetBindings(tplIds);
        }

        Set<String> applicableCodes = new HashSet<>();
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                if (!stageMatch.test(tpl.getStage())) continue;
                // 绑定过滤：无绑定=全适用；有绑定=任一行匹配即可
                List<MaterialTemplateBinding> bindings = tplBindingMap.getOrDefault(tpl.getId(), Collections.emptyList());
                if (!bindings.isEmpty() && findMatchingBinding(bindings, formFlowCode, formTransportMode) == null) {
                    continue;
                }
                applicableCodes.add(tpl.getCode());
                // 必填校验（按当前提交环节判定）
                if (resolveRequired(bindings, formFlowCode, formTransportMode, tpl, requiredStage) != 1) continue;
                DeclarationMaterialItem it = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                if (it == null || !attachmentChecker.apply(it)) {
                    if (skipRequiredCheck && missingCollector != null) {
                        Map<String, Object> miss = new HashMap<>();
                        miss.put("code", tpl.getCode());
                        miss.put("name", tpl.getName());
                        miss.put("invoiceMode", tpl.getInvoiceMode() != null ? tpl.getInvoiceMode() : 0);
                        missingCollector.add(miss);
                    } else {
                        throw new RuntimeException(labelPrefix + "「" + tpl.getName() + "」为必填项，请先上传附件");
                    }
                }
            }
        }
        return applicableCodes;
    }

    private Task findTask(Long formId, String taskKey) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(String.valueOf(formId))
                .taskDefinitionKey(taskKey)
                .list();
        return (tasks == null || tasks.isEmpty()) ? null : tasks.get(0);
    }

    /**
     * 确保当前用户可以操作该任务。
     * 如果当前用户不是 assignee，将其加入 candidateUsers，
     * 兼容旧流程实例（旧实例没有 candidateGroups）。
     */
    private void claimIfNeeded(Task task, Long currentUserId) {
        if (currentUserId == null) return;
        String assignee = task.getAssignee();
        if (assignee != null && assignee.equals(String.valueOf(currentUserId))) {
            return; // 已经是 assignee，无需处理
        }
        // 将当前用户加入候选人，确保可以完成任务
        try {
            flowableTaskService.addCandidateUser(task.getId(), String.valueOf(currentUserId));
        } catch (Exception e) {
            log.warn("添加候选人失败 taskId={} userId={}: {}", task.getId(), currentUserId, e.getMessage());
        }
    }

    /**
     * 根据 form_schema 校验必填结构化字段。返回缺失字段的 label（null = 全部合法）
     */
    private String validateSchemaFields(DeclarationMaterialItem item) {
        if (!StringUtils.hasText(item.getFormSchema())) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(item.getFormSchema());
            if (!root.isArray()) return null;
            // 解析 extra_data
            JsonNode extra = StringUtils.hasText(item.getExtraData())
                    ? MAPPER.readTree(item.getExtraData()) : null;
            for (JsonNode field : root) {
                boolean required = field.path("required").asBoolean(false);
                if (!required) continue;
                String key = field.path("key").asText();
                String label = field.path("label").asText(key);
                Object value = getFieldValue(item, key, extra);
                if (isEmpty(value)) {
                    return label;
                }
            }
        } catch (Exception e) {
            log.warn("解析 form_schema 失败 itemId={} : {}", item.getId(), e.getMessage());
        }
        return null;
    }

    /**
     * 按 key 取字段值：固定列 amount/currency/invoiceNo/invoiceDate 走实体属性，其他走 extraData JSON
     */
    private Object getFieldValue(DeclarationMaterialItem item, String key, JsonNode extra) {
        switch (key) {
            case "amount":      return item.getAmount();
            case "currency":    return item.getCurrency();
            case "invoiceNo":   return item.getInvoiceNo();
            case "invoiceDate": return item.getInvoiceDate();
            default:
                if (extra == null) return null;
                JsonNode v = extra.get(key);
                return (v == null || v.isNull()) ? null : v.asText();
        }
    }

    private boolean isEmpty(Object v) {
        if (v == null) return true;
        if (v instanceof String) return !StringUtils.hasText((String) v);
        return false;
    }

    /**
     * 根据缺失项的 invoiceMode 判断豁免类型
     * 全普通=NORMAL，全发票=INVOICE，混合=MIXED
     */
    private String determineExemptionType(List<Map<String, Object>> missingItems) {
        boolean hasNormal = false;
        boolean hasInvoice = false;
        for (Map<String, Object> item : missingItems) {
            int mode = item.get("invoiceMode") != null ? ((Number) item.get("invoiceMode")).intValue() : 0;
            if (mode == 1) hasInvoice = true;
            else hasNormal = true;
        }
        if (hasNormal && hasInvoice) return "MIXED";
        if (hasInvoice) return "INVOICE";
        return "NORMAL";
    }
}
