-- ============================================================
-- 71: 清理历史脏补交标记
-- 规则：资料项/附件的 supplement_id 只允许指向"当前补交单"
--       （该申报单在途补交单中：优先补交中status=0，其次最新草稿status=-1），
--       其余一律视为历史脏标记置 NULL：
--       a) 指向已终结（通过1/驳回2）或已删除的补交单
--       b) 指向废弃的在途补交单（同一申报单存在多张在途补交单时只保留当前一张）
-- 注意：执行前先用下方预检 SELECT 核对影响行数。
-- ============================================================

-- 预检0：查看该申报单的全部补交单（确认是否存在废弃的草稿/补交中记录）
-- SELECT id, form_id, status, reason, create_time, audit_time
-- FROM declaration_material_supplement ORDER BY form_id, create_time;

-- 预检a：指向已终结/已删除补交单的附件标记
-- SELECT a.id, a.item_id, a.file_name, a.supplement_id, s.status AS supp_status
-- FROM declaration_material_attachment a
-- LEFT JOIN declaration_material_supplement s ON s.id = a.supplement_id
-- WHERE a.supplement_id IS NOT NULL
--   AND (s.id IS NULL OR s.status NOT IN (-1, 0));

-- 预检b：指向"非当前"在途补交单的附件标记（废弃补交单的残留）
-- SELECT a.id, a.item_id, a.file_name, a.supplement_id, s.form_id, s.status, s.create_time
-- FROM declaration_material_attachment a
-- JOIN declaration_material_supplement s ON s.id = a.supplement_id AND s.status IN (-1, 0)
-- WHERE s.id <> (
--     SELECT s2.id FROM declaration_material_supplement s2
--     WHERE s2.form_id = s.form_id AND s2.status IN (-1, 0)
--     ORDER BY s2.status DESC, s2.create_time DESC LIMIT 1
-- );

-- 清理a：附件指向已终结/已删除补交单的标记
UPDATE declaration_material_attachment a
LEFT JOIN declaration_material_supplement s ON s.id = a.supplement_id
SET a.supplement_id = NULL
WHERE a.supplement_id IS NOT NULL
  AND (s.id IS NULL OR s.status NOT IN (-1, 0));

-- 清理b：附件指向废弃在途补交单的标记（同一申报单只保留当前补交单）
UPDATE declaration_material_attachment a
JOIN declaration_material_supplement s ON s.id = a.supplement_id AND s.status IN (-1, 0)
SET a.supplement_id = NULL
WHERE s.id <> (
    SELECT cur.id FROM (
        SELECT s2.id, s2.form_id
        FROM declaration_material_supplement s2
        WHERE s2.status IN (-1, 0)
          AND s2.id = (
              SELECT s3.id FROM declaration_material_supplement s3
              WHERE s3.form_id = s2.form_id AND s3.status IN (-1, 0)
              ORDER BY s3.status DESC, s3.create_time DESC LIMIT 1
          )
    ) cur
    WHERE cur.form_id = s.form_id
);

-- 清理a：资料项指向已终结/已删除补交单的标记
UPDATE declaration_material_item i
LEFT JOIN declaration_material_supplement s ON s.id = i.supplement_id
SET i.supplement_id = NULL
WHERE i.supplement_id IS NOT NULL
  AND (s.id IS NULL OR s.status NOT IN (-1, 0));

-- 清理b：资料项指向废弃在途补交单的标记
UPDATE declaration_material_item i
JOIN declaration_material_supplement s ON s.id = i.supplement_id AND s.status IN (-1, 0)
SET i.supplement_id = NULL
WHERE s.id <> (
    SELECT cur.id FROM (
        SELECT s2.id, s2.form_id
        FROM declaration_material_supplement s2
        WHERE s2.status IN (-1, 0)
          AND s2.id = (
              SELECT s3.id FROM declaration_material_supplement s3
              WHERE s3.form_id = s2.form_id AND s3.status IN (-1, 0)
              ORDER BY s3.status DESC, s3.create_time DESC LIMIT 1
          )
    ) cur
    WHERE cur.form_id = s.form_id
);
