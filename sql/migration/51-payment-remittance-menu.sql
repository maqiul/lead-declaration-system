-- ============================================================
-- 51-出款水单管理菜单与权限
-- ============================================================

-- 1. 出款水单管理一级目录（ID: 800）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (800, '出款水单管理', 'payment-remittance', 0, 1, '/payment-remittance', 'Layout',
   NULL, 'SendOutlined', 8, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 2. 草稿出款（ID: 801）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (801, '草稿出款', 'payment-remittance-draft', 800, 2, 'draft',
   '@/views/payment-remittance/list/index.vue', NULL, 'EditOutlined', 1, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 3. 待审核（ID: 802）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (802, '待审核', 'payment-remittance-pending', 800, 2, 'pending',
   '@/views/payment-remittance/list/index.vue', NULL, 'ClockCircleOutlined', 2, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 4. 已审核（ID: 803）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (803, '已审核', 'payment-remittance-audited', 800, 2, 'audited',
   '@/views/payment-remittance/list/index.vue', NULL, 'CheckCircleOutlined', 3, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 5. 未关联（ID: 804）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (804, '未关联', 'payment-remittance-unrelated', 800, 2, 'unrelated',
   '@/views/payment-remittance/list/index.vue', NULL, 'LinkOutlined', 4, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 6. 按钮权限
-- 创建（ID: 810）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (810, '创建出款水单', 'payment-remittance-create', 800, 3, '', NULL,
   'business:payment-remittance:create', NULL, 10, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 更新（ID: 811）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (811, '更新出款水单', 'payment-remittance-update', 800, 3, '', NULL,
   'business:payment-remittance:update', NULL, 11, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 删除（ID: 812）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (812, '删除出款水单', 'payment-remittance-delete', 800, 3, '', NULL,
   'business:payment-remittance:delete', NULL, 12, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 提交审核（ID: 813）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (813, '提交审核', 'payment-remittance-submit', 800, 3, '', NULL,
   'business:payment-remittance:submit', NULL, 13, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 审核（ID: 814）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (814, '审核出款水单', 'payment-remittance-audit', 800, 3, '', NULL,
   'business:payment-remittance:audit', NULL, 14, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 查看（ID: 815）
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (815, '查看出款水单', 'payment-remittance-view', 800, 3, '', NULL,
   'business:payment-remittance:view', NULL, 15, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `permission` = VALUES(`permission`), `status` = 1, `deleted` = 0;

-- 7. 将出款水单菜单分配给超级管理员（role_id=1）和财务审核员（role_id=5）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 800), (1, 801), (1, 802), (1, 803), (1, 804),
  (1, 810), (1, 811), (1, 812), (1, 813), (1, 814), (1, 815),
  (5, 800), (5, 801), (5, 802), (5, 803), (5, 804),
  (5, 810), (5, 811), (5, 812), (5, 813), (5, 814), (5, 815);

-- 8. 验证
SELECT id, menu_name, menu_code, path, icon, sort, parent_id
FROM `sys_menu`
WHERE `parent_id` = 800 OR `id` = 800
ORDER BY sort;
