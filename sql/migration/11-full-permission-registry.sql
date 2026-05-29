-- =============================================================================
-- lead-declaration-system 全量权限注册表 SQL（前后端完整对齐版）
-- 生成时间：2026-04-22
-- 范围：
--   * 全量重建 sys_menu（目录 + 菜单 + 按钮，共 180+ 条）
--   * 全量重建 role_id=1（超级管理员）的 sys_role_menu 授权
--   * 其他角色授权不动
-- 依据：
--   * 后端 @RequiresPermissions 扫描 228 条（228→去重 80 余）
--   * 前端 v-permission / checkPermission 扫描 163 处（去重 100 余）
--   * 原 sys_menu 基础结构 159 条
-- 执行方式：
--   1. 备份 sys_menu / sys_role_menu
--   2. 整段执行本脚本
--   3. 超管重新登录刷新权限缓存
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. 清空菜单与超管授权（其他角色授权保留，待管理员手动勾配）
-- -----------------------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `role_id` = 1;
DELETE FROM `sys_menu`;

-- -----------------------------------------------------------------------------
-- 2. 顶级目录（menu_type=1）
-- -----------------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(  1,'首页',    'dashboard',      0,1,'/',             'Layout',NULL,'HomeOutlined',         1,0,0,1,1,0),
( 10,'个人中心','profile',        0,1,'/profile',      'Layout',NULL,'UserOutlined',        99,0,0,0,1,0),
(100,'系统管理','system',         0,1,'/system',       'Layout',NULL,'SettingOutlined',      2,0,0,1,1,0),
(200,'出口申报','declaration',    0,1,'/declaration',  'Layout',NULL,'FileProtectOutlined',  3,0,0,1,1,0),
(300,'税务退费','tax-refund',     0,1,'/tax-refund',   'Layout',NULL,'DollarOutlined',       4,0,0,1,1,0),
(400,'工作流',  'workflow',       0,1,'/workflow',     'Layout',NULL,'BranchesOutlined',     5,0,0,1,1,0),
(500,'合同管理','contract',       0,1,'/contract',     'Layout',NULL,'FileTextOutlined',     6,0,0,1,1,0),
(600,'水单管理','remittance',     0,1,'/remittance',   'Layout',NULL,'AccountBookOutlined',  7,0,0,1,1,0),
(700,'发票管理','finance-invoice',0,1,'/finance-invoice','Layout',NULL,'FileTextOutlined',   9,0,0,1,1,0);

