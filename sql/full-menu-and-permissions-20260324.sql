-- ==========================================================
-- 项目完整全量菜单及相关权限初始化脚本 (含全部三级按钮与角色关联)
-- 包含合并: 基础系统、报关申报、税务退费、工作流、合同管理、
--          运输方式、货币管理、财务票据模块
-- 生成要求: 真正的全量输出，拒绝简略和缺漏
-- ==========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清理被完全覆盖的表数据（为保证数据一致且避免自增键错乱，建议做全面清理并重灌）
TRUNCATE TABLE sys_role_menu;
TRUNCATE TABLE sys_menu;
ALTER TABLE sys_menu AUTO_INCREMENT = 1;

-- ========================================
-- 第一步：插入一级目录（menu_type=1）
-- ========================================
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(1, '首页', 0, 1, '/', 'Layout', NULL, 'HomeOutlined', 1, 0, 1, 0, NOW(), NOW()),
(100, '系统管理', 0, 1, '/system', 'Layout', NULL, 'SettingOutlined', 2, 1, 1, 0, NOW(), NOW()),
(200, '出口申报', 0, 1, '/declaration', 'Layout', NULL, 'FileProtectOutlined', 3, 1, 1, 0, NOW(), NOW()),
(300, '税务退费', 0, 1, '/tax-refund', 'Layout', NULL, 'DollarOutlined', 4, 1, 1, 0, NOW(), NOW()),
(400, '工作流', 0, 1, '/workflow', 'Layout', NULL, 'BranchesOutlined', 5, 1, 1, 0, NOW(), NOW()),
(500, '合同管理', 0, 1, '/contract', 'Layout', NULL, 'FileTextOutlined', 6, 1, 1, 0, NOW(), NOW()),
(600, '财务票据管理', 0, 1, '/financial', 'Layout', NULL, 'MoneyCollectOutlined', 8, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第二步：插入二级菜单（menu_type=2）
-- ========================================
-- [首页模块]
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(2, '首页', 0, 2, '/dashboard', '@/views/dashboard/simple.vue', NULL, 'HomeOutlined', 0, 1, 1, 0, NOW(), NOW());

-- [个人中心隐藏模块]
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(3, '个人中心', 0, 1, '/profile', 'Layout', NULL, 'UserOutlined', 99, 0, 1, 0, NOW(), NOW()),
(4, '个人信息', 3, 2, 'index', '@/views/profile/index.vue', NULL, 'UserOutlined', 1, 0, 1, 0, NOW(), NOW());

-- [系统管理模块] (parent_id=100)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(101, '用户管理', 100, 2, 'user', '@/views/system/user/index.vue', 'system:user:list', 'UserOutlined', 1, 1, 1, 0, NOW(), NOW()),
(102, '角色管理', 100, 2, 'role', '@/views/system/role/index.vue', 'system:role:list', 'TeamOutlined', 2, 1, 1, 0, NOW(), NOW()),
(103, '组织管理', 100, 2, 'org', '@/views/system/org/index.vue', 'system:org:list', 'ApartmentOutlined', 3, 1, 1, 0, NOW(), NOW()),
(104, '菜单管理', 100, 2, 'menu', '@/views/system/menu/index.vue', 'system:menu:list', 'MenuOutlined', 4, 1, 1, 0, NOW(), NOW()),
(105, '银行账户', 100, 2, 'bank-account', '@/views/system/bank-account/index.vue', 'system:bank-account:list', 'BankOutlined', 5, 1, 1, 0, NOW(), NOW()),
(106, '国家信息', 100, 2, 'country', '@/views/system/country/index.vue', 'system:country:list', 'GlobalOutlined', 6, 1, 1, 0, NOW(), NOW()),
(107, 'HS商品维护', 100, 2, 'product', '@/views/system/product/index.vue', 'system:product:list', 'DatabaseOutlined', 7, 1, 1, 0, NOW(), NOW()),
(108, 'API测试', 100, 2, 'api-test', '@/views/system/api-test/index.vue', 'system:api-test:list', 'ApiOutlined', 8, 1, 1, 0, NOW(), NOW()),
(109, '系统配置', 100, 2, 'config', '@/views/system/config/index.vue', 'system:config:list', 'SettingOutlined', 9, 1, 1, 0, NOW(), NOW()),
(110, '运输方式', 100, 2, 'transport-mode', '@/views/system/transport-mode/index.vue', 'system:transport:list', 'CarOutlined', 10, 1, 1, 0, NOW(), NOW()),
(111, '货币管理', 100, 2, 'currency', '@/views/system/currency/index.vue', 'system:currency:list', 'MoneyCollectOutlined', 11, 1, 1, 0, NOW(), NOW());

-- [出口申报模块] (parent_id=200)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(201, '申报管理', 200, 2, 'manage', '@/views/declaration/manage/index.vue', 'business:declaration:list', 'ContainerOutlined', 1, 1, 1, 0, NOW(), NOW()),
(202, '财务单证', 200, 2, 'finance', '@/views/declaration/finance/index.vue', 'business:declaration:financeSupplement', 'PayCircleOutlined', 2, 1, 1, 0, NOW(), NOW()),
(203, '申报表单', 200, 2, 'form', '@/views/declaration/form/index.vue', 'business:declaration:form', 'FileTextOutlined', 3, 0, 1, 0, NOW(), NOW()),
(204, '支付凭证', 200, 2, 'payment', '@/views/declaration/payment/index.vue', 'business:declaration:payment', 'AccountBookOutlined', 4, 0, 1, 0, NOW(), NOW()),
(205, '申报统计', 200, 2, 'statistics', '@/views/declaration/statistics/index.vue', 'business:declaration:statistics', 'BarChartOutlined', 5, 1, 1, 0, NOW(), NOW());

-- [税务退费模块] (parent_id=300)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(301, '退税申请', 300, 2, 'apply', '@/views/tax-refund/apply/index.vue', 'business:tax-refund:apply', 'PlusOutlined', 1, 1, 1, 0, NOW(), NOW()),
(302, '申请列表', 300, 2, 'list', '@/views/tax-refund/list/index.vue', 'business:tax-refund:list', 'UnorderedListOutlined', 2, 1, 1, 0, NOW(), NOW()),
(303, '申请详情', 300, 2, 'detail/:id', '@/views/tax-refund/detail/index.vue', 'business:tax-refund:detail', 'FileSearchOutlined', 3, 0, 1, 0, NOW(), NOW());

-- [工作流模块] (parent_id=400)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(401, '流程定义', 400, 2, 'definition', '@/views/workflow/definition/index.vue', 'workflow:definition:list', 'FileDoneOutlined', 1, 1, 1, 0, NOW(), NOW()),
(402, '流程设计', 400, 2, 'modeler', '@/views/workflow/modeler/index.vue', 'workflow:modeler:view', 'EditOutlined', 2, 1, 1, 0, NOW(), NOW()),
(403, '流程监控', 400, 2, 'monitor', '@/views/workflow/monitor/index.vue', 'workflow:monitor:view', 'FundViewOutlined', 3, 1, 1, 0, NOW(), NOW()),
(404, '流程实例', 400, 2, 'instance', '@/views/workflow/instance/index.vue', 'workflow:instance:list', 'ProfileOutlined', 4, 1, 1, 0, NOW(), NOW()),
(405, '我的任务', 400, 2, 'task', '@/views/workflow/task/index.vue', 'workflow:task:list', 'CheckCircleOutlined', 5, 1, 1, 0, NOW(), NOW());

-- [合同管理模块] (parent_id=500)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(501, '模板管理', 500, 2, 'template', '@/views/contract/template/index.vue', 'business:contract:template', 'FileAddOutlined', 1, 1, 1, 0, NOW(), NOW()),
(502, '合同列表', 500, 2, 'generation', '@/views/contract/generation/index.vue', 'business:contract:generation', 'HistoryOutlined', 2, 1, 1, 0, NOW(), NOW());

-- [财务票据管理模块] (parent_id=600)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, icon, sort, is_show, status, deleted, create_time, update_time) VALUES
(601, '货代发票', 600, 2, 'freight', 'financial/freight/index', 'financial:freight:list', 'profile', 1, 1, 1, 0, NOW(), NOW()),
(602, '报关发票', 600, 2, 'customs', 'financial/customs/index', 'financial:customs:list', 'file-search', 2, 1, 1, 0, NOW(), NOW()),
(603, '开票明细', 600, 2, 'detail', 'financial/detail/index', 'financial:detail:list', 'unordered-list', 3, 1, 1, 0, NOW(), NOW());


