-- =============================================
-- 新增"补充资料"和"开票金额"两个独立菜单
-- 执行日期: 2026-05-11
-- 说明: 将补充资料(4,5)和开票金额(6,7)拆分为两个独立菜单页
-- =============================================

-- 1. 新增"补充资料"子菜单（parent_id=200 即"出口申报"）
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`, `create_time`, `update_time`, `create_by`, `update_by`)
VALUES
(209, '补充资料', 'declaration-supplement', 200, 2, 'supplement', '@/views/declaration/supplement/index.vue', NULL, 'FileAddOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `is_show` = VALUES(`is_show`),
  `update_time` = NOW();

-- 2. 新增"开票金额"子菜单
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`, `create_time`, `update_time`, `create_by`, `update_by`)
VALUES
(210, '开票金额', 'declaration-invoice-amount', 200, 2, 'invoice-amount', '@/views/declaration/invoice-amount/index.vue', NULL, 'AccountBookOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW(), NULL, NULL)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `is_show` = VALUES(`is_show`),
  `update_time` = NOW();

-- 3. 调整"发票提交"排序为5（原为3）
UPDATE `sys_menu` SET `sort` = 5, `update_time` = NOW() WHERE `id` = 207;

-- 4. 调整"归档查询"排序为6（原为4）
UPDATE `sys_menu` SET `sort` = 6, `update_time` = NOW() WHERE `id` = 208;

-- 5. 将新菜单权限分配给管理员角色（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 209), (1, 210);

-- 6. 如果存在其他角色拥有旧菜单(201)的权限，也赋予新菜单权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 209 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 210 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