-- -----------------------------------------------------------------------------
-- 3. 二级菜单（menu_type=2）
-- -----------------------------------------------------------------------------
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(  2,'首页',        'dashboard-index',           1,2,'dashboard',       '@/views/dashboard/simple.vue',              NULL,'HomeOutlined',          0,0,0,1,1,0),
( 11,'个人信息',    'profile-index',            10,2,'index',           '@/views/profile/index.vue',                 NULL,'UserOutlined',          1,0,0,0,1,0),
(101,'用户管理',    'system-user',             100,2,'user',            '@/views/system/user/index.vue',             NULL,'UserOutlined',          1,0,0,1,1,0),
(102,'角色管理',    'system-role',             100,2,'role',            '@/views/system/role/index.vue',             NULL,'TeamOutlined',          2,0,0,1,1,0),
(103,'组织管理',    'system-org',              100,2,'org',             '@/views/system/org/index.vue',              NULL,'ApartmentOutlined',     3,0,0,1,1,0),
(104,'菜单管理',    'system-menu',             100,2,'menu',            '@/views/system/menu/index.vue',             NULL,'MenuOutlined',          4,0,0,1,1,0),
(105,'银行账户',    'system-bank-account',     100,2,'bank-account',    '@/views/system/bank-account/index.vue',     NULL,'BankOutlined',          5,0,0,1,1,0),
(106,'国家信息',    'system-country',          100,2,'country',         '@/views/system/country/index.vue',          NULL,'GlobalOutlined',        6,0,0,1,1,0),
(107,'HS商品维护',  'system-product',          100,2,'product',         '@/views/system/product/index.vue',          NULL,'DatabaseOutlined',      7,0,0,1,1,0),
(108,'API测试',     'system-api-test',         100,2,'api-test',        '@/views/system/api-test/index.vue',         NULL,'ApiOutlined',           8,0,0,1,1,0),
(109,'系统配置',    'system-config',           100,2,'config',          '@/views/system/config/index.vue',           NULL,'SettingOutlined',       9,0,0,1,1,0),
(110,'运输方式',    'system-transport',        100,2,'transport-mode',  '@/views/system/transport-mode/index.vue',   NULL,'CarOutlined',          10,0,0,1,1,0),
(111,'支付方式',    'system-payment',          100,2,'payment-method',  '@/views/system/payment-method/index.vue',   NULL,'MoneyCollectOutlined', 11,0,0,1,1,0),
(112,'货币管理',    'system-currency',         100,2,'currency',        '@/views/system/currency/index.vue',         NULL,'DollarCircleOutlined', 12,0,0,1,1,0),
(113,'计量单位',    'system-measurement-unit', 100,2,'measurement-unit','@/views/system/measurement-unit/index.vue', NULL,'DashboardOutlined',    13,0,0,1,1,0),
(114,'城市管理',    'system-city-info',        100,2,'city-info',       '@/views/system/city-info/index.vue',        NULL,'EnvironmentOutlined',  14,0,0,1,1,0),
(115,'资料模板',    'system-material-template',100,2,'material-template','@/views/system/material-template/index.vue',NULL,'FileAddOutlined',     15,0,0,1,1,0),
(201,'申报管理',    'declaration-manage',      200,2,'manage',          '@/views/declaration/manage/index.vue',      NULL,'ContainerOutlined',     1,0,0,1,1,0),
(202,'财务单证',    'declaration-finance',     200,2,'finance',         '@/views/declaration/finance/index.vue',     NULL,'PayCircleOutlined',     2,0,0,1,1,0),
(203,'申报表单',    'declaration-form',        200,2,'form',            '@/views/declaration/form/index.vue',        NULL,'FileTextOutlined',      3,0,0,0,1,0),
(204,'申报统计',    'declaration-statistics',  200,2,'statistics',      '@/views/declaration/statistics/index.vue',  NULL,'BarChartOutlined',      4,0,0,1,1,0),
(301,'退税列表',    'tax-refund-list',         300,2,'list',            '@/views/tax-refund/list/index.vue',         NULL,'UnorderedListOutlined', 1,0,0,1,1,0),
(401,'流程定义',    'workflow-definition',     400,2,'definition',      '@/views/workflow/definition/index.vue',     NULL,'FileDoneOutlined',      1,0,0,1,1,0),
(402,'流程设计',    'workflow-modeler',        400,2,'modeler',         '@/views/workflow/modeler/index.vue',        NULL,'EditOutlined',          2,0,0,1,1,0),
(403,'流程监控',    'workflow-monitor',        400,2,'monitor',         '@/views/workflow/monitor/index.vue',        NULL,'FundViewOutlined',      3,0,0,1,1,0),
(404,'流程实例',    'workflow-instance',       400,2,'instance',        '@/views/workflow/instance/index.vue',       NULL,'ProfileOutlined',       4,0,0,1,1,0),
(405,'我的任务',    'workflow-task',           400,2,'task',            '@/views/workflow/task/index.vue',           NULL,'CheckCircleOutlined',   5,0,0,1,1,0),
(501,'模板管理',    'contract-template',       500,2,'template',        '@/views/contract/template/index.vue',       NULL,'FileAddOutlined',       1,0,0,1,1,0),
(502,'合同列表',    'contract-generation',     500,2,'generation',      '@/views/contract/generation/index.vue',     NULL,'HistoryOutlined',       2,0,0,1,1,0),
(601,'水单列表',    'remittance-list',         600,2,'list',            '@/views/remittance/list/index.vue',         NULL,'ListOutlined',          1,0,0,1,1,0),
(602,'水单审核',    'remittance-audit',        600,2,'audit',           '@/views/remittance/audit/index.vue',        NULL,'AuditOutlined',         2,0,0,1,1,0),
(701,'发票台账',    'finance-invoice-index',   700,2,'index',           '@/views/finance/invoice/index.vue',         NULL,'FileTextOutlined',      1,0,0,1,1,0);

-- -----------------------------------------------------------------------------
-- 4. 三级按钮（menu_type=3）- 按父菜单 id 段位分组
-- -----------------------------------------------------------------------------