-- ========================================
-- 第三步：插入底层全面三级按钮权限（menu_type=3）
-- 保证每一个按钮都能精确对应到权限！
-- ========================================

-- 用户管理 (101)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1011, '用户查询', 101, 3, '', '', 'user:query', 1, 1, 1, 0, NOW(), NOW()),
(1012, '用户新增', 101, 3, '', '', 'user:add', 2, 1, 1, 0, NOW(), NOW()),
(1013, '用户编辑', 101, 3, '', '', 'user:update', 3, 1, 1, 0, NOW(), NOW()),
(1014, '用户删除', 101, 3, '', '', 'user:delete', 4, 1, 1, 0, NOW(), NOW()),
(1015, '用户列表', 101, 3, '', '', 'user:list', 5, 1, 1, 0, NOW(), NOW()),
(1016, '密码重置', 101, 3, '', '', 'user:resetPwd', 6, 1, 1, 0, NOW(), NOW());

-- 角色管理 (102)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1021, '角色查询', 102, 3, '', '', 'role:query', 1, 1, 1, 0, NOW(), NOW()),
(1022, '角色新增', 102, 3, '', '', 'role:add', 2, 1, 1, 0, NOW(), NOW()),
(1023, '角色编辑', 102, 3, '', '', 'role:update', 3, 1, 1, 0, NOW(), NOW()),
(1024, '角色删除', 102, 3, '', '', 'role:delete', 4, 1, 1, 0, NOW(), NOW()),
(1025, '角色列表', 102, 3, '', '', 'role:list', 5, 1, 1, 0, NOW(), NOW()),
(1026, '用户角色管理', 102, 3, '', '', 'role:user', 6, 1, 1, 0, NOW(), NOW()),
(1027, '角色分配', 102, 3, '', '', 'role:assign', 7, 1, 1, 0, NOW(), NOW()),
(1028, '菜单权限管理', 102, 3, '', '', 'role:menu', 8, 1, 1, 0, NOW(), NOW());

