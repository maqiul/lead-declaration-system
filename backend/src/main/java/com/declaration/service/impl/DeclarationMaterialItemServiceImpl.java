package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationMaterialItemDao;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.User;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.DeclarationMaterialTemplateService;
import com.declaration.service.InvoiceService;
import com.declaration.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
     */
    @Override
    public List<DeclarationMaterialItem> viewByFormId(Long formId) {
        List<DeclarationMaterialItem> result = new ArrayList<>();
        if (formId == null) return result;

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
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                DeclarationMaterialItem existed = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                if (existed != null) {
                    result.add(existed);
                    continue;
                }
                DeclarationMaterialItem virtual = new DeclarationMaterialItem();
                virtual.setId(null); // 虚拟标识
                virtual.setFormId(formId);
                virtual.setTemplateId(tpl.getId());
                virtual.setCode(tpl.getCode());
                virtual.setName(tpl.getName());
                virtual.setRequired(tpl.getRequired() == null ? 1 : tpl.getRequired());
                virtual.setSort(tpl.getSort() == null ? 0 : tpl.getSort());
                virtual.setRemark(tpl.getRemark());
                virtual.setFormSchema(tpl.getFormSchema());
                virtual.setStatus(0);
                // 虚拟项未落库，无 createBy/updateBy，前端按 id==null 判定显示为“—”
                result.add(virtual);
            }
        }
        result.addAll(manualItems);
        return result;
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
        List<DeclarationMaterialTemplate> templates = templateService.listEnabled();
        if (templates == null || templates.isEmpty()) {
            return 0;
        }
        // 已存在的 code 集合，幂等去重
        List<DeclarationMaterialItem> exists = listByFormId(formId);
        Set<String> existCodes = new HashSet<>();
        for (DeclarationMaterialItem it : exists) {
            if (it.getCode() != null) existCodes.add(it.getCode());
        }

        int inserted = 0;
        for (DeclarationMaterialTemplate tpl : templates) {
            if (tpl.getCode() != null && existCodes.contains(tpl.getCode())) {
                continue;
            }
            DeclarationMaterialItem item = new DeclarationMaterialItem();
            item.setFormId(formId);
            item.setTemplateId(tpl.getId());
            item.setCode(tpl.getCode());
            item.setName(tpl.getName());
            item.setRequired(tpl.getRequired() == null ? 1 : tpl.getRequired());
            item.setSort(tpl.getSort() == null ? 0 : tpl.getSort());
            item.setRemark(tpl.getRemark());
            item.setFormSchema(tpl.getFormSchema());
            item.setStatus(0);
            this.save(item);
            inserted++;
        }
        log.info("申报单 {} 同步资料项模板 新增 {} 条", formId, inserted);
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long formId, Long currentUserId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        // 懒创建模式下，必填校验必须基于"模板 + 手动项"而非仅仅已落库的实例
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
        if (templates != null) {
            for (DeclarationMaterialTemplate tpl : templates) {
                boolean required = tpl.getRequired() != null && tpl.getRequired() == 1;
                DeclarationMaterialItem it = tpl.getCode() == null ? null : itemByCode.get(tpl.getCode());
                boolean uploaded = it != null && it.getStatus() != null && it.getStatus() == 1
                        && it.getFileUrl() != null && !it.getFileUrl().isEmpty();
                if (required && !uploaded) {
                    throw new RuntimeException("资料「" + tpl.getName() + "」为必填项，请先上传附件");
                }
                if (it != null) {
                    String missing = validateSchemaFields(it);
                    if (missing != null) {
                        throw new RuntimeException("资料「" + tpl.getName() + "」的「" + missing + "」为必填项");
                    }
                }
            }
        }
        // 单据内手动新增的项直接校验
        for (DeclarationMaterialItem it : manualItems) {
            boolean required = it.getRequired() != null && it.getRequired() == 1;
            boolean uploaded = it.getStatus() != null && it.getStatus() == 1
                    && it.getFileUrl() != null && !it.getFileUrl().isEmpty();
            if (required && !uploaded) {
                throw new RuntimeException("资料「" + it.getName() + "」为必填项，请先上传附件");
            }
            String missing = validateSchemaFields(it);
            if (missing != null) {
                throw new RuntimeException("资料「" + it.getName() + "」的「" + missing + "」为必填项");
            }
        }
        // 完成 Flowable 任务 materialSubmit
        Task task = findTask(formId, "materialSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待提交的资料任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        flowableTaskService.complete(task.getId(), variables);
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
        log.info("申报单 {} 资料审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInvoice(Long formId, Long currentUserId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
        }
        long invoiceCount = invoiceService.lambdaQuery()
                .eq(com.declaration.entity.DeclarationInvoice::getFormId, formId)
                .eq(com.declaration.entity.DeclarationInvoice::getCategory, 1)
                .eq(com.declaration.entity.DeclarationInvoice::getDeleted, 0)
                .count();
        if (invoiceCount <= 0) {
            throw new RuntimeException("请至少上传一张业务发票后再提交");
        }
        Task task = findTask(formId, "invoiceSubmit");
        if (task == null) {
            throw new RuntimeException("当前申报单没有待发票提交任务");
        }
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        flowableTaskService.complete(task.getId(), variables);
        log.info("申报单 {} 发票提交完成，操作人={}", formId, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditInvoice(Long formId, boolean approved, String remark, Long auditorId) {
        if (formId == null) {
            throw new RuntimeException("申报单ID不能为空");
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
        log.info("申报单 {} 发票审核完成 approved={} 审核人={}", formId, approved, auditorId);
    }

    private Task findTask(Long formId, String taskKey) {
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceBusinessKey(String.valueOf(formId))
                .taskDefinitionKey(taskKey)
                .list();
        return (tasks == null || tasks.isEmpty()) ? null : tasks.get(0);
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
}