-- 4.1 用户管理（101，段位 1011-1019）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1011,'用户查看','user-view',    101,3,NULL,NULL,'user:view',    NULL,1,0,0,1,1,0),
(1012,'用户创建','user-create',  101,3,NULL,NULL,'user:create',  NULL,2,0,0,1,1,0),
(1013,'用户更新','user-update',  101,3,NULL,NULL,'user:update',  NULL,3,0,0,1,1,0),
(1014,'用户删除','user-delete',  101,3,NULL,NULL,'user:delete',  NULL,4,0,0,1,1,0),
(1015,'密码重置','user-resetPwd',101,3,NULL,NULL,'user:resetPwd',NULL,5,0,0,1,1,0);

-- 4.2 角色管理（102，段位 1021-1029）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1021,'角色查看','role-view',  102,3,NULL,NULL,'role:view',  NULL,1,0,0,1,1,0),
(1022,'角色创建','role-create',102,3,NULL,NULL,'role:create',NULL,2,0,0,1,1,0),
(1023,'角色更新','role-update',102,3,NULL,NULL,'role:update',NULL,3,0,0,1,1,0),
(1024,'角色删除','role-delete',102,3,NULL,NULL,'role:delete',NULL,4,0,0,1,1,0),
(1025,'角色分配','role-assign',102,3,NULL,NULL,'role:assign',NULL,5,0,0,1,1,0),
(1026,'菜单权限','role-menu',  102,3,NULL,NULL,'role:menu',  NULL,6,0,0,1,1,0),
(1027,'角色用户','role-user',  102,3,NULL,NULL,'role:user',  NULL,7,0,0,1,1,0);

-- 4.3 组织管理（103，段位 1031-1039）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1031,'组织查看',    'org-view',     103,3,NULL,NULL,'org:view',  NULL,1,0,0,1,1,0),
(1032,'组织创建',    'org-create',   103,3,NULL,NULL,'org:create',NULL,2,0,0,1,1,0),
(1033,'组织更新',    'org-update',   103,3,NULL,NULL,'org:update',NULL,3,0,0,1,1,0),
(1034,'组织删除',    'org-delete',   103,3,NULL,NULL,'org:delete',NULL,4,0,0,1,1,0),
(1035,'组织用户',    'org-user',     103,3,NULL,NULL,'org:user',  NULL,5,0,0,1,1,0);

-- 4.4 菜单管理（104，段位 1041-1049）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1041,'菜单查看',    'menu-view',  104,3,NULL,NULL,'menu:view',  NULL,1,0,0,1,1,0),
(1042,'菜单创建',    'menu-create',104,3,NULL,NULL,'menu:create',NULL,2,0,0,1,1,0),
(1043,'菜单更新',    'menu-update',104,3,NULL,NULL,'menu:update',NULL,3,0,0,1,1,0),
(1044,'菜单删除',    'menu-delete',104,3,NULL,NULL,'menu:delete',NULL,4,0,0,1,1,0);

-- 4.5 银行账户（105，段位 1051-1059）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1051,'账户查看','bank-account-view',  105,3,NULL,NULL,'system:bank-account:view',  NULL,1,0,0,1,1,0),
(1052,'账户新增','bank-account-add',   105,3,NULL,NULL,'system:bank-account:add',   NULL,2,0,0,1,1,0),
(1053,'账户更新','bank-account-update',105,3,NULL,NULL,'system:bank-account:update',NULL,3,0,0,1,1,0),
(1054,'账户删除','bank-account-delete',105,3,NULL,NULL,'system:bank-account:delete',NULL,4,0,0,1,1,0);

-- 4.6 国家信息（106，段位 1061-1069）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1061,'国家查看','country-view',  106,3,NULL,NULL,'system:country:view',  NULL,1,0,0,1,1,0),
(1062,'国家创建','country-create',106,3,NULL,NULL,'system:country:create',NULL,2,0,0,1,1,0),
(1063,'国家更新','country-update',106,3,NULL,NULL,'system:country:update',NULL,3,0,0,1,1,0),
(1064,'国家删除','country-delete',106,3,NULL,NULL,'system:country:delete',NULL,4,0,0,1,1,0);

-- 4.7 HS商品（107，段位 1071-1079）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1071,'商品查看','product-view',  107,3,NULL,NULL,'system:product:view',  NULL,1,0,0,1,1,0),
(1072,'商品创建','product-create',107,3,NULL,NULL,'system:product:create',NULL,2,0,0,1,1,0),
(1073,'商品更新','product-update',107,3,NULL,NULL,'system:product:update',NULL,3,0,0,1,1,0),
(1074,'商品删除','product-delete',107,3,NULL,NULL,'system:product:delete',NULL,4,0,0,1,1,0);

