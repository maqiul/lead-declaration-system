package com.declaration.service;

import java.util.List;
import java.util.Map;

/**
 * 老申报单 Flowable 流程迁移：将已结束的老 BPMN 实例恢复到新版 declarationProcess 对应节点。
 */
public interface DeclarationFlowMigrationService {

    /**
     * 恢复单条申报单流程
     */
    Map<String, Object> resumeOne(Long formId);

    /**
     * 批量恢复（默认仅处理 status 1~9 且无活跃流程实例的申报单）
     *
     * @param dryRun   true 时只统计不执行
     * @param statuses 限定业务状态，null 或空表示 1~9
     */
    Map<String, Object> resumeBatch(boolean dryRun, List<Integer> statuses);

    /**
     * 是否需迁移到新版流程（老版本实例在跑，或新版实例节点与业务目标不一致）
     */
    boolean needsLegacyMigration(Long formId);

    /**
     * 列表/详情用：流程迁移提示（是否老版本、是否建议点「恢复流程」）
     */
    Map<String, Object> migrationHint(Long formId);
}
