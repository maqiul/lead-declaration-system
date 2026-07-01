-- ============================================================
-- 修复脚本：内部组织申报流程修复
-- 功能：
--   1. 设置指定组织为内部机构
--   2. 将对应申报单标记为自用申报
--   3. 将卡在开票流程(status 6-9)的自用申报推进到已完成(status 10)
--   4. 清理对应的 Flowable 活跃流程实例
-- 使用方法：将下方 @orgIds 替换为实际的组织ID列表
-- 注意：MySQL 变量不支持 IN (@var) 拆列表，改用 FIND_IN_SET
-- ============================================================

SET @orgIds = '1,2,3';  -- ← 替换为实际的内部组织ID，逗号分隔

-- ============================================================
-- Step 1: 设置指定组织为内部机构
-- ============================================================
UPDATE `sys_org`
  SET `org_type` = 'INTERNAL'
  WHERE FIND_IN_SET(`id`, @orgIds);

-- ============================================================
-- Step 2: 将对应申报单标记为自用申报
-- ============================================================
UPDATE `declaration_form`
  SET `declaration_type` = 'SELF'
  WHERE FIND_IN_SET(`org_id`, @orgIds);

-- ============================================================
-- Step 3: 找出卡在开票流程(status 6~9)的自用申报单
--         这些申报补充资料审核已通过，自用应直接完成
-- ============================================================
SELECT id, form_no, org_id, status, declaration_type
  FROM `declaration_form`
  WHERE FIND_IN_SET(`org_id`, @orgIds)
    AND `status` BETWEEN 6 AND 9
    AND `del_flag` = 0;

-- ============================================================
-- Step 4: 清理 Flowable 活跃流程（必须在更新 status 之前执行）
-- ============================================================

-- 4a. 删除待办任务
DELETE t FROM `ACT_RU_TASK` t
  INNER JOIN `ACT_RU_EXECUTION` e ON t.PROC_INST_ID_ = e.PROC_INST_ID_
  INNER JOIN `declaration_form` df ON e.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0;

-- 4b. 删除身份关联（任务候选人/候选组）
DELETE il FROM `ACT_RU_IDENTITYLINK` il
  INNER JOIN `ACT_RU_EXECUTION` e ON il.PROC_INST_ID_ = e.PROC_INST_ID_
  INNER JOIN `declaration_form` df ON e.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0;

-- 4c. 删除活跃执行实例（含子执行）
DELETE e FROM `ACT_RU_EXECUTION` e
  INNER JOIN `declaration_form` df ON e.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0;

-- 4d. 删除运行时变量
DELETE v FROM `ACT_RU_VARIABLE` v
  INNER JOIN `ACT_HI_PROCINST` hp ON v.PROC_INST_ID_ = hp.PROC_INST_ID_
  INNER JOIN `declaration_form` df ON hp.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0
    AND hp.END_TIME_ IS NULL;

-- ============================================================
-- Step 5: 更新 Flowable 历史记录（标记流程已结束）
-- ============================================================

-- 5a. 标记历史流程实例为已结束
UPDATE `ACT_HI_PROCINST` hp
  INNER JOIN `declaration_form` df ON hp.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  SET hp.END_TIME_ = NOW(),
      hp.DURATION_ = TIMESTAMPDIFF(MILLISECOND, hp.START_TIME_, NOW()),
      hp.DELETE_REASON_ = 'SELF declaration auto-complete'
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0
    AND hp.END_TIME_ IS NULL;

-- 5b. 标记未结束的历史活动为已结束
UPDATE `ACT_HI_ACTINST` ha
  INNER JOIN `ACT_HI_PROCINST` hp ON ha.PROC_INST_ID_ = hp.PROC_INST_ID_
  INNER JOIN `declaration_form` df ON hp.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  SET ha.END_TIME_ = NOW(),
      ha.DURATION_ = TIMESTAMPDIFF(MILLISECOND, ha.START_TIME_, NOW())
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0
    AND ha.END_TIME_ IS NULL;

-- 5c. 标记未结束的历史任务为已结束
UPDATE `ACT_HI_TASKINST` ht
  INNER JOIN `ACT_HI_PROCINST` hp ON ht.PROC_INST_ID_ = hp.PROC_INST_ID_
  INNER JOIN `declaration_form` df ON hp.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  SET ht.END_TIME_ = NOW(),
      ht.DURATION_ = TIMESTAMPDIFF(MILLISECOND, ht.START_TIME_, NOW()),
      ht.DELETE_REASON_ = 'SELF declaration auto-complete'
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.status BETWEEN 6 AND 9
    AND df.del_flag = 0
    AND ht.END_TIME_ IS NULL;

-- ============================================================
-- Step 6: 更新申报单业务状态为已完成(10)
-- ============================================================
UPDATE `declaration_form`
  SET `status` = 10
  WHERE FIND_IN_SET(`org_id`, @orgIds)
    AND `status` BETWEEN 6 AND 9
    AND `del_flag` = 0;

-- ============================================================
-- Step 7: 验证修复结果
-- ============================================================
SELECT '--- 组织验证 ---' AS info;
SELECT id, org_name, org_type FROM `sys_org` WHERE FIND_IN_SET(`id`, @orgIds);

SELECT '--- 申报单验证 ---' AS info;
SELECT id, form_no, org_id, declaration_type, status
  FROM `declaration_form`
  WHERE FIND_IN_SET(`org_id`, @orgIds)
  ORDER BY id DESC;

SELECT '--- 残留活跃流程验证（应为空） ---' AS info;
SELECT e.PROC_INST_ID_, e.BUSINESS_KEY_, e.PROC_DEF_ID_
  FROM `ACT_RU_EXECUTION` e
  INNER JOIN `declaration_form` df ON e.BUSINESS_KEY_ = CAST(df.id AS CHAR)
  WHERE FIND_IN_SET(df.org_id, @orgIds)
    AND df.declaration_type = 'SELF';
