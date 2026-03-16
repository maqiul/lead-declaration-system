package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.declaration.dao.ProcessInstanceDao;
import com.declaration.entity.ProcessInstance;
import com.declaration.entity.TaskInstance;
import com.declaration.service.ProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInstanceServiceImpl implements ProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProcessInstanceDao processInstanceDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessInstance startProcessInstance(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        try {
            // 构建流程实例
            ProcessInstanceBuilder builder = runtimeService.createProcessInstanceBuilder();
            builder.processDefinitionKey(processDefinitionKey);
            builder.businessKey(businessKey);
            
            if (variables != null && !variables.isEmpty()) {
                builder.variables(variables);
            }

            // 启动流程
            org.flowable.engine.runtime.ProcessInstance flowableInstance = builder.start();

            // 保存到本地数据库
            ProcessInstance processInstance = new ProcessInstance();
            processInstance.setInstanceId(flowableInstance.getId());
            processInstance.setDefinitionId(flowableInstance.getProcessDefinitionId());
            processInstance.setProcessKey(processDefinitionKey);
            processInstance.setBusinessKey(businessKey);
            processInstance.setStatus(0); // 运行中
            processInstance.setStartTime(LocalDateTime.now());

            // 设置发起人信息
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                processInstance.setStarterId(userId);
                // TODO: 获取用户姓名
                processInstance.setStarterName("用户" + userId);
            }

            processInstanceDao.insert(processInstance);

            log.info("流程实例启动成功: {} - {}", processDefinitionKey, businessKey);
            return processInstance;
        } catch (Exception e) {
            log.error("启动流程实例失败", e);
            throw new RuntimeException("启动流程实例失败: " + e.getMessage());
        }
    }

    @Override
    public ProcessInstance getProcessInstance(String processInstanceId) {
        org.flowable.engine.runtime.ProcessInstance flowableInstance =
            runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (flowableInstance != null) {
            return convertRuntimeInstance(flowableInstance);
        }

        // 查询历史实例
        HistoricProcessInstance historicInstance =
            historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        return historicInstance != null ? convertHistoricInstance(historicInstance) : null;
    }

    @Override
    public List<ProcessInstance> getProcessInstancesByStarter(Long starterId) {
        List<HistoricProcessInstance> historicInstances =
            historyService.createHistoricProcessInstanceQuery()
                .startedBy(String.valueOf(starterId))
                .orderByProcessInstanceStartTime()
                .desc()
                .list();

        return historicInstances.stream()
            .map(this::convertHistoricInstance)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProcessInstance> getRunningProcessInstances() {
        List<org.flowable.engine.runtime.ProcessInstance> runtimeInstances =
            runtimeService.createProcessInstanceQuery()
                .active()
                .list();

        return runtimeInstances.stream()
            .map(this::convertRuntimeInstance)
            .collect(Collectors.toList());
    }

    @Override
    public void suspendProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        
        // 更新本地状态
        ProcessInstance processInstance = processInstanceDao.selectById(processInstanceId);
        if (processInstance != null) {
            processInstance.setStatus(3); // 已挂起
            processInstance.setUpdateTime(LocalDateTime.now());
            processInstanceDao.updateById(processInstance);
        }
    }

    @Override
    public void activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
        
        // 更新本地状态
        ProcessInstance processInstance = processInstanceDao.selectById(processInstanceId);
        if (processInstance != null) {
            processInstance.setStatus(0); // 运行中
            processInstance.setUpdateTime(LocalDateTime.now());
            processInstanceDao.updateById(processInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        
        // 更新本地状态
        ProcessInstance processInstance = processInstanceDao.selectById(processInstanceId);
        if (processInstance != null) {
            processInstance.setStatus(2); // 已终止
            processInstance.setEndTime(LocalDateTime.now());
            processInstance.setUpdateTime(LocalDateTime.now());
            processInstanceDao.updateById(processInstance);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProcessInstance(String processInstanceId, String reason) {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        
        // 删除本地记录
        processInstanceDao.deleteById(processInstanceId);
    }

    @Override
    public List<TaskInstance> getTasksByProcessInstance(String processInstanceId) {
        List<HistoricTaskInstance> historicTasks =
            historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime()
                .asc()
                .list();

        return historicTasks.stream()
            .map(this::convertHistoricTask)
            .collect(Collectors.toList());
    }

    /**
     * 运行时流程实例转换
     */
    private ProcessInstance convertRuntimeInstance(org.flowable.engine.runtime.ProcessInstance flowableInstance) {
        ProcessInstance instance = new ProcessInstance();
        // instance.setId(flowableInstance.getId());
        instance.setInstanceId(flowableInstance.getId());
        instance.setDefinitionId(flowableInstance.getProcessDefinitionId());
        instance.setProcessKey(flowableInstance.getProcessDefinitionKey());
        instance.setBusinessKey(flowableInstance.getBusinessKey());
        instance.setStatus(0); // 运行中
        instance.setStartTime(convertDateToLocalDateTime(flowableInstance.getStartTime()));
        instance.setCurrentActivityId(flowableInstance.getActivityId());
        return instance;
    }

    /**
     * 历史流程实例转换
     */
    private ProcessInstance convertHistoricInstance(HistoricProcessInstance historicInstance) {
        ProcessInstance instance = new ProcessInstance();
        // instance.setId(historicInstance.getId());
        instance.setInstanceId(historicInstance.getId());
        instance.setDefinitionId(historicInstance.getProcessDefinitionId());
        instance.setProcessKey(historicInstance.getProcessDefinitionKey());
        instance.setBusinessKey(historicInstance.getBusinessKey());
        instance.setStartTime(convertDateToLocalDateTime(historicInstance.getStartTime()));
        instance.setEndTime(convertDateToLocalDateTime(historicInstance.getEndTime()));
        instance.setStatus(historicInstance.getEndTime() != null ? 1 : 0); // 已完成或运行中
        return instance;
    }

    /**
     * 历史任务实例转换
     */
    private TaskInstance convertHistoricTask(HistoricTaskInstance historicTask) {
        TaskInstance task = new TaskInstance();
        task.setTaskId(historicTask.getId());
        task.setTaskName(historicTask.getName());
        task.setDescription(historicTask.getDescription());
        task.setInstanceId(historicTask.getProcessInstanceId());
        task.setDefinitionId(historicTask.getProcessDefinitionId());
        task.setActivityId(historicTask.getTaskDefinitionKey());
        task.setActivityName(historicTask.getName());
        if (historicTask.getAssignee() != null) {
            task.setAssigneeId(Long.valueOf(historicTask.getAssignee()));
        }
        task.setStatus(historicTask.getEndTime() != null ? 1 : 0); // 已办或待办
        task.setPriority(historicTask.getPriority());
        task.setCreateTime(convertDateToLocalDateTime(historicTask.getCreateTime()));
        task.setClaimTime(convertDateToLocalDateTime(historicTask.getClaimTime()));
        task.setEndTime(convertDateToLocalDateTime(historicTask.getEndTime()));
        return task;
    }

    /**
     * Date转换为LocalDateTime方法
     */
    private LocalDateTime convertDateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}