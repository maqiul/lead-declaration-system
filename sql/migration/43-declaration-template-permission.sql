-- ============================================================
-- 43-申报模板选择权限 + 模板申报类型字段
-- ============================================================

-- 1. declaration_form 表新增 template_code 字段
ALTER TABLE `declaration_form` ADD COLUMN `template_code` VARCHAR(60) DEFAULT NULL COMMENT '所选流程模板编码' AFTER `declaration_type`;

-- 2. flow_template 表新增 declaration_type 字段（SELF=内部, EXTERNAL=外部）
ALTER TABLE `flow_template` ADD COLUMN `declaration_type` VARCHAR(20) DEFAULT 'EXTERNAL' COMMENT '申报类型: SELF-内部 EXTERNAL-外部' AFTER `process_type`;

-- 3. 流程模板选择权限（单一权限，挂在申报管理 parent_id=201 下）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(2042,'选择流程模板','declaration-template-select', 201,3,NULL,NULL,'business:declaration:template:select', NULL,27,0,0,1,1,0);
