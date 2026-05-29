package com.declaration.flowable;

import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Component;

/**
 * 判断 declarationProcess 流程定义是否为新版（含补充资料节点）。
 */
@Component
@RequiredArgsConstructor
public class DeclarationProcessVersionHelper {

    public static final String PROCESS_KEY = "declarationProcess";
    /** 新版 BPMN 独有节点 */
    public static final String NEW_VERSION_MARKER = "supplementSubmit";

    private final RepositoryService repositoryService;

    public boolean isNewVersionDefinition(String processDefinitionId) {
        if (processDefinitionId == null) {
            return false;
        }
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if (model == null) {
            return false;
        }
        Process process = model.getMainProcess();
        if (process == null) {
            return false;
        }
        return process.getFlowElement(NEW_VERSION_MARKER) != null;
    }

    public ProcessDefinition getLatestDefinition() {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PROCESS_KEY)
                .latestVersion()
                .singleResult();
    }

    public boolean isLatestDefinitionNewVersion() {
        ProcessDefinition latest = getLatestDefinition();
        return latest != null && isNewVersionDefinition(latest.getId());
    }
}