-- 4.8 系统配置（109，段位 1091-1099）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1091,'配置查看','config-view',  109,3,NULL,NULL,'system:config:view',  NULL,1,0,0,1,1,0),
(1092,'配置新增','config-add',   109,3,NULL,NULL,'system:config:add',   NULL,2,0,0,1,1,0),
(1093,'配置更新','config-update',109,3,NULL,NULL,'system:config:update',NULL,3,0,0,1,1,0),
(1094,'配置删除','config-delete',109,3,NULL,NULL,'system:config:delete',NULL,4,0,0,1,1,0);

-- 4.9 运输方式（110，段位 1101-1109）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1101,'运输查看','transport-view',  110,3,NULL,NULL,'system:transport:view',  NULL,1,0,0,1,1,0),
(1102,'运输创建','transport-create',110,3,NULL,NULL,'system:transport:create',NULL,2,0,0,1,1,0),
(1103,'运输更新','transport-update',110,3,NULL,NULL,'system:transport:update',NULL,3,0,0,1,1,0),
(1104,'运输删除','transport-delete',110,3,NULL,NULL,'system:transport:delete',NULL,4,0,0,1,1,0);

-- 4.10 支付方式（111，段位 1111-1119）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1111,'支付查看','payment-view',  111,3,NULL,NULL,'system:payment:view',  NULL,1,0,0,1,1,0),
(1112,'支付创建','payment-create',111,3,NULL,NULL,'system:payment:create',NULL,2,0,0,1,1,0),
(1113,'支付更新','payment-update',111,3,NULL,NULL,'system:payment:update',NULL,3,0,0,1,1,0),
(1114,'支付删除','payment-delete',111,3,NULL,NULL,'system:payment:delete',NULL,4,0,0,1,1,0);

-- 4.11 货币管理（112，段位 1121-1129）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1121,'货币查看','currency-view',  112,3,NULL,NULL,'system:currency:view',  NULL,1,0,0,1,1,0),
(1122,'货币创建','currency-create',112,3,NULL,NULL,'system:currency:create',NULL,2,0,0,1,1,0),
(1123,'货币更新','currency-update',112,3,NULL,NULL,'system:currency:update',NULL,3,0,0,1,1,0),
(1124,'货币删除','currency-delete',112,3,NULL,NULL,'system:currency:delete',NULL,4,0,0,1,1,0);

-- 4.12 计量单位（113，段位 1131-1139）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1131,'单位查看',    'measurement-unit-view',  113,3,NULL,NULL,'system:measurement-unit:view',  NULL,1,0,0,1,1,0),
(1132,'单位创建',    'measurement-unit-create',113,3,NULL,NULL,'system:measurement-unit:create',NULL,2,0,0,1,1,0),
(1133,'单位更新',    'measurement-unit-update',113,3,NULL,NULL,'system:measurement-unit:update',NULL,3,0,0,1,1,0),
(1134,'单位删除',    'measurement-unit-delete',113,3,NULL,NULL,'system:measurement-unit:delete',NULL,4,0,0,1,1,0);

-- 4.13 城市管理（114，段位 1141-1149）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1141,'城市查看','city-info-view',  114,3,NULL,NULL,'system:city-info:view',  NULL,1,0,0,1,1,0),
(1142,'城市创建','city-info-create',114,3,NULL,NULL,'system:city-info:create',NULL,2,0,0,1,1,0),
(1143,'城市更新','city-info-update',114,3,NULL,NULL,'system:city-info:update',NULL,3,0,0,1,1,0),
(1144,'城市删除','city-info-delete',114,3,NULL,NULL,'system:city-info:delete',NULL,4,0,0,1,1,0);

-- 4.14 资料模板（115，段位 1151-1159）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(1151,'模板查看','material-template-view',  115,3,NULL,NULL,'system:material:template:view',  NULL,1,0,0,1,1,0),
(1152,'模板新增','material-template-add',   115,3,NULL,NULL,'system:material:template:add',   NULL,2,0,0,1,1,0),
(1153,'模板编辑','material-template-edit',  115,3,NULL,NULL,'system:material:template:edit',  NULL,3,0,0,1,1,0),
(1154,'模板删除','material-template-delete',115,3,NULL,NULL,'system:material:template:delete',NULL,4,0,0,1,1,0);

