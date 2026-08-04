-- 70-backfill-attachment-supplement-id.sql
-- 历史数据回填：补交功能上线前/打标缺失期间，补交期内上传的附件 supplement_id 为 NULL，
-- 导致前端不显示"补交待审核"标签、增量明细面板查不到这些文件。
-- 回填规则：附件创建时间落在同一申报单某个"草稿(-1)/补交中(0)"补交单的时间窗口内
--           （补交期间存量上传被锁定，窗口内新增附件必为补交增量）。
-- 已通过(1)的补交单审核时标记已清除转正、已驳回(2)的增量已清除，均不回填。

-- ---------- 预检：查看将被回填的附件（执行 UPDATE 前可先跑这段核对） ----------
-- SELECT a.id, a.item_id, a.file_name, a.create_time, i.form_id, s.id AS supplement_id, s.status
-- FROM declaration_material_attachment a
-- JOIN declaration_material_item i ON i.id = a.item_id
-- JOIN declaration_material_supplement s
--   ON s.form_id = i.form_id
--  AND s.status IN (-1, 0)
--  AND a.create_time >= s.create_time
--  AND a.create_time <= COALESCE(s.audit_time, NOW())
-- WHERE a.supplement_id IS NULL;

-- ---------- 1. 回填附件补交标记（多条补交单窗口重叠时取最新发起的一笔） ----------
UPDATE declaration_material_attachment a
SET a.supplement_id = (
    SELECT s.id
    FROM declaration_material_item i
    JOIN declaration_material_supplement s
      ON s.form_id = i.form_id
     AND s.status IN (-1, 0)
     AND a.create_time >= s.create_time
     AND a.create_time <= COALESCE(s.audit_time, NOW())
    WHERE i.id = a.item_id
    ORDER BY s.create_time DESC
    LIMIT 1
)
WHERE a.supplement_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM declaration_material_item i2
    JOIN declaration_material_supplement s2
      ON s2.form_id = i2.form_id
     AND s2.status IN (-1, 0)
     AND a.create_time >= s2.create_time
     AND a.create_time <= COALESCE(s2.audit_time, NOW())
    WHERE i2.id = a.item_id
  );

-- ---------- 2. 回填资料项补交标记（补交期内新增的自定义资料项，同样规则） ----------
UPDATE declaration_material_item i
SET i.supplement_id = (
    SELECT s.id
    FROM declaration_material_supplement s
    WHERE s.form_id = i.form_id
      AND s.status IN (-1, 0)
      AND i.create_time >= s.create_time
      AND i.create_time <= COALESCE(s.audit_time, NOW())
    ORDER BY s.create_time DESC
    LIMIT 1
)
WHERE i.supplement_id IS NULL
  AND i.template_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM declaration_material_supplement s2
    WHERE s2.form_id = i.form_id
      AND s2.status IN (-1, 0)
      AND i.create_time >= s2.create_time
      AND i.create_time <= COALESCE(s2.audit_time, NOW())
  );

-- ---------- 验证 ----------
-- SELECT COUNT(*) FROM declaration_material_attachment WHERE supplement_id IS NOT NULL;
-- SELECT id, form_id, status, create_time, audit_time FROM declaration_material_supplement;
