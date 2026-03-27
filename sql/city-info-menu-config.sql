-- 城市信息管理菜单配置
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort`, `path`, `component`, `permission`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2000, '城市管理', 0, 4, 'city-management', 'Layout', '', 'location', 'admin', NOW(), 'admin', NOW(), '城市管理目录'),
(2001, '城市信息', 2000, 1, 'city-info', 'system/city-info/index', 'system:city-info:view', 'location', 'admin', NOW(), 'admin', NOW(), '城市信息管理');

-- 城市信息管理按钮权限
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `sort`, `permission`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(2002, '城市新增', 2001, 1, 'system:city-info:add', '#', 'admin', NOW(), 'admin', NOW(), '城市信息新增权限'),
(2003, '城市修改', 2001, 2, 'system:city-info:update', '#', 'admin', NOW(), 'admin', NOW(), '城市信息修改权限'),
(2004, '城市删除', 2001, 3, 'system:city-info:delete', '#', 'admin', NOW(), 'admin', NOW(), '城市信息删除权限'),
(2005, '城市查询', 2001, 4, 'system:city-info:query', '#', 'admin', NOW(), 'admin', NOW(), '城市信息查询权限');