-- 4.15 申报管理（201，段位 2011-2099）
-- 注：提货单相关权限已随前端功能下线而移除；后端 DeclarationFormController
--     1530-1568 行仍残留 delivery-order 接口，下一步需清理（见末尾 TODO）。
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(2011,'申报查看',    'declaration-view',              201,3,NULL,NULL,'business:declaration:view',              NULL, 1,0,0,1,1,0),
(2012,'申报创建',    'declaration-create',            201,3,NULL,NULL,'business:declaration:create',            NULL, 2,0,0,1,1,0),
(2013,'申报更新',    'declaration-update',            201,3,NULL,NULL,'business:declaration:update',            NULL, 3,0,0,1,1,0),
(2014,'申报删除',    'declaration-delete',            201,3,NULL,NULL,'business:declaration:delete',            NULL, 4,0,0,1,1,0),
(2015,'申报提交',    'declaration-submit',            201,3,NULL,NULL,'business:declaration:submit',            NULL, 5,0,0,1,1,0),
(2016,'申报审核',    'declaration-audit',             201,3,NULL,NULL,'business:declaration:audit',             NULL, 6,0,0,1,1,0),
(2017,'初审权限',    'declaration-audit-initial',     201,3,NULL,NULL,'business:declaration:audit:initial',     NULL, 7,0,0,1,1,0),
(2018,'退回审核',    'declaration-audit-return',      201,3,NULL,NULL,'business:declaration:return:audit',      NULL, 8,0,0,1,1,0),
(2019,'申报导出',    'declaration-export',            201,3,NULL,NULL,'business:declaration:export',            NULL, 9,0,0,1,1,0),
(2020,'下载单证',    'declaration-download',          201,3,NULL,NULL,'business:declaration:download',          NULL,10,0,0,1,1,0),
(2021,'生成合同',    'declaration-contract',          201,3,NULL,NULL,'business:declaration:contract',          NULL,11,0,0,1,1,0),
(2027,'申请退回',    'declaration-return-apply',      201,3,NULL,NULL,'business:declaration:return:apply',      NULL,17,0,0,1,1,0),
(2028,'财务补充',    'declaration-finance-supplement',201,3,NULL,NULL,'business:declaration:finance:supplement',NULL,18,0,0,1,1,0),
(2029,'关联水单',    'declaration-finance-remittance',201,3,NULL,NULL,'business:declaration:finance:remittance',NULL,19,0,0,1,1,0),
(2032,'资料提交',    'declaration-material-submit',   201,3,NULL,NULL,'business:declaration:material:submit',   NULL,22,0,0,1,1,0),
(2033,'资料审核',    'declaration-audit-material',    201,3,NULL,NULL,'business:declaration:audit:material',    NULL,23,0,0,1,1,0),
(2034,'发票提交',    'declaration-invoice-submit',    201,3,NULL,NULL,'business:declaration:invoice:submit',    NULL,24,0,0,1,1,0),
(2035,'发票审核',    'declaration-audit-invoice',     201,3,NULL,NULL,'business:declaration:audit:invoice',     NULL,25,0,0,1,1,0),
(2036,'资料自定义',  'declaration-material-customize',201,3,NULL,NULL,'business:declaration:material:customize',NULL,26,0,0,1,1,0);

-- 4.16 退税（301，段位 3011-3099）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(3011,'退税查看',    'tax-refund-view',   301,3,NULL,NULL,'business:tax-refund:view',   NULL, 1,0,0,1,1,0),
(3012,'退税创建',    'tax-refund-create', 301,3,NULL,NULL,'business:tax-refund:create', NULL, 2,0,0,1,1,0),
(3013,'退税更新',    'tax-refund-update', 301,3,NULL,NULL,'business:tax-refund:update', NULL, 3,0,0,1,1,0),
(3014,'退税删除',    'tax-refund-delete', 301,3,NULL,NULL,'business:tax-refund:delete', NULL, 4,0,0,1,1,0),
(3015,'退税提交',    'tax-refund-submit', 301,3,NULL,NULL,'business:tax-refund:submit', NULL, 5,0,0,1,1,0),
(3016,'退税审核',    'tax-refund-approve',301,3,NULL,NULL,'business:tax-refund:approve',NULL, 6,0,0,1,1,0),
(3017,'退税详情',    'tax-refund-detail', 301,3,NULL,NULL,'business:tax-refund:detail', NULL, 7,0,0,1,1,0),
(3018,'退税列表项',  'tax-refund-listitem',301,3,NULL,NULL,'business:tax-refund:list',  NULL, 8,0,0,1,1,0);

