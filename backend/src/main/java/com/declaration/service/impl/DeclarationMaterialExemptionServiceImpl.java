package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationMaterialExemptionDao;
import com.declaration.entity.DeclarationMaterialExemption;
import com.declaration.entity.FlowTemplate;
import com.declaration.service.BpmnGeneratorService;
import com.declaration.service.DeclarationMaterialExemptionService;
import com.declaration.service.FlowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资料豁免审批服务实现
 * 集成 Flowable 流程引擎，创建豁免时启动独立流程实例，审核时 complete 流程任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationMaterialExemptionServiceImpl
        extends ServiceImpl<DeclarationMaterialExemptionDao, DeclarationMaterialExemption>
        implements DeclarationMaterialExemptionService {

    private final TaskService flowableTaskService;
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final FlowTemplateService flowTemplateService;
    private final BpmnGeneratorService bpmnGeneratorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExemption(Long formId, String stage, String missingItemsJson,
                                String exemptionType, String mainTaskId, Long createBy) {
        DeclarationMaterialExemption exemption = new DeclarationMaterialExemption();
        exemption.setFormId(formId);
        exemption.setStage(stage);
        exemption.setMissingItems(missingItemsJson);
        exemption.setExemptionType(exemptionType);
        exemption.setStatus(0); // 待审核
        exemption.setMainTaskId(mainTaskId);
        exemption.setCreateBy(createBy);
        exemption.setCreateTime(LocalDateTime.now());
        exemption.setUpdateTime(LocalDateTime.now());
        this.save(exemption);
        log.info("创建豁免记录 id={} formId={} stage={} type={} mainTaskId={}",
                exemption.getId(), formId, stage, exemptionType, mainTaskId);

        // 启动豁免 Flowable 流程实例
        try {
            String processInstanceId = startExemptionProcess(exemption);
            exemption.setProcessInstanceId(processInstanceId);
            this.updateById(exemption);
            log.info("豁免流程已启动 exemptionId={} processInstanceId={}", exemption.getId(), processInstanceId);
        } catch (Exception e) {
            log.error("启动豁免流程失败 exemptionId={}: {}", exemption.getId(), e.getMessage(), e);
            // 流程启动失败不影响豁免记录创建，回退为纯状态模式
        }

        return exemption.getId();
    }

    /**
     * 启动豁免 Flowable 流程
     * 根据豁免类型选择对应模板：
     * - NORMAL → exemption_normal (1步审核)
     * - INVOICE/MIXED → exemption_invoice (2步审核)
     */
    private String startExemptionProcess(DeclarationMaterialExemption exemption) {
        // 确定模板编码
        String templateCode = "INVOICE".equals(exemption.getExemptionType())
                || "MIXED".equals(exemption.getExemptionType())
                ? "exemption_invoice" : "exemption_normal";

        // 查找模板
        FlowTemplate template = flowTemplateService.lambdaQuery()
                .eq(FlowTemplate::getCode, templateCode)
                .eq(FlowTemplate::getStatus, 1)
                .one();
        if (template == null) {
            throw new RuntimeException("豁免流程模板不存在: " + templateCode + "，请先在流程模板管理中配置并部署");
        }

        // 确保BPMN已部署（检查是否已有该key的流程定义）
        long defCount = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(templateCode)
                .count();
        if (defCount == 0) {
            try {
                bpmnGeneratorService.generateAndDeploy(template.getId());
                log.info("豁免流程BPMN首次部署: {}", templateCode);
            } catch (Exception e) {
                throw new RuntimeException("豁免流程BPMN部署失败: " + e.getMessage());
            }
        }

        // 启动流程实例，businessKey = 豁免记录ID
        Map<String, Object> variables = new HashMap<>();
        variables.put("exemptionId", exemption.getId());
        variables.put("exemptionType", exemption.getExemptionType());
        variables.put("formId", exemption.getFormId());

        ProcessInstance pi = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey(templateCode)
                .businessKey(String.valueOf(exemption.getId()))
                .variables(variables)
                .start();

        return pi.getId();
    }

    @Override
    public DeclarationMaterialExemption getPendingExemption(Long formId, String stage) {
        return this.getOne(new LambdaQueryWrapper<DeclarationMaterialExemption>()
                .eq(DeclarationMaterialExemption::getFormId, formId)
                .eq(DeclarationMaterialExemption::getStage, stage)
                .eq(DeclarationMaterialExemption::getStatus, 0)
                .orderByDesc(DeclarationMaterialExemption::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    public DeclarationMaterialExemption getApprovedExemption(Long formId, String stage) {
        return this.getOne(new LambdaQueryWrapper<DeclarationMaterialExemption>()
                .eq(DeclarationMaterialExemption::getFormId, formId)
                .eq(DeclarationMaterialExemption::getStage, stage)
                .eq(DeclarationMaterialExemption::getStatus, 1)
                .orderByDesc(DeclarationMaterialExemption::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    public List<DeclarationMaterialExemption> listByFormId(Long formId) {
        return this.list(new LambdaQueryWrapper<DeclarationMaterialExemption>()
                .eq(DeclarationMaterialExemption::getFormId, formId)
                .orderByDesc(DeclarationMaterialExemption::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditExemption(Long exemptionId, boolean approved, String remark, Long auditorId) {
        DeclarationMaterialExemption exemption = this.getById(exemptionId);
        if (exemption == null) {
            throw new RuntimeException("豁免记录不存在");
        }
        if (exemption.getStatus() != 0) {
            throw new RuntimeException("该豁免记录已审核，不可重复操作");
        }

        // 更新审核人信息
        exemption.setAuditBy(auditorId);
        exemption.setAuditRemark(remark);
        exemption.setUpdateTime(LocalDateTime.now());

        String processInstanceId = exemption.getProcessInstanceId();

        if (processInstanceId != null && !processInstanceId.isEmpty()) {
            // 有Flowable流程实例：通过complete任务来推进流程
            auditViaFlowable(exemption, approved, remark);
        } else {
            // 无流程实例（兼容旧数据）：直接更新状态
            auditDirectly(exemption, approved);
        }
    }

    /**
     * 通过 Flowable 流程审核豁免
     * 找到当前活跃任务并 complete，流程监听器负责后续状态同步和主流程回调
     */
    private void auditViaFlowable(DeclarationMaterialExemption exemption, boolean approved, String remark) {
        String processInstanceId = exemption.getProcessInstanceId();

        // 查找当前活跃任务
        List<Task> tasks = flowableTaskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        if (tasks.isEmpty()) {
            log.warn("豁免流程无活跃任务，回退为直接状态更新 exemptionId={}", exemption.getId());
            auditDirectly(exemption, approved);
            return;
        }

        Task currentTask = tasks.get(0);
        log.info("豁免审核: 当前任务={} ({}) exemptionId={} approved={}",
                currentTask.getName(), currentTask.getTaskDefinitionKey(), exemption.getId(), approved);

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
                // 流程已结束，ExemptionTaskListener 已处理状态更新和主流程回调
                log.info("豁免流程已结束(全部审核通过) exemptionId={}", exemption.getId());
            } else {
                // 流程未结束，还有下一步审核（如发票类第二步）
                exemption.setAuditRemark("第一步审核通过，等待下一步审核");
                this.updateById(exemption);
                log.info("豁免流程进入下一步审核 exemptionId={}", exemption.getId());
            }
        } else {
            // 驳回：终止流程实例 + 更新状态
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            // 先设置流程变量，再删除流程实例
            runtimeService.setVariable(processInstanceId, "approved", false);
            runtimeService.deleteProcessInstance(processInstanceId, "豁免审核驳回: " + remark);

            // 更新豁免记录状态
            exemption.setStatus(2); // 已驳回
            exemption.setAuditTime(LocalDateTime.now());
            this.updateById(exemption);
            log.info("豁免流程驳回并终止 exemptionId={}", exemption.getId());
        }
    }

    /**
     * 直接状态更新（兼容无流程实例的旧数据）
     */
    private void auditDirectly(DeclarationMaterialExemption exemption, boolean approved) {
        exemption.setStatus(approved ? 1 : 2);
        exemption.setAuditTime(LocalDateTime.now());
        this.updateById(exemption);

        if (approved) {
            // 通过：complete 主流程被阻塞的任务
            String mainTaskId = exemption.getMainTaskId();
            if (mainTaskId != null && !mainTaskId.isEmpty()) {
                try {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("approved", true);
                    variables.put("exemptionApproved", true);
                    flowableTaskService.complete(mainTaskId, variables);
                    log.info("豁免通过(直接模式)，已complete主流程任务 exemptionId={} mainTaskId={}",
                            exemption.getId(), mainTaskId);
                } catch (Exception e) {
                    log.error("豁免通过后complete主流程任务失败 exemptionId={}: {}",
                            exemption.getId(), e.getMessage());
                    throw new RuntimeException("豁免通过但主流程任务完成失败：" + e.getMessage());
                }
            }
        } else {
            log.info("豁免驳回(直接模式) exemptionId={}", exemption.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupByFormId(Long formId) {
        List<DeclarationMaterialExemption> exemptions = listByFormId(formId);
        if (exemptions.isEmpty()) {
            return;
        }
        for (DeclarationMaterialExemption exemption : exemptions) {
            // 终止关联的 Flowable 流程实例
            String piId = exemption.getProcessInstanceId();
            if (piId != null && !piId.isEmpty()) {
                try {
                    ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                            .processInstanceId(piId)
                            .singleResult();
                    if (pi != null) {
                        runtimeService.deleteProcessInstance(piId, "申报单退回草稿，清理豁免流程");
                        log.info("已终止豁免流程实例 exemptionId={} processInstanceId={}", exemption.getId(), piId);
                    }
                } catch (Exception e) {
                    log.warn("终止豁免流程实例失败 exemptionId={}: {}", exemption.getId(), e.getMessage());
                }
            }
        }
        // 删除所有豁免记录
        this.remove(new LambdaQueryWrapper<DeclarationMaterialExemption>()
                .eq(DeclarationMaterialExemption::getFormId, formId));
        log.info("已清理申报单 {} 的 {} 条豁免记录", formId, exemptions.size());
    }
}
