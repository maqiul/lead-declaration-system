package com.declaration.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.dao.MaterialSupplementDao;
import com.declaration.dao.MaterialSupplementFileDao;
import com.declaration.entity.BusinessAuditRecord;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationMaterialItem;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.MaterialAttachment;
import com.declaration.entity.MaterialSupplement;
import com.declaration.entity.MaterialSupplementFile;
import com.declaration.entity.User;
import com.declaration.service.BpmnGeneratorService;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationMaterialItemService;
import com.declaration.service.FlowTemplateService;
import com.declaration.service.MaterialAttachmentService;
import com.declaration.service.MaterialSupplementService;
import com.declaration.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资料补交流程服务实现
 * 独立 Flowable 流程实例，不阻塞申报主流程，增量资料审核通过才生效；
 * 流程模板（supplement_normal）未配置或启动失败时回退纯状态机模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialSupplementServiceImpl extends ServiceImpl<MaterialSupplementDao, MaterialSupplement>
        implements MaterialSupplementService {

    /** 审核留痕业务类型 */
    private static final String BUSINESS_TYPE = "MATERIAL_SUPPLEMENT";

    /** 补交流程模板编码（flow_template.code） */
    private static final String TEMPLATE_CODE = "supplement_normal";

    private final DeclarationFormService declarationFormService;
    private final DeclarationMaterialItemService itemService;
    private final MaterialAttachmentService materialAttachmentService;
    private final BusinessAuditRecordDao auditRecordDao;
    private final MaterialSupplementFileDao supplementFileDao;
    private final UserService userService;
    private final org.flowable.engine.TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final FlowTemplateService flowTemplateService;
    private final BpmnGeneratorService bpmnGeneratorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MaterialSupplement start(Long formId, String reason, Long initiatorId) {
        DeclarationForm form = declarationFormService.getById(formId);
        if (form == null) {
            throw new RuntimeException("申报单不存在");
        }
        // 补交覆盖全阶段：进入资料环节（待资料提交及之后）即可发起，支持老数据缺文件、已完成单据补交；
        // 草稿/待初审（<2）与退回待审（11）除外
        Integer status = form.getStatus();
        if (status == null || status < 2 || status == 11) {
            throw new RuntimeException("申报单尚未进入资料环节，不能发起资料补交");
        }
        if (getCurrentByFormId(formId) != null) {
            throw new RuntimeException("该申报单已有发起中的资料补交，请勿重复发起");
        }

        // 创建草稿补交单：审核人不可见，不启动流程；申报人上传完增量后点「提交补交审核」才进入审核
        MaterialSupplement supplement = new MaterialSupplement();
        supplement.setFormId(formId);
        supplement.setReason(reason);
        supplement.setStatus(-1);
        supplement.setInitiatorId(initiatorId);
        supplement.setCreateTime(LocalDateTime.now());
        this.save(supplement);
        log.info("发起资料补交(草稿) supplementId={} formId={} initiator={}", supplement.getId(), formId, initiatorId);
        return supplement;
    }

    @Override
    public void updateReason(Long supplementId, String reason, Long operatorId) {
        MaterialSupplement supplement = this.getById(supplementId);
        if (supplement == null) {
            throw new RuntimeException("补交单不存在");
        }
        // 仅草稿态可改原因：提交审核后原因随审核记录固化，不再允许修改
        if (supplement.getStatus() == null || supplement.getStatus() != -1) {
            throw new RuntimeException("仅草稿状态的补交单可修改原因");
        }
        supplement.setReason(reason == null ? "" : reason);
        this.updateById(supplement);
        log.info("更新补交原因 supplementId={} operator={}", supplementId, operatorId);
    }

    @Override
    public MaterialSupplement getCurrentByFormId(Long formId) {
        if (formId == null) return null;
        MaterialSupplement active = getActiveByFormId(formId);
        if (active != null) return active;
        LambdaQueryWrapper<MaterialSupplement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialSupplement::getFormId, formId)
                .eq(MaterialSupplement::getStatus, -1)
                .orderByDesc(MaterialSupplement::getCreateTime)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForAudit(Long supplementId, Long operatorId) {
        MaterialSupplement supplement = this.getById(supplementId);
        if (supplement == null) {
            throw new RuntimeException("补交记录不存在");
        }
        if (supplement.getStatus() == null || supplement.getStatus() != -1) {
            throw new RuntimeException("该补交单不是草稿状态，无需重复提交");
        }
        // 补交原因在提交审核时填写，未填写不允许进入审核
        if (StrUtil.isBlank(supplement.getReason())) {
            throw new RuntimeException("请填写补交原因后再提交审核");
        }
        // 必须已上传至少一条增量，避免空补交进入审核
        long attCount = materialAttachmentService.count(new LambdaQueryWrapper<MaterialAttachment>()
                .eq(MaterialAttachment::getSupplementId, supplementId));
        long itemCount = itemService.count(new LambdaQueryWrapper<DeclarationMaterialItem>()
                .eq(DeclarationMaterialItem::getSupplementId, supplementId));
        if (attCount == 0 && itemCount == 0) {
            throw new RuntimeException("请先上传补交资料后再提交审核");
        }

        supplement.setStatus(0);
        this.updateById(supplement);
        log.info("提交补交审核 supplementId={} formId={} operator={}", supplementId, supplement.getFormId(), operatorId);

        // 提交时才启动补交 Flowable 流程实例（失败则回退纯状态机模式，审核仍可用）
        try {
            String processInstanceId = startSupplementProcess(supplement);
            supplement.setProcessInstanceId(processInstanceId);
            this.updateById(supplement);
            log.info("补交流程已启动 supplementId={} processInstanceId={}", supplement.getId(), processInstanceId);
        } catch (Exception e) {
            log.warn("启动补交流程失败，回退纯状态机模式 supplementId={}: {}", supplement.getId(), e.getMessage());
        }
    }

    /**
     * 启动补交 Flowable 流程
     * 模板不存在时抛异常（由调用方回退状态机模式）；BPMN 未部署时自动部署
     */
    private String startSupplementProcess(MaterialSupplement supplement) {
        FlowTemplate template = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, TEMPLATE_CODE)
                .eq(FlowTemplate::getStatus, 1)
                .one();
        if (template == null) {
            throw new RuntimeException("补交流程模板不存在: " + TEMPLATE_CODE + "，请先在流程模板管理中配置并部署");
        }

        // 确保BPMN已部署（检查是否已有该key的流程定义）
        long defCount = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(TEMPLATE_CODE)
                .count();
        if (defCount == 0) {
            bpmnGeneratorService.generateAndDeploy(template.getId());
            log.info("补交流程BPMN首次部署: {}", TEMPLATE_CODE);
        }

        // 启动流程实例，businessKey = 补交记录ID
        Map<String, Object> variables = new HashMap<>();
        variables.put("supplementId", supplement.getId());
        variables.put("formId", supplement.getFormId());

        ProcessInstance pi = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(TEMPLATE_CODE)
                .businessKey(String.valueOf(supplement.getId()))
                .variables(variables)
                .start();

        return pi.getId();
    }

    @Override
    public MaterialSupplement getActiveByFormId(Long formId) {
        if (formId == null) return null;
        LambdaQueryWrapper<MaterialSupplement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialSupplement::getFormId, formId)
                .eq(MaterialSupplement::getStatus, 0)
                .orderByDesc(MaterialSupplement::getCreateTime)
                .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public Map<Long, Long> mapActiveByFormIds(List<Long> formIds) {
        Map<Long, Long> result = new HashMap<>();
        if (formIds == null || formIds.isEmpty()) return result;
        LambdaQueryWrapper<MaterialSupplement> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialSupplement::getFormId, formIds)
                .in(MaterialSupplement::getStatus, -1, 0)
                .orderByDesc(MaterialSupplement::getCreateTime);
        for (MaterialSupplement s : this.list(wrapper)) {
            // 按创建时间倒序遍历，同一申报单只保留最新一条在途/草稿补交单（正值=在途 status=0，负值=-id 表示草稿 status=-1）
            Long val = (s.getStatus() != null && s.getStatus() == -1) ? -s.getId() : s.getId();
            result.putIfAbsent(s.getFormId(), val);
        }
        return result;
    }

    @Override
    public List<MaterialSupplement> listPending(String declarationType) {
        LambdaQueryWrapper<MaterialSupplement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialSupplement::getStatus, 0);
        // 按申报类型（内部SELF/外部EXTERNAL）过滤：先取匹配类型的申报单ID，再限定补交单
        if (StrUtil.isNotBlank(declarationType)) {
            LambdaQueryWrapper<DeclarationForm> formWrapper = new LambdaQueryWrapper<>();
            formWrapper.eq(DeclarationForm::getDeclarationType, declarationType)
                    .select(DeclarationForm::getId);
            List<Long> formIds = declarationFormService.list(formWrapper).stream()
                    .map(DeclarationForm::getId).collect(java.util.stream.Collectors.toList());
            if (formIds.isEmpty()) {
                return new ArrayList<>();
            }
            wrapper.in(MaterialSupplement::getFormId, formIds);
        }
        wrapper.orderByDesc(MaterialSupplement::getCreateTime);
        List<MaterialSupplement> list = this.list(wrapper);
        fillDisplayNames(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long supplementId, Long operatorId) {
        MaterialSupplement supplement = this.getById(supplementId);
        if (supplement == null) {
            throw new RuntimeException("补交记录不存在");
        }
        if (supplement.getStatus() == null || supplement.getStatus() != -1) {
            throw new RuntimeException("仅草稿状态的补交单可取消");
        }
        // 删除草稿期上传的增量附件与新增资料项（存量资料不受影响），再删除补交单本身
        removeIncrements(supplementId);
        this.removeById(supplementId);
        log.info("取消资料补交(草稿) supplementId={} formId={} operator={}", supplementId, supplement.getFormId(), operatorId);
    }

    @Override
    public Map<String, Object> getIncrements(Long supplementId) {
        MaterialSupplement supplement = this.getById(supplementId);
        if (supplement == null) {
            throw new RuntimeException("补交记录不存在");
        }

        // 补交新增的资料项
        LambdaQueryWrapper<DeclarationMaterialItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(DeclarationMaterialItem::getSupplementId, supplementId)
                .orderByAsc(DeclarationMaterialItem::getSort);
        List<DeclarationMaterialItem> items = itemService.list(itemWrapper);

        // 补交新增的附件（含存量资料项上新传的补交文件）
        LambdaQueryWrapper<MaterialAttachment> attWrapper = new LambdaQueryWrapper<>();
        attWrapper.eq(MaterialAttachment::getSupplementId, supplementId)
                .orderByDesc(MaterialAttachment::getCreateTime);
        List<MaterialAttachment> attachments = materialAttachmentService.list(attWrapper);

        // 附件回填所属资料项名称与环节（审核页需标明文件属于哪个环节）：itemId 可能在本次补交新增资料项或存量资料项中
        List<Map<String, Object>> attachmentList = new java.util.ArrayList<>();
        if (!attachments.isEmpty()) {
            Map<Long, DeclarationMaterialItem> newItemMap = items.stream()
                    .collect(java.util.stream.Collectors.toMap(DeclarationMaterialItem::getId, i -> i, (a, b) -> a));
            Set<Long> restItemIds = attachments.stream().map(MaterialAttachment::getItemId)
                    .filter(java.util.Objects::nonNull).filter(id -> !newItemMap.containsKey(id))
                    .collect(java.util.stream.Collectors.toSet());
            Map<Long, DeclarationMaterialItem> existItemMap = new HashMap<>();
            if (!restItemIds.isEmpty()) {
                itemService.listByIds(restItemIds).forEach(i -> existItemMap.put(i.getId(), i));
            }
            for (MaterialAttachment att : attachments) {
                Map<String, Object> vo = new LinkedHashMap<>();
                vo.put("id", att.getId());
                vo.put("itemId", att.getItemId());
                vo.put("fileName", att.getFileName());
                vo.put("fileUrl", att.getFileUrl());
                vo.put("uploadTime", att.getUploadTime());
                DeclarationMaterialItem item = att.getItemId() == null ? null
                        : newItemMap.getOrDefault(att.getItemId(), existItemMap.get(att.getItemId()));
                vo.put("itemName", item != null ? item.getName() : null);
                // 提交环节：附件自身 stage 为上传时记录的所处环节（单值）；资料项 stage 是多环节归属，仅兼容老数据回退
                vo.put("stage", att.getStage() != null ? att.getStage() : (item != null ? item.getStage() : null));
                attachmentList.add(vo);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("supplement", supplement);
        result.put("items", items);
        result.put("attachments", attachmentList);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long supplementId, boolean approved, String remark, Long auditorId) {
        MaterialSupplement supplement = this.getById(supplementId);
        if (supplement == null) {
            throw new RuntimeException("补交记录不存在");
        }
        if (supplement.getStatus() == null || supplement.getStatus() != 0) {
            throw new RuntimeException("该补交记录已审核，请勿重复操作");
        }

        String processInstanceId = supplement.getProcessInstanceId();
        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            // 有Flowable流程实例：先记录审核人/备注，再通过 complete 任务推进流程，
            // 流程结束时由 SupplementTaskListener 落地审核结果
            supplement.setAuditorId(auditorId);
            supplement.setAuditRemark(remark);
            this.updateById(supplement);
            auditViaFlowable(supplement, approved, remark);
        } else {
            // 无流程实例（旧数据/回退模式）：直接落地审核结果
            applyAuditResult(supplement, approved, remark, auditorId);
        }
        log.info("资料补交审核 supplementId={} approved={} auditor={}", supplementId, approved, auditorId);
    }

    /**
     * 通过 Flowable 流程审核补交
     * 找到当前活跃任务并 complete，流程监听器负责后续增量转正/清除
     */
    private void auditViaFlowable(MaterialSupplement supplement, boolean approved, String remark) {
        String processInstanceId = supplement.getProcessInstanceId();

        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (tasks.isEmpty()) {
            log.warn("补交流程无活跃任务，回退为直接落地 supplementId={}", supplement.getId());
            applyAuditResult(supplement, approved, remark, supplement.getAuditorId());
            return;
        }

        Task currentTask = tasks.get(0);
        log.info("补交审核: 当前任务={} ({}) supplementId={} approved={}",
                currentTask.getName(), currentTask.getTaskDefinitionKey(), supplement.getId(), approved);

        if (approved) {
            // 通过：complete当前任务，流程自动推进
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", true);
            flowableTaskService.complete(currentTask.getId(), variables);

            // 检查流程是否已结束（最后一步审核通过）
            ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (pi == null) {
                // 流程已结束，SupplementTaskListener 已落地审核结果
                log.info("补交流程已结束(全部审核通过) supplementId={}", supplement.getId());
            } else {
                // 流程未结束，还有下一步审核（多步模板）
                supplement.setAuditRemark("当前步骤审核通过，等待下一步审核");
                this.updateById(supplement);
                log.info("补交流程进入下一步审核 supplementId={}", supplement.getId());
            }
        } else {
            // 驳回：终止流程实例（deleteProcessInstance 不触发 end 监听器）后直接落地
            runtimeService.setVariable(processInstanceId, "approved", false);
            runtimeService.deleteProcessInstance(processInstanceId, "补交审核驳回: " + remark);
            applyAuditResult(supplement, false, remark, supplement.getAuditorId());
            log.info("补交流程驳回并终止 supplementId={}", supplement.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyAuditResult(MaterialSupplement supplement, boolean approved, String remark, Long auditorId) {
        // 已审核过的记录不重复落地（防止监听器与直接路径叠加）
        if (supplement.getStatus() != null && supplement.getStatus() != 0) {
            log.info("补交 {} 已审核(status={})，跳过重复落地", supplement.getId(), supplement.getStatus());
            return;
        }

        Long supplementId = supplement.getId();
        LocalDateTime now = LocalDateTime.now();

        // 文件快照留档：记录本次补交了哪些文件（通过/驳回均记录，需在清标/删除增量之前执行）
        try {
            snapshotFiles(supplement);
        } catch (Exception e) {
            log.error("补交文件快照留档失败 supplementId={}", supplementId, e);
        }

        if (approved) {
            // 通过：增量转正，清除 supplement_id 标记（保留上传时 stage）
            LambdaUpdateWrapper<MaterialAttachment> attUpdate = new LambdaUpdateWrapper<>();
            attUpdate.eq(MaterialAttachment::getSupplementId, supplementId)
                    .set(MaterialAttachment::getSupplementId, null);
            materialAttachmentService.update(attUpdate);

            LambdaUpdateWrapper<DeclarationMaterialItem> itemUpdate = new LambdaUpdateWrapper<>();
            itemUpdate.eq(DeclarationMaterialItem::getSupplementId, supplementId)
                    .set(DeclarationMaterialItem::getSupplementId, null);
            itemService.update(itemUpdate);
        } else {
            // 驳回：删除增量附件与补交新增的资料项（存量资料不受影响）
            removeIncrements(supplementId);
        }

        supplement.setStatus(approved ? 1 : 2);
        supplement.setAuditorId(auditorId);
        supplement.setAuditRemark(remark);
        supplement.setAuditTime(now);
        this.updateById(supplement);

        // 审核留痕
        try {
            BusinessAuditRecord record = new BusinessAuditRecord();
            record.setBusinessId(supplement.getFormId());
            record.setBusinessType(BUSINESS_TYPE);
            record.setApplicantId(supplement.getInitiatorId());
            record.setApplyReason(supplement.getReason());
            record.setApplyTime(supplement.getCreateTime());
            record.setAuditorId(auditorId);
            record.setAuditStatus(approved ? 1 : 2);
            record.setAuditRemark(remark);
            record.setAuditTime(now);
            auditRecordDao.insert(record);
        } catch (Exception e) {
            log.error("补交审核留痕失败 supplementId={}", supplementId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupByFormId(Long formId) {
        List<MaterialSupplement> supplements = this.list(new LambdaQueryWrapper<MaterialSupplement>()
                .eq(MaterialSupplement::getFormId, formId));
        if (supplements.isEmpty()) {
            return;
        }
        for (MaterialSupplement supplement : supplements) {
            // 终止关联的 Flowable 流程实例
            String piId = supplement.getProcessInstanceId();
            if (piId != null && !piId.isEmpty()) {
                try {
                    ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                            .processInstanceId(piId)
                            .singleResult();
                    if (pi != null) {
                        runtimeService.deleteProcessInstance(piId, "申报单退回草稿，清理补交流程");
                        log.info("已终止补交流程实例 supplementId={} processInstanceId={}", supplement.getId(), piId);
                    }
                } catch (Exception e) {
                    log.warn("终止补交流程实例失败 supplementId={}: {}", supplement.getId(), e.getMessage());
                }
            }
            // 删除补交增量（未转正的附件与资料项）
            removeIncrements(supplement.getId());
        }
        // 删除所有补交记录
        this.remove(new LambdaQueryWrapper<MaterialSupplement>()
                .eq(MaterialSupplement::getFormId, formId));
        log.info("已清理申报单 {} 的 {} 条补交记录", formId, supplements.size());
    }

    /** 删除补交增量：打标附件 + 补交新增的资料项 */
    private void removeIncrements(Long supplementId) {
        LambdaQueryWrapper<MaterialAttachment> attDelete = new LambdaQueryWrapper<>();
        attDelete.eq(MaterialAttachment::getSupplementId, supplementId);
        materialAttachmentService.remove(attDelete);

        LambdaQueryWrapper<DeclarationMaterialItem> itemDelete = new LambdaQueryWrapper<>();
        itemDelete.eq(DeclarationMaterialItem::getSupplementId, supplementId);
        itemService.remove(itemDelete);
    }

    /**
     * 补交文件快照留档：审核落地时把本次补交的增量文件拷入快照表。
     * 通过后 supplement_id 会清标、驳回后增量会删除，快照保证"哪次补交了哪些文件"可追溯
     */
    private void snapshotFiles(MaterialSupplement supplement) {
        Long sid = supplement.getId();
        Long existCount = supplementFileDao.selectCount(new LambdaQueryWrapper<MaterialSupplementFile>()
                .eq(MaterialSupplementFile::getSupplementId, sid));
        if (existCount != null && existCount > 0) {
            return; // 已有快照（防监听器与直接路径重复落地）
        }
        List<MaterialAttachment> attachments = materialAttachmentService.list(new LambdaQueryWrapper<MaterialAttachment>()
                .eq(MaterialAttachment::getSupplementId, sid));
        if (attachments.isEmpty()) {
            return;
        }
        // 资料项名称映射（含补交新增的自定义项，驳回删除前仍可查到）
        Set<Long> itemIds = new HashSet<>();
        for (MaterialAttachment att : attachments) {
            if (att.getItemId() != null) itemIds.add(att.getItemId());
        }
        Map<Long, String> itemNameMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<DeclarationMaterialItem> items = itemService.listByIds(itemIds);
            for (DeclarationMaterialItem it : items) {
                itemNameMap.put(it.getId(), it.getName());
            }
        }
        LocalDateTime snapTime = LocalDateTime.now();
        for (MaterialAttachment att : attachments) {
            MaterialSupplementFile file = new MaterialSupplementFile();
            file.setSupplementId(sid);
            file.setFormId(supplement.getFormId());
            file.setItemId(att.getItemId());
            file.setItemName(itemNameMap.get(att.getItemId()));
            file.setAttachmentId(att.getId());
            file.setFileName(att.getFileName());
            file.setFileUrl(att.getFileUrl());
            file.setFileSize(att.getFileSize());
            file.setStage(att.getStage());
            file.setUploadBy(att.getUploadBy());
            file.setUploadTime(att.getUploadTime());
            file.setCreateTime(snapTime);
            supplementFileDao.insert(file);
        }
        log.info("补交文件快照留档完成 supplementId={} count={}", sid, attachments.size());
    }

    @Override
    public List<MaterialSupplement> listHistoryByFormId(Long formId) {
        if (formId == null) return Collections.emptyList();
        LambdaQueryWrapper<MaterialSupplement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialSupplement::getFormId, formId)
                .orderByDesc(MaterialSupplement::getCreateTime);
        List<MaterialSupplement> list = this.list(wrapper);
        if (list.isEmpty()) return list;
        fillDisplayNames(list);

        // 回填每次补交的文件快照
        List<Long> sids = list.stream().map(MaterialSupplement::getId).collect(Collectors.toList());
        List<MaterialSupplementFile> files = supplementFileDao.selectList(new LambdaQueryWrapper<MaterialSupplementFile>()
                .in(MaterialSupplementFile::getSupplementId, sids)
                .orderByAsc(MaterialSupplementFile::getId));
        // 上传人显示名
        Set<Long> uploaderIds = new HashSet<>();
        for (MaterialSupplementFile f : files) {
            if (f.getUploadBy() != null) uploaderIds.add(f.getUploadBy());
        }
        Map<Long, String> uploaderNameMap = new HashMap<>();
        if (!uploaderIds.isEmpty()) {
            try {
                List<User> users = userService.listByIds(uploaderIds);
                for (User u : users) {
                    String display = (u.getNickname() != null && !u.getNickname().isBlank())
                            ? u.getNickname() : u.getUsername();
                    uploaderNameMap.put(u.getId(), display);
                }
            } catch (Exception e) {
                log.warn("回填补交快照上传人名失败：{}", e.getMessage());
            }
        }
        for (MaterialSupplementFile f : files) {
            if (f.getUploadBy() != null) f.setUploadByName(uploaderNameMap.get(f.getUploadBy()));
        }
        Map<Long, List<MaterialSupplementFile>> fileMap = files.stream()
                .collect(Collectors.groupingBy(MaterialSupplementFile::getSupplementId));
        for (MaterialSupplement s : list) {
            s.setFiles(fileMap.getOrDefault(s.getId(), Collections.emptyList()));
        }
        return list;
    }

    /** 批量回填发起人/审核人显示名与申报单号 */
    private void fillDisplayNames(List<MaterialSupplement> list) {
        if (list == null || list.isEmpty()) return;

        Set<Long> userIds = new HashSet<>();
        Set<Long> formIds = new HashSet<>();
        for (MaterialSupplement s : list) {
            if (s.getInitiatorId() != null) userIds.add(s.getInitiatorId());
            if (s.getAuditorId() != null) userIds.add(s.getAuditorId());
            if (s.getFormId() != null) formIds.add(s.getFormId());
        }

        Map<Long, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            try {
                List<User> users = userService.listByIds(userIds);
                for (User u : users) {
                    String display = (u.getNickname() != null && !u.getNickname().isBlank())
                            ? u.getNickname() : u.getUsername();
                    nameMap.put(u.getId(), display);
                }
            } catch (Exception e) {
                log.warn("回填补交记录人名失败：{}", e.getMessage());
            }
        }

        Map<Long, String> formNoMap = new HashMap<>();
        if (!formIds.isEmpty()) {
            try {
                List<DeclarationForm> forms = declarationFormService.listByIds(formIds);
                for (DeclarationForm f : forms) {
                    formNoMap.put(f.getId(), f.getFormNo());
                }
            } catch (Exception e) {
                log.warn("回填补交记录申报单号失败：{}", e.getMessage());
            }
        }

        for (MaterialSupplement s : list) {
            if (s.getInitiatorId() != null) s.setInitiatorName(nameMap.get(s.getInitiatorId()));
            if (s.getAuditorId() != null) s.setAuditorName(nameMap.get(s.getAuditorId()));
            if (s.getFormId() != null) s.setFormNo(formNoMap.get(s.getFormId()));
        }
    }
}