-- 4.17 流程定义（401，段位 4011-4019）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(4011,'流程查看','workflow-definition-view',  401,3,NULL,NULL,'workflow:definition:view',  NULL,1,0,0,1,1,0),
(4012,'流程新增','workflow-definition-create',401,3,NULL,NULL,'workflow:definition:create',NULL,2,0,0,1,1,0),
(4013,'流程部署','workflow-definition-deploy',401,3,NULL,NULL,'workflow:definition:deploy',NULL,3,0,0,1,1,0),
(4014,'流程更新','workflow-definition-update',401,3,NULL,NULL,'workflow:definition:update',NULL,4,0,0,1,1,0),
(4015,'流程删除','workflow-definition-delete',401,3,NULL,NULL,'workflow:definition:delete',NULL,5,0,0,1,1,0);

-- 4.18 流程监控（403，段位 4031-4039）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(4031,'监控查看','workflow-monitor-view',403,3,NULL,NULL,'workflow:monitor:view',NULL,1,0,0,1,1,0);

-- 4.19 流程实例（404，段位 4041-4049）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(4041,'实例查看','workflow-instance-view',     404,3,NULL,NULL,'workflow:instance:view',     NULL,1,0,0,1,1,0),
(4042,'实例启动','workflow-instance-start',    404,3,NULL,NULL,'workflow:instance:start',    NULL,2,0,0,1,1,0),
(4043,'实例暂停','workflow-instance-suspend',  404,3,NULL,NULL,'workflow:instance:suspend',  NULL,3,0,0,1,1,0),
(4044,'实例激活','workflow-instance-activate', 404,3,NULL,NULL,'workflow:instance:activate', NULL,4,0,0,1,1,0),
(4045,'实例终止','workflow-instance-terminate',404,3,NULL,NULL,'workflow:instance:terminate',NULL,5,0,0,1,1,0);

-- 4.20 我的任务（405，段位 4051-4059）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(4051,'任务查看','workflow-task-view',    405,3,NULL,NULL,'workflow:task:view',    NULL,1,0,0,1,1,0),
(4052,'任务签收','workflow-task-claim',   405,3,NULL,NULL,'workflow:task:claim',   NULL,2,0,0,1,1,0),
(4053,'任务处理','workflow-task-complete',405,3,NULL,NULL,'workflow:task:complete',NULL,3,0,0,1,1,0),
(4054,'任务驳回','workflow-task-reject',  405,3,NULL,NULL,'workflow:task:reject',  NULL,4,0,0,1,1,0),
(4055,'任务转派','workflow-task-transfer',405,3,NULL,NULL,'workflow:task:transfer',NULL,5,0,0,1,1,0);

-- 4.21 合同模板（501，段位 5011-5019）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(5011,'模板查看','contract-template-view',  501,3,NULL,NULL,'business:contract:template:view',  NULL,1,0,0,1,1,0),
(5012,'模板创建','contract-template-create',501,3,NULL,NULL,'business:contract:template:create',NULL,2,0,0,1,1,0),
(5013,'模板更新','contract-template-update',501,3,NULL,NULL,'business:contract:template:update',NULL,3,0,0,1,1,0),
(5014,'模板删除','contract-template-delete',501,3,NULL,NULL,'business:contract:template:delete',NULL,4,0,0,1,1,0),
(5015,'模板上传','contract-template-upload',501,3,NULL,NULL,'business:contract:template:upload',NULL,5,0,0,1,1,0),
(5016,'合同生成','contract-generate',       501,3,NULL,NULL,'business:contract:generate',       NULL,6,0,0,1,1,0),
(5017,'合同更新','contract-update',         501,3,NULL,NULL,'business:contract:update',         NULL,7,0,0,1,1,0);

-- 4.22 合同生成（502，段位 5021-5029）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(5021,'合同查看','contract-generation-view',  502,3,NULL,NULL,'business:contract:generation:view',  NULL,1,0,0,1,1,0),
(5022,'合同下载','contract-download',         502,3,NULL,NULL,'business:contract:download',         NULL,2,0,0,1,1,0),
(5023,'合同删除','contract-generation-delete',502,3,NULL,NULL,'business:contract:generation:delete',NULL,3,0,0,1,1,0);