-- 组织管理 (103)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1031, '组织查询', 103, 3, '', '', 'org:query', 1, 1, 1, 0, NOW(), NOW()),
(1032, '组织新增', 103, 3, '', '', 'org:add', 2, 1, 1, 0, NOW(), NOW()),
(1033, '组织编辑', 103, 3, '', '', 'org:update', 3, 1, 1, 0, NOW(), NOW()),
(1034, '组织删除', 103, 3, '', '', 'org:delete', 4, 1, 1, 0, NOW(), NOW()),
(1035, '组织列表', 103, 3, '', '', 'org:list', 5, 1, 1, 0, NOW(), NOW()),
(1036, '组织用户管理', 103, 3, '', '', 'org:user', 6, 1, 1, 0, NOW(), NOW());

-- 菜单管理 (104)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1041, '菜单查询', 104, 3, '', '', 'menu:query', 1, 1, 1, 0, NOW(), NOW()),
(1042, '菜单新增', 104, 3, '', '', 'menu:add', 2, 1, 1, 0, NOW(), NOW()),
(1043, '菜单编辑', 104, 3, '', '', 'menu:update', 3, 1, 1, 0, NOW(), NOW()),
(1044, '菜单删除', 104, 3, '', '', 'menu:delete', 4, 1, 1, 0, NOW(), NOW()),
(1045, '菜单列表', 104, 3, '', '', 'menu:list', 5, 1, 1, 0, NOW(), NOW());

-- 银行账户 (105)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1051, '列表查看', 105, 3, '', '', 'system:bank-account:list', 1, 1, 1, 0, NOW(), NOW()),
(1052, '账户新增', 105, 3, '', '', 'system:bank-account:add', 2, 1, 1, 0, NOW(), NOW()),
(1053, '账户编辑', 105, 3, '', '', 'system:bank-account:update', 3, 1, 1, 0, NOW(), NOW()),
(1054, '账户删除', 105, 3, '', '', 'system:bank-account:delete', 4, 1, 1, 0, NOW(), NOW()),
(1055, '账户查看', 105, 3, '', '', 'system:bank-account:view', 5, 1, 1, 0, NOW(), NOW());

