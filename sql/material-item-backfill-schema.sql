-- ============================================================
-- 存量申报资料项 form_schema 回填脚本
-- 场景：方案 C 上线前已创建的申报单，其 declaration_material_item
--       记录是在 form_schema 字段尚不存在/为 NULL 时同步生成的。
--       本脚本从对应模板回填结构化字段定义，使老流程也能正常显示
--       金额/发票号等结构化字段。
-- 前置：先执行 material-item-extend-fields.sql（DDL 加列）
-- 幂等：仅更新 form_schema 为 NULL 或空串的行，可重复执行。
-- ============================================================

-- 1. 按 template_id 关联模板回填
UPDATE declaration_material_item item
INNER JOIN declaration_material_template tpl ON tpl.id = item.template_id
SET item.form_schema = tpl.form_schema
WHERE item.template_id IS NOT NULL
  AND (item.form_schema IS NULL OR item.form_schema = '')
  AND tpl.form_schema IS NOT NULL
  AND tpl.form_schema <> '';

-- 2. 如果历史数据丢失了 template_id（早期手工录入或模板已删除），
--    可按 code 二次匹配兜底。
UPDATE declaration_material_item item
INNER JOIN declaration_material_template tpl ON tpl.code = item.code
SET item.form_schema = tpl.form_schema,
    item.template_id = IFNULL(item.template_id, tpl.id)
WHERE item.code IS NOT NULL
  AND item.code <> ''
  AND (item.form_schema IS NULL OR item.form_schema = '')
  AND tpl.form_schema IS NOT NULL
  AND tpl.form_schema <> '';

-- 3. 查看回填结果（可选）
-- SELECT item.id, item.form_id, item.name, item.code,
--        item.template_id, item.form_schema
--   FROM declaration_material_item item
--  WHERE item.form_schema IS NOT NULL AND item.form_schema <> ''
--  ORDER BY item.form_id DESC
--  LIMIT 50;
