-- 54-declaration-menu-split.sql
-- 将申报菜单拆分为两套独立菜单（物理隔离）
-- 梓熠、理德申报(SELF) 和 集洛申报(EXTERNAL) 各自独立控制子菜单
-- 子菜单路径分别用 /declaration-self/* 和 /declaration-external/*
-- ID 范围：900-921（ID 300 已被税务退费占用）
--
-- 旧菜单角色分配参考（数据库实际状态）：
--   管理员(1): 200,201,202,204,205,206,207,208
--   普通用户(2): 200,201,204,205,206,207,208,209,210
--   部门管理员(3): 200,201,203,205,206,207,208,209,210
--   财务(4): 200,201,202,204,205,206,207,208,209,210
--   业务员(5): 200,201,204,205,206,207,208

-- ============================================================
-- 1. 创建两个新的顶级目录菜单
-- ============================================================
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
(900, '梓熠、理德申报', 'declaration-self', 0, 1, '/declaration-self', 'Layout',
    NULL, 'ShopOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(901, '集洛申报', 'declaration-external', 0, 1, '/declaration-external', 'Layout',
    NULL, 'TeamOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW());

-- ============================================================
-- 2. 梓熠、理德申报(900) 的子菜单
-- ============================================================
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
(902, '申报录入', 'declaration-self-entry', 900, 2, 'entry',
    '@/views/declaration/entry/index.vue', NULL, 'EditOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(903, '资料提交', 'declaration-self-material', 900, 2, 'material',
    '@/views/declaration/material/index.vue', NULL, 'UploadOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(904, '补充资料', 'declaration-self-supplement', 900, 2, 'supplement',
    '@/views/declaration/supplement/index.vue', NULL, 'FileAddOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(905, '开票金额', 'declaration-self-invoice-amount', 900, 2, 'invoice-amount',
    '@/views/declaration/invoice-amount/index.vue', NULL, 'AccountBookOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(906, '发票提交', 'declaration-self-invoice', 900, 2, 'invoice',
    '@/views/declaration/invoice/index.vue', NULL, 'FileTextOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(907, '归档查询', 'declaration-self-archive', 900, 2, 'archive',
    '@/views/declaration/archive/index.vue', NULL, 'FolderOpenOutlined', 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(908, '财务单证', 'declaration-self-finance', 900, 2, 'finance',
    '@/views/declaration/finance/index.vue', NULL, 'PayCircleOutlined', 7, 0, 0, 1, 1, 0, NOW(), NOW()),
(909, '申报管理', 'declaration-self-manage', 900, 2, 'manage',
    '@/views/declaration/manage/index.vue', NULL, 'ContainerOutlined', 8, 0, 0, 1, 1, 0, NOW(), NOW()),
(910, '申报统计', 'declaration-self-statistics', 900, 2, 'statistics',
    '@/views/declaration/statistics/index.vue', NULL, 'BarChartOutlined', 9, 0, 0, 1, 1, 0, NOW(), NOW()),
(911, '申报表单', 'declaration-self-form', 900, 2, 'form',
    '@/views/declaration/form/index.vue', NULL, 'FileTextOutlined', 10, 0, 0, 0, 1, 0, NOW(), NOW());

-- ============================================================
-- 3. 集洛申报(901) 的子菜单
-- ============================================================
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
(912, '申报录入', 'declaration-ext-entry', 901, 2, 'entry',
    '@/views/declaration/entry/index.vue', NULL, 'EditOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(913, '资料提交', 'declaration-ext-material', 901, 2, 'material',
    '@/views/declaration/material/index.vue', NULL, 'UploadOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(914, '补充资料', 'declaration-ext-supplement', 901, 2, 'supplement',
    '@/views/declaration/supplement/index.vue', NULL, 'FileAddOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(915, '开票金额', 'declaration-ext-invoice-amount', 901, 2, 'invoice-amount',
    '@/views/declaration/invoice-amount/index.vue', NULL, 'AccountBookOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(916, '发票提交', 'declaration-ext-invoice', 901, 2, 'invoice',
    '@/views/declaration/invoice/index.vue', NULL, 'FileTextOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(917, '归档查询', 'declaration-ext-archive', 901, 2, 'archive',
    '@/views/declaration/archive/index.vue', NULL, 'FolderOpenOutlined', 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(918, '财务单证', 'declaration-ext-finance', 901, 2, 'finance',
    '@/views/declaration/finance/index.vue', NULL, 'PayCircleOutlined', 7, 0, 0, 1, 1, 0, NOW(), NOW()),
(919, '申报管理', 'declaration-ext-manage', 901, 2, 'manage',
    '@/views/declaration/manage/index.vue', NULL, 'ContainerOutlined', 8, 0, 0, 1, 1, 0, NOW(), NOW()),
(920, '申报统计', 'declaration-ext-statistics', 901, 2, 'statistics',
    '@/views/declaration/statistics/index.vue', NULL, 'BarChartOutlined', 9, 0, 0, 1, 1, 0, NOW(), NOW()),
(921, '申报表单', 'declaration-ext-form', 901, 2, 'form',
    '@/views/declaration/form/index.vue', NULL, 'FileTextOutlined', 10, 0, 0, 0, 1, 0, NOW(), NOW());

-- ============================================================
-- 4. 复制原 201(申报管理) 下的按钮权限到新的申报管理(909 和 919)
--    按钮权限不改变 permission 字符串，只是 parent_id 不同
-- ============================================================

-- 梓熠、理德 - 申报管理(909) 下的按钮权限
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
SELECT m.menu_name, CONCAT(m.menu_code, '-self'), 909, m.menu_type, m.path, m.component,
    m.permission, m.icon, m.sort, m.is_external, m.is_cache, m.is_show, m.status, 0,
    NOW(), NOW()
FROM `sys_menu` m
WHERE m.parent_id = 201 AND m.menu_type = 3 AND m.deleted = 0;

-- 集洛 - 申报管理(919) 下的按钮权限
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
SELECT m.menu_name, CONCAT(m.menu_code, '-ext'), 919, m.menu_type, m.path, m.component,
    m.permission, m.icon, m.sort, m.is_external, m.is_cache, m.is_show, m.status, 0,
    NOW(), NOW()
FROM `sys_menu` m
WHERE m.parent_id = 201 AND m.menu_type = 3 AND m.deleted = 0;

-- 4.1 退回上一步按钮权限（直接新增到新申报管理下）
--     原按钮挂在旧 202 下，旧菜单已清理，这里直接新增

-- 梓熠、理德 - 申报管理(909) 下的退回上一步
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
('退回上一步', 'business-declaration-rollback-self', 909, 3, NULL, NULL,
    'business:declaration:rollback', NULL, 56, 0, 0, 1, 1, 0, NOW(), NOW()),
('审核退回上一步', 'business-declaration-rollback-audit-self', 909, 3, NULL, NULL,
    'business:declaration:rollback:audit', NULL, 57, 0, 0, 1, 1, 0, NOW(), NOW());

-- 集洛 - 申报管理(919) 下的退回上一步
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
    `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
    `create_time`, `update_time`)
VALUES
('退回上一步', 'business-declaration-rollback-ext', 919, 3, NULL, NULL,
    'business:declaration:rollback', NULL, 56, 0, 0, 1, 1, 0, NOW(), NOW()),
('审核退回上一步', 'business-declaration-rollback-audit-ext', 919, 3, NULL, NULL,
    'business:declaration:rollback:audit', NULL, 57, 0, 0, 1, 1, 0, NOW(), NOW());

-- ============================================================
-- 5. 角色分配 —— 根据数据库实际 sys_role_menu 数据映射
--    原则：拥有旧菜单权限的角色，同时获得 SELF + EXT 两套对应新菜单
-- ============================================================

-- 5.1 顶级菜单：有原"出口申报"(200) 的角色 → 两个新顶级菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 900 FROM `sys_role_menu` rm WHERE rm.menu_id = 200;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 901 FROM `sys_role_menu` rm WHERE rm.menu_id = 200;

-- 5.2 页面菜单：有原对应旧菜单的角色 → SELF + EXT 两套
--     旧 → 新 SELF / 新 EXT 映射：
--     申报录入(205) → 902 / 912    资料提交(206) → 903 / 913
--     补充资料(209) → 904 / 914    开票金额(210) → 905 / 915
--     发票提交(207) → 906 / 916    归档查询(208) → 907 / 917
--     财务单证(202) → 908 / 918    申报管理(201) → 909 / 919
--     申报统计(204) → 910 / 920    申报表单(203) → 911 / 921

-- 申报录入 205 → SELF(902) + EXT(912)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 902 FROM `sys_role_menu` rm WHERE rm.menu_id = 205;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 912 FROM `sys_role_menu` rm WHERE rm.menu_id = 205;

-- 资料提交 206 → SELF(903) + EXT(913)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 903 FROM `sys_role_menu` rm WHERE rm.menu_id = 206;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 913 FROM `sys_role_menu` rm WHERE rm.menu_id = 206;

-- 补充资料 209 → SELF(904) + EXT(914)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 904 FROM `sys_role_menu` rm WHERE rm.menu_id = 209;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 914 FROM `sys_role_menu` rm WHERE rm.menu_id = 209;

-- 开票金额 210 → SELF(905) + EXT(915)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 905 FROM `sys_role_menu` rm WHERE rm.menu_id = 210;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 915 FROM `sys_role_menu` rm WHERE rm.menu_id = 210;

-- 发票提交 207 → SELF(906) + EXT(916)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 906 FROM `sys_role_menu` rm WHERE rm.menu_id = 207;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 916 FROM `sys_role_menu` rm WHERE rm.menu_id = 207;

-- 归档查询 208 → SELF(907) + EXT(917)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 907 FROM `sys_role_menu` rm WHERE rm.menu_id = 208;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 917 FROM `sys_role_menu` rm WHERE rm.menu_id = 208;

-- 财务单证 202 → SELF(908) + EXT(918)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 908 FROM `sys_role_menu` rm WHERE rm.menu_id = 202;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 918 FROM `sys_role_menu` rm WHERE rm.menu_id = 202;

-- 申报管理 201 → SELF(909) + EXT(919)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 909 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 919 FROM `sys_role_menu` rm WHERE rm.menu_id = 201;

-- 申报统计 204 → SELF(910) + EXT(920)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 910 FROM `sys_role_menu` rm WHERE rm.menu_id = 204;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 920 FROM `sys_role_menu` rm WHERE rm.menu_id = 204;

-- 申报表单 203 → SELF(911) + EXT(921)  [隐藏菜单]
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 911 FROM `sys_role_menu` rm WHERE rm.menu_id = 203;
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 921 FROM `sys_role_menu` rm WHERE rm.menu_id = 203;

-- 5.3 按钮权限：有原 201 下按钮的角色 → 对应新按钮（SELF 909 和 EXT 919 下）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, new_m.id
FROM `sys_role_menu` rm
JOIN `sys_menu` old_m ON rm.menu_id = old_m.id AND old_m.parent_id = 201 AND old_m.menu_type = 3
JOIN `sys_menu` new_m ON new_m.permission = old_m.permission AND new_m.parent_id = 909;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, new_m.id
FROM `sys_role_menu` rm
JOIN `sys_menu` old_m ON rm.menu_id = old_m.id AND old_m.parent_id = 201 AND old_m.menu_type = 3
JOIN `sys_menu` new_m ON new_m.permission = old_m.permission AND new_m.parent_id = 919;

-- 5.4 退回上一步按钮权限授权（超级管理员 role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.id FROM `sys_menu` m
WHERE m.permission IN ('business:declaration:rollback', 'business:declaration:rollback:audit')
  AND m.parent_id IN (909, 919) AND m.menu_type = 3;

-- ============================================================
-- 6. 隐藏旧菜单及其所有子菜单、税务退费菜单
-- ============================================================
UPDATE `sys_menu` SET `is_show` = 0, `update_time` = NOW() WHERE `id` = 200;
UPDATE `sys_menu` SET `is_show` = 0, `update_time` = NOW() WHERE `parent_id` = 200;
UPDATE `sys_menu` SET `is_show` = 0, `update_time` = NOW() WHERE `id` = 300;

-- ============================================================
-- 7. 删除旧按钮权限（201 下的所有 menu_type=3 记录）
--    角色映射已在 5.3 完成，先删角色关联再删菜单
-- ============================================================
DELETE rm FROM `sys_role_menu` rm
INNER JOIN `sys_menu` m ON rm.menu_id = m.id
WHERE m.parent_id = 201 AND m.menu_type = 3;

DELETE FROM `sys_menu` WHERE `parent_id` = 201 AND `menu_type` = 3;