-- 国家信息 (106)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1061, '信息列表', 106, 3, '', '', 'system:country:list', 1, 1, 1, 0, NOW(), NOW()),
(1062, '信息新增', 106, 3, '', '', 'system:country:add', 2, 1, 1, 0, NOW(), NOW()),
(1063, '信息编辑', 106, 3, '', '', 'system:country:edit', 3, 1, 1, 0, NOW(), NOW()),
(1064, '信息删除', 106, 3, '', '', 'system:country:delete', 4, 1, 1, 0, NOW(), NOW()),
(1065, '信息查看', 106, 3, '', '', 'system:country:view', 5, 1, 1, 0, NOW(), NOW());

-- HS商品 (107)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1071, '商品查询', 107, 3, '', '', 'system:product:query', 1, 1, 1, 0, NOW(), NOW()),
(1072, '商品新增', 107, 3, '', '', 'system:product:add', 2, 1, 1, 0, NOW(), NOW()),
(1073, '商品编辑', 107, 3, '', '', 'system:product:update', 3, 1, 1, 0, NOW(), NOW()),
(1074, '商品删除', 107, 3, '', '', 'system:product:delete', 4, 1, 1, 0, NOW(), NOW()),
(1075, '商品列表', 107, 3, '', '', 'system:product:list', 5, 1, 1, 0, NOW(), NOW());

-- 系统配置 (109)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1091, '配置查询', 109, 3, '', '', 'system:config:query', 1, 1, 1, 0, NOW(), NOW()),
(1092, '配置新增', 109, 3, '', '', 'system:config:add', 2, 1, 1, 0, NOW(), NOW()),
(1093, '配置编辑', 109, 3, '', '', 'system:config:update', 3, 1, 1, 0, NOW(), NOW()),
(1094, '配置删除', 109, 3, '', '', 'system:config:delete', 4, 1, 1, 0, NOW(), NOW()),
(1095, '配置列表', 109, 3, '', '', 'system:config:list', 5, 1, 1, 0, NOW(), NOW());

-- 运输方式 (110)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1101, '列表查看', 110, 3, '', '', 'system:transport:list', 1, 1, 1, 0, NOW(), NOW()),
(1102, '详情查看', 110, 3, '', '', 'system:transport:view', 2, 1, 1, 0, NOW(), NOW()),
(1103, '运输方式新增', 110, 3, '', '', 'system:transport:add', 3, 1, 1, 0, NOW(), NOW()),
(1104, '运输方式编辑', 110, 3, '', '', 'system:transport:edit', 4, 1, 1, 0, NOW(), NOW()),
(1105, '运输方式删除', 110, 3, '', '', 'system:transport:delete', 5, 1, 1, 0, NOW(), NOW());

-- 货币管理 (111)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(1111, '货币查询', 111, 3, '', '', 'system:currency:query', 1, 1, 1, 0, NOW(), NOW()),
(1112, '货币新增', 111, 3, '', '', 'system:currency:add', 2, 1, 1, 0, NOW(), NOW()),
(1113, '货币编辑', 111, 3, '', '', 'system:currency:edit', 3, 1, 1, 0, NOW(), NOW()),
(1114, '货币删除', 111, 3, '', '', 'system:currency:delete', 4, 1, 1, 0, NOW(), NOW());

