-- ============================================================
-- 补充资料 / 申请开票金额 按钮权限（此前后端与前端已使用，但未注册到 sys_menu）
-- 导致 v-permission 隐藏「提交补充资料」等按钮
-- ============================================================

INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (2037, '补充资料提交', 'declaration-supplement-submit', 201, 3, NULL, NULL, 'business:declaration:supplement:submit', NULL, 27, 0, 0, 1, 1, 0),
  (2038, '补充资料审核', 'declaration-supplement-audit',  201, 3, NULL, NULL, 'business:declaration:audit:supplement',  NULL, 28, 0, 0, 1, 1, 0),
  (2039, '开票金额提交', 'declaration-invoice-amount-submit', 201, 3, NULL, NULL, 'business:declaration:invoice-amount:submit', NULL, 29, 0, 0, 1, 1, 0),
  (2040, '开票金额审核', 'declaration-invoice-amount-audit',  201, 3, NULL, NULL, 'business:declaration:audit:invoice-amount',  NULL, 30, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name`  = VALUES(`menu_name`),
  `menu_code`  = VALUES(`menu_code`),
  `permission` = VALUES(`permission`),
  `status`     = 1,
  `deleted`    = 0;

-- 超级管理员
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 2037), (1, 2038), (1, 2039), (1, 2040);

-- 已有「资料提交/资料审核」权限的角色，同步赋予补充资料权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2037 FROM `sys_role_menu` rm WHERE rm.menu_id IN (2032, 2033, 209);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2038 FROM `sys_role_menu` rm WHERE rm.menu_id IN (2033, 2035, 209);

-- 已有「发票提交/发票审核」权限的角色，同步赋予开票金额权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2039 FROM `sys_role_menu` rm WHERE rm.menu_id IN (2034, 210);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2040 FROM `sys_role_menu` rm WHERE rm.menu_id IN (2035, 210);

-- 普通申报用户：已有资料/补充资料提交权限的角色，同步赋予开票金额提交
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 2039 FROM `sys_role_menu` rm WHERE rm.menu_id IN (2032, 2037, 209);
