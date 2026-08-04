-- ============================================================
-- 67-template-required-stages.sql
-- 资料模板必填按环节配置：declaration_material_template 增加 required_stages
-- 计划：R3 模板必填按环节
-- ============================================================

-- 1. 增加 required_stages 字段（逗号分隔环节：BASIC,MATERIAL_SUBMIT,SUPPLEMENT,INVOICE）
ALTER TABLE declaration_material_template
    ADD COLUMN required_stages VARCHAR(100) NULL COMMENT '必填环节（逗号分隔，命中当前环节才必填；为空回退 required 字段）' AFTER required;

-- 2. 数据回填：required=1 的模板保持原有环节必填语义（required_stages = stage）
UPDATE declaration_material_template
SET required_stages = stage
WHERE required = 1
  AND (required_stages IS NULL OR required_stages = '');

-- required=0 的模板保持 required_stages 为 NULL，resolveRequired 回退 required=0，行为不变