-- 出口申报管理所有相关按钮 (201)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(2010, '申报查询', 201, 3, '', '', 'business:declaration:list', 1, 1, 1, 0, NOW(), NOW()),
(2011, '新增申报', 201, 3, '', '', 'business:declaration:add', 2, 1, 1, 0, NOW(), NOW()),
(2012, '编辑申报', 201, 3, '', '', 'business:declaration:update', 3, 1, 1, 0, NOW(), NOW()),
(2013, '删除申报', 201, 3, '', '', 'business:declaration:delete', 4, 1, 1, 0, NOW(), NOW()),
(2014, '查看申报', 201, 3, '', '', 'business:declaration:view', 5, 1, 1, 0, NOW(), NOW()),
(2015, '详情查询', 201, 3, '', '', 'business:declaration:query', 6, 1, 1, 0, NOW(), NOW()),
(2016, '提交申报', 201, 3, '', '', 'business:declaration:submit', 7, 1, 1, 0, NOW(), NOW()),
(2017, '申报审核', 201, 3, '', '', 'business:declaration:audit', 8, 1, 1, 0, NOW(), NOW()),
(2018, '导出申报', 201, 3, '', '', 'business:declaration:export', 9, 1, 1, 0, NOW(), NOW()),
(2019, '下载单证', 201, 3, '', '', 'business:declaration:download', 10, 1, 1, 0, NOW(), NOW()),
(2020, '生成合同', 201, 3, '', '', 'business:declaration:contract', 11, 1, 1, 0, NOW(), NOW()),
(2021, '付款管理', 201, 3, '', '', 'business:declaration:payment', 12, 1, 1, 0, NOW(), NOW()),
(2022, '申报权限管理', 201, 3, '', '', 'business:declaration:edit', 13, 1, 1, 0, NOW(), NOW()),
(2023, '提货单提交', 201, 3, '', '', 'business:declaration:pickup-submit', 14, 1, 1, 0, NOW(), NOW()),
(2024, '提货单审核', 201, 3, '', '', 'business:declaration:pickup-audit', 15, 1, 1, 0, NOW(), NOW()),
(2025, '提货单删除', 201, 3, '', '', 'business:declaration:pickup-delete', 16, 1, 1, 0, NOW(), NOW());

-- 退税申请按钮 (301)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(3011, '申请查询', 301, 3, '', '', 'business:tax-refund:list', 1, 1, 1, 0, NOW(), NOW()),
(3012, '申请新增', 301, 3, '', '', 'business:tax-refund:add', 2, 1, 1, 0, NOW(), NOW()),
(3013, '申请编辑', 301, 3, '', '', 'business:tax-refund:edit', 3, 1, 1, 0, NOW(), NOW()),
(3014, '申请删除', 301, 3, '', '', 'business:tax-refund:delete', 4, 1, 1, 0, NOW(), NOW()),
(3015, '申请提交', 301, 3, '', '', 'business:tax-refund:submit', 5, 1, 1, 0, NOW(), NOW()),
(3016, '详情查询', 301, 3, '', '', 'business:tax-refund:detail', 6, 1, 1, 0, NOW(), NOW());

-- 退税审核处理按钮 (302)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(3021, '列表查询', 302, 3, '', '', 'business:tax-refund:list:query', 1, 1, 1, 0, NOW(), NOW()),
(3022, '总览审核', 302, 3, '', '', 'business:tax-refund:approve', 2, 1, 1, 0, NOW(), NOW()),
(3023, '系统审核', 302, 3, '', '', 'system:tax-refund:approve', 3, 1, 1, 0, NOW(), NOW()),
(3024, '财务初审', 302, 3, '', '', 'system:tax-refund:first-review', 4, 1, 1, 0, NOW(), NOW()),
(3025, '财务复审', 302, 3, '', '', 'system:tax-refund:final-review', 5, 1, 1, 0, NOW(), NOW()),
(3026, '业务财务审核', 302, 3, '', '', 'business:tax-refund:finance', 6, 1, 1, 0, NOW(), NOW());

-- 工作流模块按钮 (401, 404, 405)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(4011, '流程部署', 401, 3, '', '', 'workflow:definition:deploy', 1, 1, 1, 0, NOW(), NOW()),
(4012, '流程更新', 401, 3, '', '', 'workflow:definition:update', 2, 1, 1, 0, NOW(), NOW()),
(4013, '流程查看', 401, 3, '', '', 'workflow:definition:view', 3, 1, 1, 0, NOW(), NOW()),
(4041, '流程实例终止', 404, 3, '', '', 'workflow:instance:terminate', 1, 1, 1, 0, NOW(), NOW()),
(4042, '实例启动', 404, 3, '', '', 'workflow:instance:start', 2, 1, 1, 0, NOW(), NOW()),
(4043, '实例暂停', 404, 3, '', '', 'workflow:instance:suspend', 3, 1, 1, 0, NOW(), NOW()),
(4044, '实例激活', 404, 3, '', '', 'workflow:instance:activate', 4, 1, 1, 0, NOW(), NOW()),
(4051, '任务驳回', 405, 3, '', '', 'workflow:task:reject', 1, 1, 1, 0, NOW(), NOW()),
(4052, '任务转派', 405, 3, '', '', 'workflow:task:transfer', 2, 1, 1, 0, NOW(), NOW()),
(4053, '任务列表', 405, 3, '', '', 'workflow:task:list', 3, 1, 1, 0, NOW(), NOW()),
(4054, '任务签收', 405, 3, '', '', 'workflow:task:claim', 4, 1, 1, 0, NOW(), NOW()),
(4055, '任务办理', 405, 3, '', '', 'workflow:task:complete', 5, 1, 1, 0, NOW(), NOW());

