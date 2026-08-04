package com.declaration.service.impl;

import com.declaration.entity.FlowNode;
import com.declaration.entity.FlowTemplate;
import com.declaration.entity.FlowTemplateNode;
import com.declaration.entity.ProcessDefinition;
import com.declaration.service.BpmnGeneratorService;
import com.declaration.service.FlowTemplateService;
import com.declaration.service.ProcessDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * BPMN XML 自动生成服务实现
 *
 * @author Administrator
 * @since 2026-06-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BpmnGeneratorServiceImpl implements BpmnGeneratorService {

    private final FlowTemplateService flowTemplateService;
    private final ProcessDefinitionService processDefinitionService;

    @Override
    public String generateBpmnXml(Long templateId) {
        FlowTemplate template = flowTemplateService.getTemplateWithSteps(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在: " + templateId);
        }

        List<FlowTemplateNode> templateNodes = template.getTemplateNodes();
        if (templateNodes == null || templateNodes.isEmpty()) {
            throw new RuntimeException("模板无节点编排: " + templateId);
        }

        // 按编排顺序收集所有已启用节点（保持 sortOrder 顺序）
        List<FlowTemplateNode> enabledNodes = new ArrayList<>();
        for (FlowTemplateNode tn : templateNodes) {
            if (tn.getEnabled() != null && tn.getEnabled() == 1 && tn.getNode() != null) {
                String nodeType = tn.getNode().getNodeType();
                if ("userTask".equals(nodeType) || "serviceTask".equals(nodeType)) {
                    enabledNodes.add(tn);
                }
            }
        }

        boolean hasUserTask = enabledNodes.stream().anyMatch(tn -> "userTask".equals(tn.getNode().getNodeType()));
        if (!hasUserTask) {
            throw new RuntimeException("模板无启用的业务节点");
        }

        return buildBpmnXml(template, enabledNodes);
    }

    @Override
    public String generateAndDeploy(Long templateId) {
        String bpmnXml = generateBpmnXml(templateId);
        FlowTemplate template = flowTemplateService.getById(templateId);

        String processKey = template.getCode();
        String processType = template.getProcessType() != null ? template.getProcessType() : "declaration";

        ProcessDefinition pd = new ProcessDefinition();
        pd.setProcessKey(processKey);
        pd.setProcessName(getProcessNamePrefix(processType) + template.getName());
        pd.setCategory(processType);
        pd.setDescription("由模板[" + template.getCode() + "]自动生成");

        processDefinitionService.deployProcess(bpmnXml, pd);
        log.info("模板 {} BPMN 已生成并部署, processKey={}, category={}", templateId, processKey, processType);

        return "deployed";
    }

    /**
     * 构建 BPMN 2.0 XML（含图形坐标数据）
     *
     * @param template      流程模板
     * @param enabledNodes  已启用节点（按编排顺序，混合 serviceTask + userTask）
     */
    private String buildBpmnXml(FlowTemplate template, List<FlowTemplateNode> enabledNodes) {
        // 根据流程类型确定监听器
        String listenerExpr = getListenerExpression(template.getProcessType());
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        xml.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        xml.append("  xmlns:flowable=\"http://flowable.org/bpmn\"\n");
        xml.append("  xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\"\n");
        xml.append("  xmlns:omgdc=\"http://www.omg.org/spec/DD/20100524/DC\"\n");
        xml.append("  xmlns:omgdi=\"http://www.omg.org/spec/DD/20100524/DI\"\n");
        xml.append("  targetNamespace=\"http://www.flowable.org/processdef\">\n\n");

        String processKey = template.getCode();
        xml.append("  <process id=\"").append(processKey).append("\" name=\"").append(getProcessNamePrefix(template.getProcessType()))
           .append(escapeXml(template.getName()))
           .append("\" isExecutable=\"true\">\n\n");

        // 图形坐标记录: id -> [x, y, w, h]
        List<String[]> diagramShapes = new ArrayList<>();
        // 连线坐标记录: flowId -> [[x1,y1],[x2,y2],...]
        List<String[]> diagramEdges = new ArrayList<>();

        // 布局参数
        int mainY = 200;          // 主流程 Y 坐标
        int rejectY = 340;        // 驳回处理 Y 坐标
        int startX = 80;          // 起始 X
        int hSpacing = 170;       // 水平间距
        int curX = startX;

        // ---- Flow elements ----
        List<String> sequenceFlows = new ArrayList<>();

        // 1. StartEvent
        xml.append("    <startEvent id=\"startEvent\" name=\"开始\" />\n\n");
        diagramShapes.add(new String[]{"startEvent", String.valueOf(curX), String.valueOf(mainY), "36", "36"});
        curX += hSpacing;

        // 2. 按编排顺序逐个处理节点（serviceTask 和 userTask 混合）
        String lastSubmitId = "startEvent";
        int gatewayCount = 0;
        List<Integer> gatewayXPositions = new ArrayList<>();
        // 记录需要 rejectHandler 的网关信息: [gwIndex, gwX]
        List<int[]> rejectHandlerInfos = new ArrayList<>();

        for (int i = 0; i < enabledNodes.size(); i++) {
            FlowTemplateNode tn = enabledNodes.get(i);
            FlowNode node = tn.getNode();
            String nodeType = node.getNodeType();

            // 模板级覆盖：优先使用 flow_template_node 的 assignee / candidateGroups
            String effectiveAssignee = (tn.getAssignee() != null && !tn.getAssignee().isEmpty())
                    ? tn.getAssignee() : node.getAssignee();
            String effectiveCandidateGroups = (tn.getCandidateGroups() != null && !tn.getCandidateGroups().isEmpty())
                    ? tn.getCandidateGroups() : node.getCandidateGroups();

            // rejectHandler 是驳回路径专用节点，不作为普通 serviceTask 参与主流程
            if ("rejectHandler".equals(node.getNodeKey())) continue;

            if ("serviceTask".equals(nodeType)) {
                // ---- serviceTask: 直接输出，不产生网关 ----
                String svcKey = node.getNodeKey();
                String delegateExpr = node.getDelegateExpression();
                if (delegateExpr == null || delegateExpr.isEmpty()) {
                    log.warn("serviceTask 节点 {} 未配置 delegateExpression，跳过", svcKey);
                    continue;
                }
                xml.append("    <serviceTask id=\"").append(svcKey)
                   .append("\" name=\"").append(escapeXml(node.getNodeName())).append("\"\n");
                xml.append("      flowable:delegateExpression=\"").append(delegateExpr).append("\">\n");
                xml.append("      <extensionElements>\n");
                xml.append("        <flowable:executionListener event=\"end\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
                xml.append("      </extensionElements>\n");
                xml.append("    </serviceTask>\n\n");

                // 连线：如果上一个节点是网关，带 approved=true 条件
                if (lastSubmitId.startsWith("gw_")) {
                    sequenceFlows.add(lastSubmitId + " -> " + svcKey + " [approved==true]");
                } else {
                    sequenceFlows.add(lastSubmitId + " -> " + svcKey);
                }
                diagramShapes.add(new String[]{svcKey, String.valueOf(curX), String.valueOf(mainY - 17), "110", "70"});
                curX += hSpacing;
                lastSubmitId = svcKey;

            } else if ("userTask".equals(nodeType)) {
                // ---- userTask: 分提交节点和审核节点 ----
                boolean isAudit = isAuditNode(effectiveCandidateGroups, node.getNodeKey());
                String nodeId = node.getNodeKey();

                appendUserTask(xml, node, effectiveAssignee, effectiveCandidateGroups, listenerExpr);

                if (lastSubmitId.startsWith("gw_")) {
                    sequenceFlows.add(lastSubmitId + " -> " + nodeId + " [approved==true]");
                } else {
                    sequenceFlows.add(lastSubmitId + " -> " + nodeId);
                }
                diagramShapes.add(new String[]{nodeId, String.valueOf(curX), String.valueOf(mainY - 17), "110", "70"});
                curX += hSpacing;

                if (!isAudit) {
                    // 提交节点：直接记录为上一个提交ID
                    lastSubmitId = nodeId;
                } else {
                    // 审核节点：生成网关 + 驳回处理
                    int gwIndex = gatewayCount;
                    String gwId = "gw_" + gwIndex;
                    gatewayCount++;
                    xml.append("    <exclusiveGateway id=\"").append(gwId)
                       .append("\" name=\"审批结果\" />\n\n");
                    sequenceFlows.add(nodeId + " -> " + gwId);
                    diagramShapes.add(new String[]{gwId, String.valueOf(curX), String.valueOf(mainY), "40", "40"});
                    gatewayXPositions.add(curX);
                    curX += hSpacing;

                    lastSubmitId = gwId;

                    // 如果是最后一个审核节点，网关直接连到结束事件
                    boolean hasNextUserTask = false;
                    for (int j = i + 1; j < enabledNodes.size(); j++) {
                        if ("userTask".equals(enabledNodes.get(j).getNode().getNodeType())) {
                            hasNextUserTask = true;
                            break;
                        }
                    }
                    if (!hasNextUserTask) {
                        sequenceFlows.add(gwId + " -> endEvent [approved==true]");
                    }

                    // 驳回处理：检查节点是否配置了 rejectToEnd
                    boolean nodeRejectToEnd = node.getRejectToEnd() != null && node.getRejectToEnd() == 1;

                    if (nodeRejectToEnd) {
                        // 驳回直接结束流程
                        sequenceFlows.add(gwId + " -> endEvent [approved==false]");
                    } else {
                        // 驳回处理：找前置提交节点，找不到则回到当前审核节点自身（自循环）
                        String prevSubmit = findPreviousSubmitId(enabledNodes, i);
                        String rejectTarget = (prevSubmit != null) ? prevSubmit : nodeId;

                        // 检查当前审核节点与前一个提交节点之间是否配置了 rejectHandler
                        boolean thisAuditHasRejectHandler = hasRejectHandlerBetween(enabledNodes, i);

                        if (thisAuditHasRejectHandler) {
                            // 配置了 rejectHandler：网关 → rejectHandler → 目标节点
                            String rejectId = "rejectHandler_" + gwIndex;
                            sequenceFlows.add(gwId + " -> " + rejectId + " [approved==false]");
                            sequenceFlows.add(rejectId + " -> " + rejectTarget);
                            rejectHandlerInfos.add(new int[]{gwIndex, curX - hSpacing});
                        } else {
                            // 未配置 rejectHandler：网关驳回直接回到上一个提交节点
                            sequenceFlows.add(gwId + " -> " + rejectTarget + " [approved==false]");
                        }
                    }
                }
            }
        }

        // 3. 仅为需要驳回循环的网关生成 rejectHandler
        for (int r = 0; r < rejectHandlerInfos.size(); r++) {
            int gwIndex = rejectHandlerInfos.get(r)[0];
            int gwX = rejectHandlerInfos.get(r)[1];
            String rejectId = "rejectHandler_" + gwIndex;
            xml.append("    <userTask id=\"").append(rejectId)
               .append("\" name=\"驳回修改\" flowable:assignee=\"${starterId}\">\n");
            xml.append("      <extensionElements>\n");
            xml.append("        <flowable:taskListener event=\"create\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
            xml.append("        <flowable:taskListener event=\"complete\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
            xml.append("      </extensionElements>\n");
            xml.append("    </userTask>\n\n");

            int rejectX = gwX - 35;
            diagramShapes.add(new String[]{rejectId, String.valueOf(rejectX), String.valueOf(rejectY), "110", "70"});
        }

        // 4. EndEvent
        xml.append("    <endEvent id=\"endEvent\" name=\"结束\">\n");
        xml.append("      <extensionElements>\n");
        xml.append("        <flowable:executionListener event=\"end\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
        xml.append("      </extensionElements>\n");
        xml.append("    </endEvent>\n\n");
        diagramShapes.add(new String[]{"endEvent", String.valueOf(curX), String.valueOf(mainY), "36", "36"});
        curX += hSpacing;

        // 5. Sequence flows
        for (String flow : sequenceFlows) {
            String[] parts;
            String condition = null;

            if (flow.contains("[")) {
                String condPart = flow.substring(flow.indexOf('[') + 1, flow.indexOf(']'));
                condition = condPart;
                flow = flow.substring(0, flow.indexOf('[')).trim();
            }

            parts = flow.split(" -> ");
            String source = parts[0].trim();
            String target = parts[1].trim();
            // 当有条件时加入后缀避免同一 source→target 多条线 ID 冲突
            String condSuffix = "";
            if (condition != null) {
                condSuffix = condition.contains("true") ? "_pass" : "_reject";
            }
            String flowId = "flow_" + source + "_" + target + condSuffix;

            xml.append("    <sequenceFlow id=\"").append(flowId)
               .append("\" sourceRef=\"").append(source)
               .append("\" targetRef=\"").append(target).append("\"");

            if (condition != null) {
                xml.append(">\n");
                xml.append("      <conditionExpression xsi:type=\"tFormalExpression\">")
                   .append("${").append(condition).append("}")
                   .append("</conditionExpression>\n");
                xml.append("    </sequenceFlow>\n");
            } else {
                xml.append(" />\n");
            }

            diagramEdges.add(new String[]{flowId, source, target});
        }

        xml.append("\n  </process>\n\n");

        // 6. BPMNDiagram 图形数据
        appendBpmnDiagram(xml, processKey, diagramShapes, diagramEdges, mainY, rejectY);

        xml.append("</definitions>\n");
        return xml.toString();
    }

    /**
     * 生成 BPMNDiagram 图形坐标数据
     */
    private void appendBpmnDiagram(StringBuilder xml, String processKey, List<String[]> shapes, List<String[]> edges,
                                   int mainY, int rejectY) {
        xml.append("  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n");
        xml.append("    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"").append(processKey).append("\">\n\n");

        // 输出所有节点图形
        for (String[] shape : shapes) {
            String id = shape[0];
            int x = Integer.parseInt(shape[1]);
            int y = Integer.parseInt(shape[2]);
            int w = Integer.parseInt(shape[3]);
            int h = Integer.parseInt(shape[4]);

            xml.append("      <bpmndi:BPMNShape id=\"BPMNShape_").append(id)
               .append("\" bpmnElement=\"").append(id).append("\"");

            if (id.startsWith("gw_")) {
                xml.append(" isMarkerVisible=\"true\"");
            }
            xml.append(">\n");
            xml.append("        <omgdc:Bounds x=\"").append(x)
               .append("\" y=\"").append(y)
               .append("\" width=\"").append(w)
               .append("\" height=\"").append(h).append("\" />\n");
            xml.append("      </bpmndi:BPMNShape>\n");
        }

        xml.append("\n");

        // 输出所有连线
        for (String[] edge : edges) {
            String flowId = edge[0];
            String source = edge[1];
            String target = edge[2];

            String[] srcShape = findShape(shapes, source);
            String[] tgtShape = findShape(shapes, target);
            if (srcShape == null || tgtShape == null) continue;

            int srcX = Integer.parseInt(srcShape[1]);
            int srcY = Integer.parseInt(srcShape[2]);
            int srcW = Integer.parseInt(srcShape[3]);
            int srcH = Integer.parseInt(srcShape[4]);
            int tgtX = Integer.parseInt(tgtShape[1]);
            int tgtY = Integer.parseInt(tgtShape[2]);
            int tgtW = Integer.parseInt(tgtShape[3]);
            int tgtH = Integer.parseInt(tgtShape[4]);

            xml.append("      <bpmndi:BPMNEdge id=\"BPMNEdge_").append(flowId)
               .append("\" bpmnElement=\"").append(flowId).append("\">\n");

            if (target.startsWith("rejectHandler")) {
                // 网关 → 驳回修改：从网关底部直下到驳回修改顶部
                int gwCenterX = srcX + srcW / 2;

                xml.append("        <omgdi:waypoint x=\"").append(gwCenterX)
                   .append("\" y=\"").append(srcY + srcH).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(gwCenterX)
                   .append("\" y=\"").append(tgtY).append("\" />\n");

            } else if (source.startsWith("rejectHandler")) {
                // 驳回修改 → 回到提交节点
                int returnY = rejectY + tgtH + 30;

                xml.append("        <omgdi:waypoint x=\"").append(srcX)
                   .append("\" y=\"").append(srcY + srcH).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(srcX)
                   .append("\" y=\"").append(returnY).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(tgtX + tgtW / 2)
                   .append("\" y=\"").append(returnY).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(tgtX + tgtW / 2)
                   .append("\" y=\"").append(tgtY + tgtH).append("\" />\n");

            } else if (source.startsWith("gw_") && tgtX < srcX) {
                // 网关驳回直接回到左侧提交节点：网关底部下到底部安全区，水平走到目标，上到目标底部
                int gwCenterX = srcX + srcW / 2;
                int returnY = mainY + 120;

                xml.append("        <omgdi:waypoint x=\"").append(gwCenterX)
                   .append("\" y=\"").append(srcY + srcH).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(gwCenterX)
                   .append("\" y=\"").append(returnY).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(tgtX + tgtW / 2)
                   .append("\" y=\"").append(returnY).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(tgtX + tgtW / 2)
                   .append("\" y=\"").append(tgtY + tgtH).append("\" />\n");

            } else {
                // 普通水平连线：源右侧中心 → 目标左侧中心
                int x1 = srcX + srcW;
                int y1 = srcY + srcH / 2;
                int x2 = tgtX;
                int y2 = tgtY + tgtH / 2;

                xml.append("        <omgdi:waypoint x=\"").append(x1)
                   .append("\" y=\"").append(y1).append("\" />\n");
                xml.append("        <omgdi:waypoint x=\"").append(x2)
                   .append("\" y=\"").append(y2).append("\" />\n");
            }

            xml.append("      </bpmndi:BPMNEdge>\n");
        }

        xml.append("\n    </bpmndi:BPMNPlane>\n");
        xml.append("  </bpmndi:BPMNDiagram>\n");
    }

    /**
     * 查找图形坐标
     */
    private String[] findShape(List<String[]> shapes, String id) {
        for (String[] s : shapes) {
            if (id.equals(s[0])) return s;
        }
        return null;
    }

    /**
     * 生成 userTask XML 片段（使用覆盖后的 assignee / candidateGroups）
     */
    private void appendUserTask(StringBuilder xml, FlowNode node, String assignee, String candidateGroups, String listenerExpr) {
        xml.append("    <userTask id=\"").append(node.getNodeKey())
           .append("\" name=\"").append(escapeXml(node.getNodeName())).append("\"");

        // Assignee（优先使用模板级覆盖）
        if (assignee != null && !assignee.isEmpty()) {
            xml.append("\n      flowable:assignee=\"").append(assignee).append("\"");
        }

        // Candidate groups（优先使用模板级覆盖）
        if (candidateGroups != null && !candidateGroups.isEmpty()) {
            xml.append("\n      flowable:candidateGroups=\"").append(candidateGroups).append("\"");
        }

        xml.append(">\n");

        // TaskListener
        xml.append("      <extensionElements>\n");
        xml.append("        <flowable:taskListener event=\"create\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
        xml.append("        <flowable:taskListener event=\"complete\" delegateExpression=\"").append(listenerExpr).append("\" />\n");
        xml.append("      </extensionElements>\n");

        xml.append("    </userTask>\n\n");
    }

    /**
     * 判断节点是否为审核节点（需要网关分支）。
     * 仅根据 nodeKey 判断，candidateGroups 不改变节点性质。
     */
    private boolean isAuditNode(String effectiveCandidateGroups, String nodeKey) {
        return nodeKey != null && nodeKey.endsWith("Audit");
    }

    /**
     * 检查当前审核节点与前一个提交节点之间是否配置了 rejectHandler 节点
     */
    private boolean hasRejectHandlerBetween(List<FlowTemplateNode> enabledNodes, int currentAuditIndex) {
        // 向前查找最近的提交节点，检查中间的节点是否包含 rejectHandler
        for (int i = currentAuditIndex - 1; i >= 0; i--) {
            FlowNode node = enabledNodes.get(i).getNode();
            if ("rejectHandler".equals(node.getNodeKey())) {
                return true;
            }
            // 遇到提交节点（非审核、非 serviceTask）则停止
            if ("userTask".equals(node.getNodeType()) && !isAuditNode(null, node.getNodeKey())) {
                return false;
            }
        }
        return false;
    }

    /**
     * 查找当前审核节点之前最近的提交类 userTask 节点（跳过审核节点和 serviceTask）。
     * 找不到则返回 null（如 deptAudit，驳回应直接结束流程）。
     */
    private String findPreviousSubmitId(List<FlowTemplateNode> enabledNodes, int currentAuditIndex) {
        for (int i = currentAuditIndex - 1; i >= 0; i--) {
            FlowTemplateNode prevTn = enabledNodes.get(i);
            FlowNode prev = prevTn.getNode();
            if ("serviceTask".equals(prev.getNodeType())) continue;
            String prevCandidateGroups = (prevTn.getCandidateGroups() != null && !prevTn.getCandidateGroups().isEmpty())
                    ? prevTn.getCandidateGroups() : prev.getCandidateGroups();
            if (isAuditNode(prevCandidateGroups, prev.getNodeKey())) continue;
            return prev.getNodeKey();
        }
        return null;
    }

    /**
     * 根据流程类型返回对应的流程名称前缀
     */
    private String getProcessNamePrefix(String processType) {
        if ("exemption".equals(processType)) {
            return "豁免流程-";
        }
        if ("supplement".equals(processType)) {
            return "补交流程-";
        }
        return "申报流程-";
    }

    /**
     * 根据流程类型返回对应的监听器表达式
     */
    private String getListenerExpression(String processType) {
        if ("exemption".equals(processType)) {
            return "${exemptionTaskListener}";
        }
        if ("supplement".equals(processType)) {
            return "${supplementTaskListener}";
        }
        // 默认使用申报流程监听器
        return "${declarationTaskListener}";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
