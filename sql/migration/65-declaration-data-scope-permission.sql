-- 65-declaration-data-scope-permission.sql
-- 申报数据权限隔离：新增“查看下级申报”权限点（组织树向下可见），配给部门主管类角色
-- 同时种子“发起资料补交”权限点（资料补交流程用，见 66-material-supplement-flow.sql）

-- ---------- 1. 查看下级申报（business:declaration:view-scope） ----------
-- 拥有该权限的用户可见：自己创建的 + 本组织及所有子组织的申报单
-- 未配置该权限的普通用户仅可见自己创建的申报单
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (81082, '查看下级申报', 'business-declaration-view-scope', 201, 3, NULL, NULL,
        'business:declaration:view-scope', NULL, 58, 0, 0, 1, 1, 0, NOW(), NOW());

-- ---------- 2. 发起资料补交（business:declaration:supplement:initiate） ----------
-- 申报人在资料提交后可发起独立的资料补交流程
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`,
                        `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`,
                        `create_time`, `update_time`)
VALUES (81083, '发起资料补交', 'business-declaration-supplement-initiate', 202, 3, NULL, NULL,
        'business:declaration:supplement:initiate', NULL, 59, 0, 0, 1, 1, 0, NOW(), NOW());

-- ---------- 3. 授权 ----------
-- 超级管理员默认拥有全部新权限
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (1, 81082), (1, 81083);

-- 查看下级申报：自动挂到部门主管类角色（按 role_code/role_name 匹配，存在才生效）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.id, 81082
FROM `sys_role` r
WHERE r.deleted = 0 AND r.status = 1
  AND (r.role_code IN ('DEPT_ADMIN', 'DEPT_MANAGER') OR r.role_name LIKE '%主管%');
