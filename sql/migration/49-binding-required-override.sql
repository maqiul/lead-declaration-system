-- ============================================================
-- 资料模板绑定表增加 required 字段
-- 允许每条绑定规则单独配置"是否必填"，覆盖模板全局设置
-- NULL = 使用模板默认值，1 = 必填，0 = 选填
-- ============================================================

ALTER TABLE `declaration_material_template_binding`
  ADD COLUMN `required` INT DEFAULT NULL COMMENT '是否必填(NULL=模板默认,1=必填,0=选填)' AFTER `transport_mode_code`;
