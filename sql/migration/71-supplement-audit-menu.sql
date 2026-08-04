-- =============================================
-- 71-supplement-audit-menu.sql
-- 新增「补充资料审核」独立菜单（SELF + EXT 两套）
-- 页面展示待审资料补交列表（申报人提交补交审核后才出现），
-- 点击「去审核」进入详情页补交审核模式查看增量并通过/驳回。
--
-- 依赖：69-supplement-flow.sql（补交流程与 SUPPLEMENT_AUDITOR 角色）
-- 生效前提：执行后需重新登录
-- =============================================

-- 1. 菜单：SELF(900) 与 EXT(901) 各一条页面菜单
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
(922, '补充资料审核', 'declaration-self-supplement-audit', 900, 2, 'supplement-audit',
    '@/views/declaration/supplement-audit/index.vue', NULL, 'AuditOutlined', 11, 0, 0, 1, 1, 0, NOW(), NOW()),
(923, '补充资料审核', 'declaration-ext-supplement-audit', 901, 2, 'supplement-audit',
    '@/views/declaration/supplement-audit/index.vue', NULL, 'AuditOutlined', 11, 0, 0, 1, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `is_show` = VALUES(`is_show`),
  `update_time` = NOW();

-- 2. 超级管理员直接授权
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 922), (1, 923);

-- 3. 拥有「资料审核」权限点的角色同步授予新菜单及顶级目录（否则菜单树不显示）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 922
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON rm.menu_id = m.id
WHERE m.`permission` = 'business:declaration:audit:material' AND m.`menu_type` = 3;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 923
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON rm.menu_id = m.id
WHERE m.`permission` = 'business:declaration:audit:material' AND m.`menu_type` = 3;

-- 顶级目录（SELF 900 / EXT 901）兜底授权，保证菜单层级可见
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 900 FROM `sys_role_menu` rm WHERE rm.menu_id IN (922, 923);
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, 901 FROM `sys_role_menu` rm WHERE rm.menu_id IN (922, 923);
