-- ============================================================
-- 批量回填老申报单的 entity_id
-- 逻辑：根据 declaration_form.shipper_company 匹配 entity_config.entity_name
-- ============================================================

-- 先预览匹配结果（DRY RUN，不修改数据）
SELECT 
  df.id,
  df.form_no,
  df.shipper_company,
  ec.id AS matched_entity_id,
  ec.entity_name,
  ec.entity_name_cn
FROM declaration_form df
LEFT JOIN entity_config ec ON TRIM(df.shipper_company) = TRIM(ec.entity_name)
WHERE df.entity_id IS NULL
  AND df.del_flag = 0
ORDER BY df.id DESC
LIMIT 50;

-- 确认后执行批量更新
UPDATE declaration_form df
INNER JOIN entity_config ec ON TRIM(df.shipper_company) = TRIM(ec.entity_name)
SET df.entity_id = ec.id
WHERE df.entity_id IS NULL
  AND df.del_flag = 0;

-- 查看未匹配的记录（shipper_company 不在主体列表中的）
SELECT 
  df.id,
  df.form_no,
  df.shipper_company
FROM declaration_form df
LEFT JOIN entity_config ec ON TRIM(df.shipper_company) = TRIM(ec.entity_name)
WHERE df.entity_id IS NULL
  AND df.del_flag = 0
  AND ec.id IS NULL
ORDER BY df.id DESC;

-- 如有未匹配的，可手动指定主体ID（将下面的 ? 替换为实际的 entity_id）
-- UPDATE declaration_form SET entity_id = ? WHERE entity_id IS NULL AND del_flag = 0;
