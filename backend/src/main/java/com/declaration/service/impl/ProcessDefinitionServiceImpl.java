package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.declaration.dao.ProcessDefinitionDao;
import com.declaration.entity.ProcessDefinition;
import com.declaration.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.springframework.stereotype.Service;
import com.declaration.util.FlowableCleanupUtil;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import cn.dev33.satoken.stp.StpUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

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
    private final FlowableCleanupUtil cleanupUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Deployment deployProcess(String bpmnXml, ProcessDefinition processDefinition) {
        try {
            // Flowable部署
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
            // 确保文件名以 .bpmn20.xml 结尾，否则 Flowable 可能不解析
            String resourceName = processDefinition.getProcessKey() + ".bpmn20.xml";
            deploymentBuilder.addString(resourceName, bpmnXml);
            deploymentBuilder.name(processDefinition.getProcessName());
            deploymentBuilder.category(processDefinition.getCategory());
            Deployment deployment = deploymentBuilder.deploy();

            // 获取部署后的流程定义详细信息
            org.flowable.engine.repository.ProcessDefinition flowableDefinition =
                repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            if (flowableDefinition == null) {
                throw new RuntimeException("部署成功但未能获取流程定义信息，请检查 XML 是否包含有效的可执行流程");
            }

            // 更新或保存到本地数据库
            ProcessDefinition existing = processDefinitionDao.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>()
                    .eq(ProcessDefinition::getProcessKey, flowableDefinition.getKey())
            );

            if (existing != null) {
                existing.setProcessName(processDefinition.getProcessName());
                existing.setCategory(processDefinition.getCategory());
                existing.setDescription(processDefinition.getDescription());
                existing.setBpmnXml(bpmnXml);
                existing.setVersion(flowableDefinition.getVersion());
                existing.setDeploymentId(flowableDefinition.getId()); // 存储真正的 ProcessDefinitionId
                existing.setStatus(flowableDefinition.isSuspended() ? 0 : 1);
                processDefinitionDao.updateById(existing);
                processDefinition.setId(existing.getId());
            } else {
                processDefinition.setProcessKey(flowableDefinition.getKey());
                processDefinition.setVersion(flowableDefinition.getVersion());
                processDefinition.setDeploymentId(flowableDefinition.getId());
                processDefinition.setBpmnXml(bpmnXml);
                processDefinition.setStatus(flowableDefinition.isSuspended() ? 0 : 1);
                
                if (StpUtil.isLogin()) {
                    processDefinition.setCreateBy(StpUtil.getLoginIdAsLong());
                }
                processDefinitionDao.insert(processDefinition);
            }

            log.info("流程部署成功: {} - ID: {}", processDefinition.getProcessKey(), flowableDefinition.getId());
            return deployment;
        } catch (Exception e) {
            log.error("流程部署失败", e);
            throw new RuntimeException("流程部署失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Deployment deployProcess(InputStream inputStream, String processName, String processKey, String category, String description) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            String bpmnXml = new String(bytes, StandardCharsets.UTF_8);

            ProcessDefinition processDefinition = new ProcessDefinition();
            processDefinition.setProcessName(processName);
            processDefinition.setProcessKey(processKey);
            processDefinition.setCategory(category);
            processDefinition.setDescription(description);

            return deployProcess(bpmnXml, processDefinition);
        } catch (IOException e) {
            log.error("读取流程流失败", e);
            throw new RuntimeException("读取流程流失败: " + e.getMessage());
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
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
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
        // 1. 先获取本地所有记录
        List<ProcessDefinition> list = processDefinitionDao.selectList(
            new LambdaQueryWrapper<ProcessDefinition>()
                .orderByDesc(ProcessDefinition::getCreateTime)
        );

        // 2. 从 Flowable 获取所有最新版本的定义
        List<org.flowable.engine.repository.ProcessDefinition> flowableDefinitions = 
            repositoryService.createProcessDefinitionQuery().latestVersion().list();
            
        // 3. 循环同步：以 Flowable 为准，补齐或更新本地元数据
        for (org.flowable.engine.repository.ProcessDefinition flowDef : flowableDefinitions) {
            ProcessDefinition localDef = list.stream()
                .filter(d -> d.getProcessKey().equals(flowDef.getKey()))
                .findFirst()
                .orElse(null);
                
            if (localDef == null) {
                // 补偿插入：本地表缺失记录（如通过 API 或工具直接部署的）
                ProcessDefinition newDef = convertToEntity(flowDef);
                newDef.setDeploymentId(flowDef.getId()); // 设置真正的流程定义ID
                newDef.setStatus(flowDef.isSuspended() ? 0 : 1);
                processDefinitionDao.insert(newDef);
                list.add(newDef);
            } else {
                // 状态/版本同步：本地表已有记录，更新其运行态数据
                localDef.setStatus(flowDef.isSuspended() ? 0 : 1);
                localDef.setVersion(flowDef.getVersion());
                localDef.setDeploymentId(flowDef.getId());
            }
        }

        return list;
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
    public String getProcessDefinitionXml(String processKey) {
        ProcessDefinition processDefinition = processDefinitionDao.selectOne(
            new LambdaQueryWrapper<ProcessDefinition>()
                .eq(ProcessDefinition::getProcessKey, processKey)
                .orderByDesc(ProcessDefinition::getId)
                .last("LIMIT 1")
        );
        return processDefinition != null ? processDefinition.getBpmnXml() : null;
    }

    @Override
    public void suspendProcessDefinition(String idOrKey) {
        String processDefinitionId = idOrKey;
        // 如果是纯数字，认为是本地数据库 ID
        if (idOrKey.matches("^\\d+$")) {
            ProcessDefinition def = processDefinitionDao.selectById(Long.parseLong(idOrKey));
            if (def != null && def.getDeploymentId() != null) {
                processDefinitionId = def.getDeploymentId();
            }
        }
        
        repositoryService.suspendProcessDefinitionById(processDefinitionId, true, null);
        
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
    public void activateProcessDefinition(String idOrKey) {
        String processDefinitionId = idOrKey;
        if (idOrKey.matches("^\\d+$")) {
            ProcessDefinition def = processDefinitionDao.selectById(Long.parseLong(idOrKey));
            if (def != null && def.getDeploymentId() != null) {
                processDefinitionId = def.getDeploymentId();
            }
        }

        repositoryService.activateProcessDefinitionById(processDefinitionId, true, null);
        
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
    public void deleteProcessDefinition(String idOrKey, boolean cascade) {
        String processDefinitionId = idOrKey;
        ProcessDefinition localDef = null;
        
        if (idOrKey.matches("^\\d+$")) {
            localDef = processDefinitionDao.selectById(Long.parseLong(idOrKey));
            if (localDef != null && localDef.getDeploymentId() != null) {
                processDefinitionId = localDef.getDeploymentId();
            }
        }
        
        org.flowable.engine.repository.ProcessDefinition flowDef = 
            repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
            
        if (flowDef != null) {
            repositoryService.deleteDeployment(flowDef.getDeploymentId(), cascade);
        }
        
        // 删除本地记录
        if (localDef == null) {
             localDef = processDefinitionDao.selectOne(
                new LambdaQueryWrapper<ProcessDefinition>()
                    .eq(ProcessDefinition::getProcessKey, processDefinitionId.split(":")[0])
            );
        }
        if (localDef != null) {
            processDefinitionDao.deleteById(localDef.getId());
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