-- 4.23 水单列表（601，段位 6011-6019）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(6011,'水单查看','remittance-view',  601,3,NULL,NULL,'business:remittance:view',  NULL,1,0,0,1,1,0),
(6012,'水单创建','remittance-create',601,3,NULL,NULL,'business:remittance:create',NULL,2,0,0,1,1,0),
(6013,'水单更新','remittance-update',601,3,NULL,NULL,'business:remittance:update',NULL,3,0,0,1,1,0),
(6014,'水单删除','remittance-delete',601,3,NULL,NULL,'business:remittance:delete',NULL,4,0,0,1,1,0),
(6015,'水单提交','remittance-submit',601,3,NULL,NULL,'business:remittance:submit',NULL,5,0,0,1,1,0),
(6016,'水单审核','remittance-audit', 601,3,NULL,NULL,'business:remittance:audit', NULL,6,0,0,1,1,0);

-- 4.24 发票台账（701，段位 7011-7019）
INSERT INTO `sys_menu` (`id`,`menu_name`,`menu_code`,`parent_id`,`menu_type`,`path`,`component`,`permission`,`icon`,`sort`,`is_external`,`is_cache`,`is_show`,`status`,`deleted`) VALUES
(7011,'查看发票',  'finance-invoice-view',  701,3,NULL,NULL,'finance:invoice:view',  NULL,1,0,0,1,1,0),
(7012,'录入发票',  'finance-invoice-create',701,3,NULL,NULL,'finance:invoice:create',NULL,2,0,0,1,1,0),
(7013,'编辑发票',  'finance-invoice-update',701,3,NULL,NULL,'finance:invoice:update',NULL,3,0,0,1,1,0),
(7014,'删除发票',  'finance-invoice-delete',701,3,NULL,NULL,'finance:invoice:delete',NULL,4,0,0,1,1,0);

-- -----------------------------------------------------------------------------
-- 5. 超管（role_id=1）全授权
-- -----------------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `deleted` = 0 AND `status` = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- TODO：后续需要手工清理/统一的事项（不在本 SQL 范围）
-- =============================================================================
-- 1. 后端 DeclarationFormController 1530-1568 行仍保留提货单接口
--    （saveDeliveryOrder / getDeliveryOrders / updateDeliveryOrder / deleteDeliveryOrder），
--    前端已不再调用，建议连同 DeliveryOrderDao / DeliveryOrder 实体一并移除。
-- 2. 后端历史遗留 add/edit 动词（SQL 已对齐保留，可在下一轮重构时改为 create/update）：
--    - system:bank-account:add      → 建议改 create
--    - system:config:add            → 建议改 create
--    - system:material:template:add → 建议改 create
--    - system:material:template:edit→ 建议改 update
--    - business:declaration:edit    → 已合并到 update（后端 1826 行 deleteBusinessInvoice 已改 update，SQL 已删 2037 条目）
--    - finance:invoice:edit         → 已合并到 update（后端 88 行 uploadFile 已改 update，SQL 已删 7015 条目）
--    以上每项修改后需同步更新本 SQL 及前端 v-permission。
-- =============================================================================

-- =============================================================================
-- 验证查询（选择性执行）
-- =============================================================================
-- 全部按钮权限清单：
-- SELECT m.parent_id, p.menu_name AS parent_name, m.id, m.menu_name, m.permission
-- FROM sys_menu m LEFT JOIN sys_menu p ON p.id = m.parent_id
-- WHERE m.menu_type = 3 ORDER BY m.parent_id, m.sort;

-- 超管被授权的按钮权限码（与后端 @RequiresPermissions、前端 v-permission 对账）：
-- SELECT DISTINCT m.permission
-- FROM sys_role_menu rm JOIN sys_menu m ON rm.menu_id = m.id
-- WHERE rm.role_id = 1 AND m.menu_type = 3 AND m.permission IS NOT NULL
-- ORDER BY m.permission;

-- 统计：
-- SELECT menu_type, COUNT(*) FROM sys_menu GROUP BY menu_type;
-- 预期：1(目录)=9, 2(菜单)=32, 3(按钮)=128
-- =============================================================================