-- 合同模板 (501)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(5011, '模板查询', 501, 3, '', '', 'business:contract:template:query', 1, 1, 1, 0, NOW(), NOW()),
(5012, '模板新增', 501, 3, '', '', 'business:contract:template:add', 2, 1, 1, 0, NOW(), NOW()),
(5013, '模板编辑', 501, 3, '', '', 'business:contract:template:update', 3, 1, 1, 0, NOW(), NOW()),
(5014, '模板删除', 501, 3, '', '', 'business:contract:template:delete', 4, 1, 1, 0, NOW(), NOW()),
(5015, '模板上传', 501, 3, '', '', 'business:contract:template:upload', 5, 1, 1, 0, NOW(), NOW()),
(5016, '按模板生成合同', 501, 3, '', '', 'business:contract:generate', 6, 1, 1, 0, NOW(), NOW());

-- 合同列表按钮 (502)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(5021, '合同查询', 502, 3, '', '', 'business:contract:generation:query', 1, 1, 1, 0, NOW(), NOW()),
(5022, '合同下载', 502, 3, '', '', 'business:contract:download', 2, 1, 1, 0, NOW(), NOW()),
(5023, '合同删除', 502, 3, '', '', 'business:contract:generation:delete', 3, 1, 1, 0, NOW(), NOW());

-- 财务发票按钮 (601, 602, 603)
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(6011, '发票查询', 601, 3, '', '', 'financial:freight:query', 1, 1, 1, 0, NOW(), NOW()),
(6012, '发票新增', 601, 3, '', '', 'financial:freight:add', 2, 1, 1, 0, NOW(), NOW()),
(6013, '发票修改', 601, 3, '', '', 'financial:freight:edit', 3, 1, 1, 0, NOW(), NOW()),
(6014, '发票删除', 601, 3, '', '', 'financial:freight:delete', 4, 1, 1, 0, NOW(), NOW()),
(6015, '发票审核', 601, 3, '', '', 'financial:freight:audit', 5, 1, 1, 0, NOW(), NOW()),

(6021, '发票查询', 602, 3, '', '', 'financial:customs:query', 1, 1, 1, 0, NOW(), NOW()),
(6022, '发票新增', 602, 3, '', '', 'financial:customs:add', 2, 1, 1, 0, NOW(), NOW()),
(6023, '发票修改', 602, 3, '', '', 'financial:customs:edit', 3, 1, 1, 0, NOW(), NOW()),
(6024, '发票删除', 602, 3, '', '', 'financial:customs:delete', 4, 1, 1, 0, NOW(), NOW()),
(6025, '发票审核', 602, 3, '', '', 'financial:customs:audit', 5, 1, 1, 0, NOW(), NOW()),

(6031, '明细查询', 603, 3, '', '', 'financial:detail:query', 1, 1, 1, 0, NOW(), NOW()),
(6032, '明细新增', 603, 3, '', '', 'financial:detail:add', 2, 1, 1, 0, NOW(), NOW()),
(6033, '明细修改', 603, 3, '', '', 'financial:detail:edit', 3, 1, 1, 0, NOW(), NOW()),
(6034, '明细删除', 603, 3, '', '', 'financial:detail:delete', 4, 1, 1, 0, NOW(), NOW()),
(6035, '开票操作', 603, 3, '', '', 'financial:detail:invoice', 5, 1, 1, 0, NOW(), NOW());

-- ========================================
-- 第四步：管理员角色挂载数据
-- 设定 role_id = 1 是超级管理员或基础系统管理员
-- 必须拥有通过这版全尺寸生成的所有权限记录
-- ========================================
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW() FROM sys_menu;

SET FOREIGN_KEY_CHECKS = 1;

-- 校验统计
-- SELECT '操作完毕，包含新增、修改、导入所有子模块及财务模块底层精细化按钮级别的记录已全面打入' AS message;
-- SELECT menu_type as '类型(1目录/2视图/3按钮)', count(*) as '计数' from sys_menu group by menu_type;
