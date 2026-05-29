-- 初始化菜单数据
DELETE FROM sys_menu WHERE id <= 20;

-- 插入根菜单
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, status, deleted, create_time, update_time) VALUES
(1, '首页', 0, 1, '/dashboard', 'dashboard/index.vue', '', 'HomeOutlined', 1, 1, 0, NOW(), NOW()),
(2, '系统管理', 0, 1, '/system', 'Layout', '', 'SettingOutlined', 2, 1, 0, NOW(), NOW()),
(7, '工作流', 0, 1, '/workflow', 'Layout', '', 'BranchesOutlined', 3, 1, 0, NOW(), NOW());

-- 插入系统管理子菜单
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, status, deleted, create_time, update_time) VALUES
(3, '用户管理', 2, 2, 'user', 'system/user/index.vue', 'system:user:list', 'UserOutlined', 1, 1, 0, NOW(), NOW()),
(4, '角色管理', 2, 2, 'role', 'system/role/index.vue', 'system:role:list', 'TeamOutlined', 2, 1, 0, NOW(), NOW()),
(5, '组织管理', 2, 2, 'org', 'system/org/index.vue', 'system:org:list', 'ApartmentOutlined', 3, 1, 0, NOW(), NOW()),
(6, '菜单管理', 2, 2, 'menu', 'system/menu/index.vue', 'system:menu:list', 'MenuOutlined', 4, 1, 0, NOW(), NOW());

-- 插入工作流子菜单
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, status, deleted, create_time, update_time) VALUES
(8, '流程定义', 7, 2, 'definition', 'workflow/definition/index.vue', 'workflow:definition:list', 'FileDoneOutlined', 1, 1, 0, NOW(), NOW()),
(9, '流程设计', 7, 2, 'modeler', 'workflow/modeler/index.vue', 'workflow:modeler:view', 'EditOutlined', 2, 1, 0, NOW(), NOW()),
(10, '流程监控', 7, 2, 'monitor', 'workflow/monitor/index.vue', 'workflow:monitor:view', 'FundViewOutlined', 3, 1, 0, NOW(), NOW()),
(11, '流程实例', 7, 2, 'instance', 'workflow/instance/index.vue', 'workflow:instance:list', 'ProfileOutlined', 4, 1, 0, NOW(), NOW()),
(12, '我的任务', 7, 2, 'task', 'workflow/task/index.vue', 'workflow:task:list', 'CheckCircleOutlined', 5, 1, 0, NOW(), NOW());

-- 插入按钮权限
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, status, deleted, create_time, update_time) VALUES
(13, '用户查询', 3, 3, '', '', 'system:user:query', '', 1, 1, 0, NOW(), NOW()),
(14, '用户新增', 3, 3, '', '', 'system:user:add', '', 2, 1, 0, NOW(), NOW()),
(15, '用户修改', 3, 3, '', '', 'system:user:edit', '', 3, 1, 0, NOW(), NOW()),
(16, '用户删除', 3, 3, '', '', 'system:user:remove', '', 4, 1, 0, NOW(), NOW()),
(17, '重置密码', 3, 3, '', '', 'system:user:resetPwd', '', 5, 1, 0, NOW(), NOW()),

(18, '角色查询', 4, 3, '', '', 'system:role:query', '', 1, 1, 0, NOW(), NOW()),
(19, '角色新增', 4, 3, '', '', 'system:role:add', '', 2, 1, 0, NOW(), NOW()),
(20, '角色修改', 4, 3, '', '', 'system:role:edit', '', 3, 1, 0, NOW(), NOW());