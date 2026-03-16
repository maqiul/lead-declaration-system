package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.ProcessDefinitionDao;
import com.declaration.entity.ProcessDefinition;
import com.declaration.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程定义服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final RepositoryService repositoryService;
    private final ProcessDefinitionDao processDefinitionDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Deployment deployProcess(String bpmnXml, ProcessDefinition processDefinition) {
        try {
            // Flowable部署
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
            deploymentBuilder.addString(processDefinition.getProcessKey() + ".bpmn20.xml", bpmnXml);
            deploymentBuilder.name(processDefinition.getProcessName());
            deploymentBuilder.category(processDefinition.getCategory());
            Deployment deployment = deploymentBuilder.deploy();

            // 保存到本地数据库
            org.flowable.engine.repository.ProcessDefinition flowableDefinition =
                repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            processDefinition.setProcessKey(flowableDefinition.getKey());
            processDefinition.setVersion(flowableDefinition.getVersion());
            processDefinition.setBpmnXml(bpmnXml);
            processDefinition.setStatus(1); // 启用状态
            
            // 设置创建人信息
            if (StpUtil.isLogin()) {
                processDefinition.setCreateBy(StpUtil.getLoginIdAsLong());
            }
            
            processDefinitionDao.insert(processDefinition);

            log.info("流程部署成功: {} - {}", processDefinition.getProcessKey(), processDefinition.getProcessName());
            return deployment;
        } catch (Exception e) {
            log.error("流程部署失败", e);
            throw new RuntimeException("流程部署失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Deployment deployProcessFromFile(MultipartFile file, ProcessDefinition processDefinition) {
        try {
            byte[] bytes = file.getBytes();
            String bpmnXml = new String(bytes, StandardCharsets.UTF_8);
            return deployProcess(bpmnXml, processDefinition);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new RuntimeException("读取文件失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Deployment deployProcessFromResource(String resourceName, InputStream inputStream, ProcessDefinition processDefinition) {
        try {
            // 使用 ByteArrayOutputStream 替代 readAllBytes
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            String bpmnXml = new String(bytes, StandardCharsets.UTF_8);
            return deployProcess(bpmnXml, processDefinition);
        } catch (IOException e) {
            log.error("读取资源失败", e);
            throw new RuntimeException("读取资源失败: " + e.getMessage());
        }
    }

    @Override
    public List<ProcessDefinition> getProcessDefinitions() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
        List<org.flowable.engine.repository.ProcessDefinition> flowableDefinitions = query.list();

        if (CollUtil.isEmpty(flowableDefinitions)) {
            return CollUtil.newArrayList();
        }

        return flowableDefinitions.stream().map(this::convertToEntity).collect(Collectors.toList());
    }

    @Override
    public ProcessDefinition getLatestProcessDefinition(String processKey) {
        org.flowable.engine.repository.ProcessDefinition flowableDefinition =
            repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();

        return flowableDefinition != null ? convertToEntity(flowableDefinition) : null;
    }

    @Override
    public void suspendProcessDefinition(String processDefinitionId) {
        repositoryService.suspendProcessDefinitionById(processDefinitionId);
        
        // 更新本地状态
        ProcessDefinition processDefinition = processDefinitionDao.selectOne(
            new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getProcessKey, processDefinitionId.split(":")[0])
        );
        if (processDefinition != null) {
            processDefinition.setStatus(0); // 停用状态
            processDefinition.setUpdateTime(LocalDateTime.now());
            processDefinitionDao.updateById(processDefinition);
        }
    }

    @Override
    public void activateProcessDefinition(String processDefinitionId) {
        repositoryService.activateProcessDefinitionById(processDefinitionId);
        
        // 更新本地状态
        ProcessDefinition processDefinition = processDefinitionDao.selectOne(
            new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getProcessKey, processDefinitionId.split(":")[0])
        );
        if (processDefinition != null) {
            processDefinition.setStatus(1); // 启用状态
            processDefinition.setUpdateTime(LocalDateTime.now());
            processDefinitionDao.updateById(processDefinition);
        }
    }

    @Override
    public void deleteProcessDefinition(String processDefinitionId, boolean cascade) {
        repositoryService.deleteDeployment(processDefinitionId, cascade);
        
        // 删除本地记录
        ProcessDefinition processDefinition = processDefinitionDao.selectOne(
            new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getProcessKey, processDefinitionId.split(":")[0])
        );
        if (processDefinition != null) {
            processDefinitionDao.deleteById(processDefinition.getId());
        }
    }

    /**
     * Flowable流程定义转换为本地实体
     */
    private ProcessDefinition convertToEntity(org.flowable.engine.repository.ProcessDefinition flowableDefinition) {
        ProcessDefinition definition = new ProcessDefinition();
        definition.setProcessKey(flowableDefinition.getKey());
        definition.setProcessName(flowableDefinition.getName());
        definition.setDescription(flowableDefinition.getDescription());
        definition.setCategory(flowableDefinition.getCategory());
        definition.setVersion(flowableDefinition.getVersion());
        definition.setStatus(flowableDefinition.isSuspended() ? 0 : 1);
        return definition;
    }
}