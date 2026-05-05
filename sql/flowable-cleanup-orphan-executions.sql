-- ============================================================
-- Flowable 运行时脏数据诊断与救援
-- 场景：删除/重部署 act_re_procdef 时被 act_ru_execution 外键阻挡
-- 仅用于排查与开发/测试环境清理；生产建议走 Flowable API 级联删除
-- ============================================================

-- ---------- 1. 查：哪些流程定义 / 部署还存在？ ----------
SELECT p.ID_ AS procdef_id, p.KEY_, p.VERSION_, p.DEPLOYMENT_ID_,
       d.NAME_ AS deployment_name, d.DEPLOY_TIME_
FROM ACT_RE_PROCDEF p
LEFT JOIN ACT_RE_DEPLOYMENT d ON d.ID_ = p.DEPLOYMENT_ID_
WHERE p.KEY_ = 'declarationProcess'
ORDER BY d.DEPLOY_TIME_ DESC;

-- ---------- 2. 查：哪些活跃流程实例（未结束）在引用这些 procdef？ ----------
SELECT e.PROC_INST_ID_, e.BUSINESS_KEY_, e.PROC_DEF_ID_, e.ACT_ID_, e.START_TIME_
FROM ACT_RU_EXECUTION e
WHERE e.PARENT_ID_ IS NULL
  AND e.PROC_DEF_ID_ IN (SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_ = 'declarationProcess')
ORDER BY e.START_TIME_ DESC;

-- ---------- 3. 关联业务：对应哪些申报单？ ----------
SELECT e.PROC_INST_ID_, e.BUSINESS_KEY_, f.form_no, f.status, f.create_time
FROM ACT_RU_EXECUTION e
LEFT JOIN declaration_form f ON f.id = CAST(e.BUSINESS_KEY_ AS UNSIGNED)
WHERE e.PARENT_ID_ IS NULL
  AND e.PROC_DEF_ID_ IN (SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_ = 'declarationProcess')
ORDER BY e.START_TIME_ DESC;


-- ============================================================
-- 救援方案（二选一）
-- ============================================================

-- ========== 方案 A（推荐）：Flowable API 级联删除 ==========
-- 不要用 SQL，改在后端执行（或写个临时 controller 调用）：
--   repositoryService.deleteDeployment(deploymentId, true);  -- true = 级联清理 runtime/history
-- 或按流程实例维度：
--   runtimeService.deleteProcessInstance(processInstanceId, "手工清理脏数据");

-- ========== 方案 B：直接 SQL 清理（仅限开发/测试环境） ==========
-- 严格按顺序执行，且只清"declarationProcess"相关数据，避免误伤其它流程
-- 执行前务必备份 act_ru_* / act_hi_* 表！

-- 1) 用变量锁定目标流程定义集合
SET @procdef_ids := (
  SELECT GROUP_CONCAT(ID_) FROM ACT_RE_PROCDEF WHERE KEY_ = 'declarationProcess'
);

-- 2) 清 RU（运行时）—— 必须从子表到主表
DELETE FROM ACT_RU_IDENTITYLINK
WHERE PROC_INST_ID_ IN (SELECT ID_ FROM (
  SELECT ID_ FROM ACT_RU_EXECUTION
  WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids)
) t);

DELETE FROM ACT_RU_VARIABLE
WHERE PROC_INST_ID_ IN (SELECT ID_ FROM (
  SELECT ID_ FROM ACT_RU_EXECUTION
  WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids)
) t);

DELETE FROM ACT_RU_TASK
WHERE PROC_DEF_ID_ IN (SELECT ID_ FROM (
  SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_ = 'declarationProcess'
) t);

DELETE FROM ACT_RU_EVENT_SUBSCR
WHERE PROC_DEF_ID_ IN (SELECT ID_ FROM (
  SELECT ID_ FROM ACT_RE_PROCDEF WHERE KEY_ = 'declarationProcess'
) t);

DELETE FROM ACT_RU_EXECUTION
WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids);

-- 3) 清 HI（历史，按需，一般保留）
-- DELETE FROM ACT_HI_TASKINST       WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids);
-- DELETE FROM ACT_HI_ACTINST        WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids);
-- DELETE FROM ACT_HI_VARINST        WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM ACT_HI_PROCINST WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids));
-- DELETE FROM ACT_HI_IDENTITYLINK   WHERE PROC_INST_ID_ IN (SELECT PROC_INST_ID_ FROM ACT_HI_PROCINST WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids));
-- DELETE FROM ACT_HI_PROCINST       WHERE FIND_IN_SET(PROC_DEF_ID_, @procdef_ids);

-- 4) 之后再执行原本触发报错的 delete/redeploy 操作即可成功
