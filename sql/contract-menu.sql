-- 合同管理菜单配置
-- 1. 插入合同管理根菜单（目录类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '合同管理', 'contract', 1, '/contract', 'Layout', NULL, 'FileTextOutlined', 4, 1);

-- 获取刚插入的根菜单ID
SET @contract_parent_id = (SELECT LAST_INSERT_ID());

-- 2. 插入子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@contract_parent_id, '模板管理', 'contract:template', 2, 'template', 'contract/template/index', 'contract:template:list', 'FileAddOutlined', 1, 1);
SET @template_menu_id = (SELECT LAST_INSERT_ID());

INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@contract_parent_id, '合同列表', 'contract:generation', 2, 'generation', 'contract/generation/index', 'contract:generation:list', 'HistoryOutlined', 2, 1);
SET @generation_menu_id = (SELECT LAST_INSERT_ID());

-- 3. 插入模板管理操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@template_menu_id, '模板查询', 'contract:template:query', 3, NULL, NULL, 'contract:template:list', NULL, 1, 1),
(@template_menu_id, '模板新增', 'contract:template:add', 3, NULL, NULL, 'contract:template:add', NULL, 2, 1),
(@template_menu_id, '模板修改', 'contract:template:update', 3, NULL, NULL, 'contract:template:edit', NULL, 3, 1),
(@template_menu_id, '模板删除', 'contract:template:delete', 3, NULL, NULL, 'contract:template:delete', NULL, 4, 1),
(@template_menu_id, '模板上传', 'contract:template:upload', 3, NULL, NULL, 'contract:template:upload', NULL, 5, 1);

-- 4. 插入合同列表操作权限（按钮类型）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@generation_menu_id, '合同查询', 'contract:generation:query', 3, NULL, NULL, 'contract:generation:list', NULL, 1, 1),
(@generation_menu_id, '合同下载', 'contract:generation:download', 3, NULL, NULL, 'contract:download', NULL, 2, 1),
(@generation_menu_id, '合同删除', 'contract:generation:delete', 3, NULL, NULL, 'contract:generation:delete', NULL, 3, 1);

-- 5. 为超级管理员分配权限 (role_id = 1)
-- 这一步推荐在所有菜单插入完成后执行
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code LIKE 'contract%'
ON DUPLICATE KEY UPDATE create_time = NOW();
