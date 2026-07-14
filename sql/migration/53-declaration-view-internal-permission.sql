-- 53-declaration-view-internal-permission.sql
-- 新增“查看内部申报”和“查看外部申报”权限按钮，对称控制首页/侧边栏菜单可见性

-- 在“申报管理”菜单(id=201)下新增权限按钮
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (2022, '查看内部申报', 'declaration-view-internal', 201, 3, NULL, NULL,
        'business:declaration:view-internal', NULL, 15, 0, 0, 1, 1, 0, NOW(), NOW());

INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (2023, '查看外部申报', 'declaration-view-external', 201, 3, NULL, NULL,
        'business:declaration:view-external', NULL, 16, 0, 0, 1, 1, 0, NOW(), NOW());

-- 将两个新权限自动分配给已有“申报查看”权限的角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 2022
FROM `sys_role_menu` rm
WHERE rm.menu_id = 2011;  -- 2011 = business:declaration:view

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.role_id, 2023
FROM `sys_role_menu` rm
WHERE rm.menu_id = 2011;
