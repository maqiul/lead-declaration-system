-- ========================================
-- 完整菜单权限初始化脚本
-- 生成时间: 2026-03-20
-- 说明: 包含所有三级菜单结构（目录/菜单/按钮）
-- ========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 第一步：清空旧数据
-- ========================================
DELETE FROM sys_role_menu;
DELETE FROM sys_menu;

-- 重置自增ID
ALTER TABLE sys_menu AUTO_INCREMENT = 1;

-- ========================================
-- 第二步：插入一级目录（menu_type=1）
-- ========================================
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1, '首页', 'dashboard', 0, 1, '/', 'Layout', NULL, 'HomeOutlined', 1, 0, 0, 0, 1, 0, NOW(), NOW()),
(100, '系统管理', 'system', 0, 1, '/system', 'Layout', NULL, 'SettingOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(200, '出口申报', 'declaration', 0, 1, '/declaration', 'Layout', NULL, 'FileProtectOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(300, '税务退费', 'tax-refund', 0, 1, '/tax-refund', 'Layout', NULL, 'DollarOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(400, '工作流', 'workflow', 0, 1, '/workflow', 'Layout', NULL, 'BranchesOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(500, '合同管理', 'contract', 0, 1, '/contract', 'Layout', NULL, 'FileTextOutlined', 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(600, '城市管理', 'city-management', 0, 1, '/city-management', 'Layout', NULL, 'EnvironmentOutlined', 7, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第三步：插入二级菜单（menu_type=2）
-- ========================================

-- 首页作为顶级菜单（不嵌套）
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(2, '首页', 'dashboard-index', 0, 2, '/dashboard', '@/views/dashboard/simple.vue', NULL, 'HomeOutlined', 0, 0, 0, 1, 1, 0, NOW(), NOW());

-- 个人中心（独立隐藏结构，不显示在菜单栏，通过顶部用户头像访问）
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(3, '个人中心', 'profile', 0, 1, '/profile', 'Layout', NULL, 'UserOutlined', 99, 0, 0, 0, 1, 0, NOW(), NOW()),
(4, '个人信息', 'profile-index', 3, 2, 'index', '@/views/profile/index.vue', NULL, 'UserOutlined', 1, 0, 0, 0, 1, 0, NOW(), NOW());

-- 系统管理下的菜单 (parent_id=100)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(101, '用户管理', 'system-user', 100, 2, 'user', '@/views/system/user/index.vue', NULL, 'UserOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(102, '角色管理', 'system-role', 100, 2, 'role', '@/views/system/role/index.vue', NULL, 'TeamOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(103, '组织管理', 'system-org', 100, 2, 'org', '@/views/system/org/index.vue', NULL, 'ApartmentOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(104, '菜单管理', 'system-menu', 100, 2, 'menu', '@/views/system/menu/index.vue', NULL, 'MenuOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(105, '银行账户', 'system-bank-account', 100, 2, 'bank-account', '@/views/system/bank-account/index.vue', NULL, 'BankOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(106, '国家信息', 'system-country', 100, 2, 'country', '@/views/system/country/index.vue', NULL, 'GlobalOutlined', 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(107, 'HS商品维护', 'system-product', 100, 2, 'product', '@/views/system/product/index.vue', NULL, 'DatabaseOutlined', 7, 0, 0, 1, 1, 0, NOW(), NOW()),
(108, 'API测试', 'system-api-test', 100, 2, 'api-test', '@/views/system/api-test/index.vue', NULL, 'ApiOutlined', 8, 0, 0, 1, 1, 0, NOW(), NOW()),
(109, '系统配置', 'system-config', 100, 2, 'config', '@/views/system/config/index.vue', NULL, 'SettingOutlined', 9, 0, 0, 1, 1, 0, NOW(), NOW());

-- 出口申报下的菜单 (parent_id=200)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(201, '申报管理', 'declaration-manage', 200, 2, 'manage', '@/views/declaration/manage/index.vue', 'business:declaration:list', 'ContainerOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(202, '申报表单', 'declaration-form', 200, 2, 'form', '@/views/declaration/form/index.vue', NULL, 'FileTextOutlined', 2, 0, 0, 0, 1, 0, NOW(), NOW()),
(203, '支付凭证', 'declaration-payment', 200, 2, 'payment', '@/views/declaration/payment/index.vue', NULL, 'AccountBookOutlined', 3, 0, 0, 0, 1, 0, NOW(), NOW()),
(204, '申报统计', 'declaration-statistics', 200, 2, 'statistics', '@/views/declaration/statistics/index.vue', 'business:declaration:statistics', 'BarChartOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW());

-- 税务退费下的菜单 (parent_id=300)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(301, '退税申请', 'tax-refund-apply', 300, 2, 'apply', '@/views/tax-refund/apply/index.vue', 'business:tax-refund:apply', 'PlusOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(302, '申请列表', 'tax-refund-list', 300, 2, 'list', '@/views/tax-refund/list/index.vue', 'business:tax-refund:list', 'UnorderedListOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(303, '申请详情', 'tax-refund-detail', 300, 2, 'detail', '@/views/tax-refund/detail/index.vue', 'business:tax-refund:detail', 'FileSearchOutlined', 3, 0, 0, 0, 1, 0, NOW(), NOW());

-- 工作流下的菜单 (parent_id=400)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(401, '流程定义', 'workflow-definition', 400, 2, 'definition', '@/views/workflow/definition/index.vue', 'workflow:definition:list', 'FileDoneOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(402, '流程设计', 'workflow-modeler', 400, 2, 'modeler', '@/views/workflow/modeler/index.vue', 'workflow:modeler:view', 'EditOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(403, '流程监控', 'workflow-monitor', 400, 2, 'monitor', '@/views/workflow/monitor/index.vue', 'workflow:monitor:view', 'FundViewOutlined', 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(404, '流程实例', 'workflow-instance', 400, 2, 'instance', '@/views/workflow/instance/index.vue', 'workflow:instance:list', 'ProfileOutlined', 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(405, '我的任务', 'workflow-task', 400, 2, 'task', '@/views/workflow/task/index.vue', 'workflow:task:list', 'CheckCircleOutlined', 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 工作流按钮权限 (parent_id=401, 404, 405)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(4011, '流程部署', 'workflow-definition-deploy', 401, 3, NULL, NULL, 'workflow:definition:deploy', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(4012, '流程更新', 'workflow-definition-update', 401, 3, NULL, NULL, 'workflow:definition:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(4041, '流程实例终止', 'workflow-instance-terminate', 404, 3, NULL, NULL, 'workflow:instance:terminate', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(4042, '实例启动', 'workflow-instance-start', 404, 3, NULL, NULL, 'workflow:instance:start', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(4043, '实例暂停', 'workflow-instance-suspend', 404, 3, NULL, NULL, 'workflow:instance:suspend', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(4044, '实例激活', 'workflow-instance-activate', 404, 3, NULL, NULL, 'workflow:instance:activate', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(4051, '任务拒绝', 'workflow-task-reject', 405, 3, NULL, NULL, 'workflow:task:reject', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(4052, '任务转派', 'workflow-task-transfer', 405, 3, NULL, NULL, 'workflow:task:transfer', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW());

-- 合同管理下的菜单 (parent_id=500)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(501, '模板管理', 'contract-template', 500, 2, 'template', '@/views/contract/template/index.vue', 'business:contract:template', 'FileAddOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(502, '合同列表', 'contract-generation', 500, 2, 'generation', '@/views/contract/generation/index.vue', 'business:contract:generation', 'HistoryOutlined', 2, 0, 0, 1, 1, 0, NOW(), NOW());

-- 城市管理下的菜单 (parent_id=600)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(601, '城市信息', 'city-info', 600, 2, 'city-info', '@/views/system/city-info/index.vue', 'system:city-info:list', 'EnvironmentOutlined', 1, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第四步：插入三级按钮权限（menu_type=3）
-- ========================================

-- 用户管理下的按钮 (parent_id=101)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1011, '用户查询', 'user-query', 101, 3, NULL, NULL, 'user:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1012, '用户新增', 'user-add', 101, 3, NULL, NULL, 'user:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1013, '用户编辑', 'user-update', 101, 3, NULL, NULL, 'user:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1014, '用户删除', 'user-delete', 101, 3, NULL, NULL, 'user:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1015, '用户列表', 'user-list', 101, 3, NULL, NULL, 'user:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(1016, '密码重置', 'user-resetPwd', 101, 3, NULL, NULL, 'user:resetPwd', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());

-- 角色管理下的按钮 (parent_id=102)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1021, '角色查询', 'role-query', 102, 3, NULL, NULL, 'role:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1022, '角色新增', 'role-add', 102, 3, NULL, NULL, 'role:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1023, '角色编辑', 'role-update', 102, 3, NULL, NULL, 'role:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1024, '角色删除', 'role-delete', 102, 3, NULL, NULL, 'role:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1025, '角色列表', 'role-list', 102, 3, NULL, NULL, 'role:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(1026, '用户角色管理', 'role-user', 102, 3, NULL, NULL, 'role:user', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(1027, '角色分配', 'role-assign', 102, 3, NULL, NULL, 'role:assign', NULL, 7, 0, 0, 1, 1, 0, NOW(), NOW()),
(1028, '菜单权限管理', 'role-menu', 102, 3, NULL, NULL, 'role:menu', NULL, 8, 0, 0, 1, 1, 0, NOW(), NOW());

-- 组织管理下的按钮 (parent_id=103)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1031, '组织查询', 'org-query', 103, 3, NULL, NULL, 'org:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1032, '组织新增', 'org-add', 103, 3, NULL, NULL, 'org:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1033, '组织编辑', 'org-update', 103, 3, NULL, NULL, 'org:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1034, '组织删除', 'org-delete', 103, 3, NULL, NULL, 'org:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1035, '组织列表', 'org-list', 103, 3, NULL, NULL, 'org:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(1036, '组织用户管理', 'org-user', 103, 3, NULL, NULL, 'org:user', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());

-- 菜单管理下的按钮 (parent_id=104)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1041, '菜单查询', 'menu-query', 104, 3, NULL, NULL, 'menu:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1042, '菜单新增', 'menu-add', 104, 3, NULL, NULL, 'menu:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1043, '菜单编辑', 'menu-update', 104, 3, NULL, NULL, 'menu:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1044, '菜单删除', 'menu-delete', 104, 3, NULL, NULL, 'menu:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1045, '菜单列表', 'menu-list', 104, 3, NULL, NULL, 'menu:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 银行账户下的按钮 (parent_id=105)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1051, '银行账户列表', 'bank-account-list', 105, 3, NULL, NULL, 'system:bank-account:list', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1052, '银行账户新增', 'bank-account-add', 105, 3, NULL, NULL, 'system:bank-account:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1053, '银行账户编辑', 'bank-account-update', 105, 3, NULL, NULL, 'system:bank-account:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1054, '银行账户删除', 'bank-account-delete', 105, 3, NULL, NULL, 'system:bank-account:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1055, '银行账户查看', 'bank-account-view', 105, 3, NULL, NULL, 'system:bank-account:view', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 国家信息下的按钮 (parent_id=106)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1061, '国家信息列表', 'country-list', 106, 3, NULL, NULL, 'system:country:list', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1062, '国家信息新增', 'country-add', 106, 3, NULL, NULL, 'system:country:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1063, '国家信息编辑', 'country-edit', 106, 3, NULL, NULL, 'system:country:edit', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1064, '国家信息删除', 'country-delete', 106, 3, NULL, NULL, 'system:country:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1065, '国家信息查看', 'country-view', 106, 3, NULL, NULL, 'system:country:view', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- HS商品维护下的按钮 (parent_id=107)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1071, '商品查询', 'product-query', 107, 3, NULL, NULL, 'system:product:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1072, '商品新增', 'product-add', 107, 3, NULL, NULL, 'system:product:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1073, '商品编辑', 'product-update', 107, 3, NULL, NULL, 'system:product:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1074, '商品删除', 'product-delete', 107, 3, NULL, NULL, 'system:product:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1075, '商品列表', 'product-list', 107, 3, NULL, NULL, 'system:product:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 系统配置下的按钮 (parent_id=109)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(1091, '配置查询', 'config-query', 109, 3, NULL, NULL, 'system:config:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(1092, '配置新增', 'config-add', 109, 3, NULL, NULL, 'system:config:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(1093, '配置编辑', 'config-update', 109, 3, NULL, NULL, 'system:config:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(1094, '配置删除', 'config-delete', 109, 3, NULL, NULL, 'system:config:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(1095, '配置列表', 'config-list', 109, 3, NULL, NULL, 'system:config:list', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 申报管理下的按钮 (parent_id=201)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(2011, '申报查询', 'declaration-list', 201, 3, NULL, NULL, 'business:declaration:list', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(2012, '新增申报', 'declaration-add', 201, 3, NULL, NULL, 'business:declaration:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(2013, '编辑申报', 'declaration-update', 201, 3, NULL, NULL, 'business:declaration:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(2014, '删除申报', 'declaration-delete', 201, 3, NULL, NULL, 'business:declaration:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(2015, '查看申报', 'declaration-view', 201, 3, NULL, NULL, 'business:declaration:view', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(2016, '申报详情查询', 'declaration-query', 201, 3, NULL, NULL, 'business:declaration:query', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW()),
(2017, '提交申报', 'declaration-submit', 201, 3, NULL, NULL, 'business:declaration:submit', NULL, 7, 0, 0, 1, 1, 0, NOW(), NOW()),
(2018, '申报审核', 'declaration-audit', 201, 3, NULL, NULL, 'business:declaration:audit', NULL, 8, 0, 0, 1, 1, 0, NOW(), NOW()),
(2019, '导出申报', 'declaration-export', 201, 3, NULL, NULL, 'business:declaration:export', NULL, 9, 0, 0, 1, 1, 0, NOW(), NOW()),
(2020, '下载单证', 'declaration-download', 201, 3, NULL, NULL, 'business:declaration:download', NULL, 10, 0, 0, 1, 1, 0, NOW(), NOW()),
(2021, '生成合同', 'declaration-contract', 201, 3, NULL, NULL, 'business:declaration:contract', NULL, 11, 0, 0, 1, 1, 0, NOW(), NOW()),
(2022, '付款管理', 'declaration-payment', 201, 3, NULL, NULL, 'business:declaration:payment', NULL, 12, 0, 0, 1, 1, 0, NOW(), NOW()),
(2023, '申报编辑权限', 'declaration-edit', 201, 3, NULL, NULL, 'business:declaration:edit', NULL, 13, 0, 0, 1, 1, 0, NOW(), NOW()),
(2024, '提货单提交', 'declaration-pickup-submit', 201, 3, NULL, NULL, 'business:declaration:pickup-submit', NULL, 14, 0, 0, 1, 1, 0, NOW(), NOW()),
(2025, '提货单审核', 'declaration-pickup-audit', 201, 3, NULL, NULL, 'business:declaration:pickup-audit', NULL, 15, 0, 0, 1, 1, 0, NOW(), NOW()),
(2026, '提货单删除', 'declaration-pickup-delete', 201, 3, NULL, NULL, 'business:declaration:pickup-delete', NULL, 16, 0, 0, 1, 1, 0, NOW(), NOW());

-- 退税申请下的按钮 (parent_id=301)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(3011, '申请查询', 'tax-refund-apply-query', 301, 3, NULL, NULL, 'business:tax-refund:apply:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(3012, '申请新增', 'tax-refund-apply-add', 301, 3, NULL, NULL, 'business:tax-refund:apply:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(3013, '申请编辑', 'tax-refund-apply-update', 301, 3, NULL, NULL, 'business:tax-refund:apply:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(3014, '申请删除', 'tax-refund-apply-delete', 301, 3, NULL, NULL, 'business:tax-refund:apply:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(3015, '新增退费', 'tax-refund-add', 301, 3, NULL, NULL, 'business:tax-refund:add', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW());

-- 申请列表下的按钮 (parent_id=302)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(3021, '列表查询', 'tax-refund-list-query', 302, 3, NULL, NULL, 'business:tax-refund:list:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(3022, '审核操作', 'tax-refund-approve', 302, 3, NULL, NULL, 'business:tax-refund:approve', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(3023, '申请审核', 'tax-refund-sys-approve', 302, 3, NULL, NULL, 'system:tax-refund:approve', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(3024, '财务初审', 'tax-refund-first-review', 302, 3, NULL, NULL, 'system:tax-refund:first-review', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(3025, '财务复审', 'tax-refund-final-review', 302, 3, NULL, NULL, 'system:tax-refund:final-review', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(3026, '财务审核', 'tax-refund-finance', 302, 3, NULL, NULL, 'business:tax-refund:finance', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());

-- 模板管理下的按钮 (parent_id=501)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(5011, '模板查询', 'contract-template-query', 501, 3, NULL, NULL, 'business:contract:template:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(5012, '模板新增', 'contract-template-add', 501, 3, NULL, NULL, 'business:contract:template:add', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(5013, '模板编辑', 'contract-template-update', 501, 3, NULL, NULL, 'business:contract:template:update', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW()),
(5014, '模板删除', 'contract-template-delete', 501, 3, NULL, NULL, 'business:contract:template:delete', NULL, 4, 0, 0, 1, 1, 0, NOW(), NOW()),
(5015, '模板上传', 'contract-template-upload', 501, 3, NULL, NULL, 'business:contract:template:upload', NULL, 5, 0, 0, 1, 1, 0, NOW(), NOW()),
(5016, '合同生成', 'contract-generate', 501, 3, NULL, NULL, 'business:contract:generate', NULL, 6, 0, 0, 1, 1, 0, NOW(), NOW());

-- 合同列表下的按钮 (parent_id=502)
INSERT INTO sys_menu (id, menu_name, menu_code, parent_id, menu_type, path, component, permission, icon, sort, is_external, is_cache, is_show, status, deleted, create_time, update_time) VALUES
(5021, '合同查询', 'contract-generation-query', 502, 3, NULL, NULL, 'business:contract:generation:query', NULL, 1, 0, 0, 1, 1, 0, NOW(), NOW()),
(5022, '合同下载', 'contract-download', 502, 3, NULL, NULL, 'business:contract:download', NULL, 2, 0, 0, 1, 1, 0, NOW(), NOW()),
(5023, '合同删除', 'contract-generation-delete', 502, 3, NULL, NULL, 'business:contract:generation:delete', NULL, 3, 0, 0, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第五步：分配角色权限
-- ========================================

-- 1. 给管理员角色(role_id=1)分配所有权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW() FROM sys_menu WHERE deleted = 0 AND status = 1;

-- 2. 给普通用户角色(role_id=2)分配基础权限
-- 首页、个人中心、申报列表查看、退费申请
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
-- 首页目录和菜单
(2, 1, NOW()),   -- 首页目录
(2, 2, NOW()),   -- 首页
(2, 3, NOW()),   -- 个人中心目录
(2, 4, NOW()),   -- 个人信息
-- 出口申报（只读）
(2, 200, NOW()), -- 出口申报目录
(2, 201, NOW()), -- 申报管理
(2, 2011, NOW()), -- 申报查询
(2, 2015, NOW()), -- 查看申报
(2, 2016, NOW()), -- 申报详情查询
-- 申报统计
(2, 204, NOW()), -- 申报统计
-- 税务退费（只读）
(2, 300, NOW()), -- 税务退费目录
(2, 302, NOW()), -- 申请列表
(2, 3021, NOW()); -- 列表查询

-- 3. 给部门管理员角色(role_id=3)分配权限
-- 首页、组织管理、出口申报（查看/编辑/提交/审核）
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
-- 首页
(3, 1, NOW()),
(3, 2, NOW()),
(3, 3, NOW()),   -- 个人中心目录
(3, 4, NOW()),   -- 个人信息
-- 系统管理-组织管理
(3, 100, NOW()),
(3, 103, NOW()),
(3, 1031, NOW()),
(3, 1032, NOW()),
(3, 1033, NOW()),
(3, 1034, NOW()),
(3, 1035, NOW()),
(3, 1036, NOW()),
-- 出口申报（查看/编辑/提交/审核）
(3, 200, NOW()),
(3, 201, NOW()),
(3, 2011, NOW()),
(3, 2013, NOW()),
(3, 2015, NOW()),
(3, 2016, NOW()),
(3, 2017, NOW()),
(3, 2018, NOW());

-- 4. 给财务角色(role_id=4)分配财务相关权限
-- 首页、申报管理全部、税务退费全部、合同管理全部
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
-- 首页
(4, 1, NOW()),
(4, 2, NOW()),
(4, 3, NOW()),   -- 个人中心目录
(4, 4, NOW()),   -- 个人信息
-- 出口申报全部
(4, 200, NOW()),
(4, 201, NOW()),
(4, 202, NOW()),
(4, 203, NOW()),
(4, 204, NOW()),
(4, 2011, NOW()),
(4, 2012, NOW()),
(4, 2013, NOW()),
(4, 2014, NOW()),
(4, 2015, NOW()),
(4, 2016, NOW()),
(4, 2017, NOW()),
(4, 2018, NOW()),
(4, 2019, NOW()),
(4, 2020, NOW()),
(4, 2021, NOW()),
(4, 2022, NOW()),
(4, 2023, NOW()),
(4, 2024, NOW()),
(4, 2025, NOW()),
(4, 2026, NOW()),
-- 税务退费全部
(4, 300, NOW()),
(4, 301, NOW()),
(4, 302, NOW()),
(4, 303, NOW()),
(4, 3011, NOW()),
(4, 3012, NOW()),
(4, 3013, NOW()),
(4, 3014, NOW()),
(4, 3015, NOW()),
(4, 3021, NOW()),
(4, 3022, NOW()),
(4, 3023, NOW()),
(4, 3024, NOW()),
(4, 3025, NOW()),
(4, 3026, NOW()),
-- 合同管理全部
(4, 500, NOW()),
(4, 501, NOW()),
(4, 502, NOW()),
(4, 5011, NOW()),
(4, 5012, NOW()),
(4, 5013, NOW()),
(4, 5014, NOW()),
(4, 5015, NOW()),
(4, 5021, NOW()),
(4, 5022, NOW()),
(4, 5023, NOW()),
-- 新增权限
(4, 1055, NOW()), -- 银行账户查看
(4, 1065, NOW()), -- 国家信息查看
(4, 5016, NOW()); -- 合同生成

-- 5. 给销售员角色(role_id=5)分配销售相关权限
-- 首页、申报管理（查看/新增/编辑/提交）、合同查看
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES
-- 首页
(5, 1, NOW()),
(5, 2, NOW()),
(5, 3, NOW()),   -- 个人中心目录
(5, 4, NOW()),   -- 个人信息
-- 出口申报（查看/新增/编辑/提交）
(5, 200, NOW()),
(5, 201, NOW()),
(5, 202, NOW()),
(5, 2011, NOW()),
(5, 2012, NOW()),
(5, 2013, NOW()),
(5, 2015, NOW()),
(5, 2016, NOW()),
(5, 2017, NOW()),
(5, 2019, NOW()),
(5, 2020, NOW()),
(5, 2021, NOW()),
(5, 2022, NOW()),
(5, 2023, NOW()),
-- 合同查看
(5, 500, NOW()),
(5, 502, NOW()),
(5, 5021, NOW()),
(5, 5022, NOW()),
(5, 5016, NOW()); -- 合同生成

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 完成提示
-- ========================================
SELECT '权限初始化完成!' AS message;
SELECT COUNT(*) AS total_menus FROM sys_menu WHERE deleted = 0;
SELECT menu_type, COUNT(*) AS count FROM sys_menu WHERE deleted = 0 GROUP BY menu_type ORDER BY menu_type;
