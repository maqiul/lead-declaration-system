-- ============================================================
-- 水单管理菜单拆分：草稿水单 / 待审核 / 已审核 / 未关联
-- ============================================================

-- 1. 更新原"水单列表"(601)为"草稿水单"
UPDATE `sys_menu`
SET `menu_name`  = '草稿水单',
    `menu_code`  = 'remittance-draft',
    `path`       = 'draft',
    `icon`       = 'EditOutlined',
    `sort`       = 1
WHERE `id` = 601;

-- 2. 新增"待审核"菜单(603)
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (603, '待审核', 'remittance-pending', 600, 2, 'pending',
   '@/views/remittance/list/index.vue', NULL, 'ClockCircleOutlined', 2, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 3. 新增"已审核"菜单(604)
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (604, '已审核', 'remittance-audited', 600, 2, 'audited',
   '@/views/remittance/list/index.vue', NULL, 'CheckCircleOutlined', 3, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 4. 新增"未关联"菜单(605)
INSERT INTO `sys_menu`
  (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
   `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
  (605, '未关联', 'remittance-unrelated', 600, 2, 'unrelated',
   '@/views/remittance/list/index.vue', NULL, 'LinkOutlined', 4, 0, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_code` = VALUES(`menu_code`),
  `path` = VALUES(`path`), `component` = VALUES(`component`),
  `status` = 1, `deleted` = 0;

-- 5. 调整"水单审核"(602)排序
UPDATE `sys_menu` SET `sort` = 5 WHERE `id` = 602;

-- 6. 将新菜单分配给超级管理员（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 603), (1, 604), (1, 605);

-- 7. 验证
SELECT id, menu_name, menu_code, path, icon, sort, parent_id
FROM `sys_menu`
WHERE `parent_id` = 600
ORDER BY sort;
