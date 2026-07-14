package com.declaration.flowable;

import com.declaration.entity.FlowTemplate;
import com.declaration.service.FlowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 判断流程定义是否为当前启用的模板版本。
 * 通过 flow_template 表的 code 字段判断，不再硬编码 processKey。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeclarationProcessVersionHelper {

    /** 兜底默认 key，当没有启用模板时使用 */
    public static final String DEFAULT_PROCESS_KEY = "declarationProcess";

    private final RepositoryService repositoryService;
    private final FlowTemplateService flowTemplateService;

    /**
     * 获取所有启用的申报流程模板的 processKey（code）集合
     */
    public Set<String> getEnabledProcessKeys() {
        try {
            List<FlowTemplate> templates = flowTemplateService.listByProcessType("declaration");
            return templates.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus() == 1 && t.getCode() != null)
                    .map(FlowTemplate::getCode)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("查询启用的申报流程模板失败: {}", e.getMessage());
            return Set.of(DEFAULT_PROCESS_KEY);
        }
    }

    /**
     * 判断给定 processDefinitionId 对应的流程是否为当前启用模板的最新版本。
     * 必须同时满足：1) processKey 匹配启用模板 code  2) 版本号等于该 key 的最新部署版本
     */
    public boolean isNewVersionDefinition(String processDefinitionId) {
        if (processDefinitionId == null) {
            return false;
        }
        try {
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionId(processDefinitionId)
                    .singleResult();
            if (def == null) {
                return false;
            }
            Set<String> enabledKeys = getEnabledProcessKeys();
            if (!enabledKeys.contains(def.getKey())) {
                return false;
            }
            // key 匹配后，再比较版本号是否为最新
            ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(def.getKey())
                    .latestVersion()
                    .singleResult();
            return latest != null && def.getVersion() == latest.getVersion();
        } catch (Exception e) {
            log.warn("判断流程定义版本失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前最新版本的流程定义（优先取第一个启用模板对应的最新版本）
     */
    public ProcessDefinition getLatestDefinition() {
        Set<String> enabledKeys = getEnabledProcessKeys();
        for (String key : enabledKeys) {
            ProcessDefinition def = repositoryService.createProcessDefinitionQuery()
                    .processDefinitionKey(key)
                    .latestVersion()
                    .singleResult();
            if (def != null) {
                return def;
            }
        }
        // 兜底
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(DEFAULT_PROCESS_KEY)
                .latestVersion()
                .singleResult();
    }

    /**
     * 根据指定的 processKey（模板编码）获取该流程的最新版本定义。
     * 如果该 key 在启用模板中且已部署 BPMN，返回对应最新版本；否则返回 null。
     */
    public ProcessDefinition getLatestDefinition(String processKey) {
        if (processKey == null || processKey.isEmpty()) {
            return null;
        }
        Set<String> enabledKeys = getEnabledProcessKeys();
        if (!enabledKeys.contains(processKey)) {
            log.warn("processKey={} 不在启用模板列表中，无法获取最新定义", processKey);
            return null;
        }
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processKey)
                .latestVersion()
                .singleResult();
    }

    public boolean isLatestDefinitionNewVersion() {
        ProcessDefinition latest = getLatestDefinition();
        return latest != null && isNewVersionDefinition(latest.getId());
    }
}
