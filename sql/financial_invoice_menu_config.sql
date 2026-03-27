-- 财务票据管理菜单配置

-- 1. 财务票据管理父菜单
INSERT INTO sys_menu (menu_name, parent_id, sort, path, component, icon, permission, create_by, create_time, update_by, update_time, remark) 
VALUES ('财务票据管理', 0, 8, 'financial', NULL, 'money-collect', '', 1, NOW(), 1, NOW(), '财务票据管理目录');

-- 获取刚插入的菜单ID
SET @parentId = LAST_INSERT_ID();

-- 2. 货代发票子菜单
INSERT INTO sys_menu (menu_name, parent_id, sort, path, component, icon, permission, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票', @parentId, 1, 'freight', 'financial/freight/index', 'profile', 'financial:freight:list', 1, NOW(), 1, NOW(), '货代发票管理');

-- 3. 报关发票子菜单
INSERT INTO sys_menu (menu_name, parent_id, sort, path, component, icon, permission, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票', @parentId, 2, 'customs', 'financial/customs/index', 'file-search', 'financial:customs:list', 1, NOW(), 1, NOW(), '报关发票管理');

-- 4. 开票明细子菜单
INSERT INTO sys_menu (menu_name, parent_id, sort, path, component, icon, permission, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细', @parentId, 3, 'detail', 'financial/detail/index', 'unordered-list', 'financial:detail:list', 1, NOW(), 1, NOW(), '开票明细管理');

-- 获取各子菜单ID
SET @freightMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '货代发票');
SET @customsMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '报关发票');
SET @detailMenuId = (SELECT menu_id FROM sys_menu WHERE menu_name = '开票明细');

-- 5. 货代发票按钮权限
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票查询', @freightMenuId, 1, 'financial:freight:query', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票新增', @freightMenuId, 2, 'financial:freight:add', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票修改', @freightMenuId, 3, 'financial:freight:edit', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票删除', @freightMenuId, 4, 'financial:freight:delete', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('货代发票审核', @freightMenuId, 5, 'financial:freight:audit', '#', 1, NOW(), 1, NOW(), '');

-- 6. 报关发票按钮权限
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票查询', @customsMenuId, 1, 'financial:customs:query', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票新增', @customsMenuId, 2, 'financial:customs:add', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票修改', @customsMenuId, 3, 'financial:customs:edit', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票删除', @customsMenuId, 4, 'financial:customs:delete', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('报关发票审核', @customsMenuId, 5, 'financial:customs:audit', '#', 1, NOW(), 1, NOW(), '');

-- 7. 开票明细按钮权限
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细查询', @detailMenuId, 1, 'financial:detail:query', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细新增', @detailMenuId, 2, 'financial:detail:add', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细修改', @detailMenuId, 3, 'financial:detail:edit', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细删除', @detailMenuId, 4, 'financial:detail:delete', '#', 1, NOW(), 1, NOW(), '');
INSERT INTO sys_menu (menu_name, parent_id, sort, permission, icon, create_by, create_time, update_by, update_time, remark) 
VALUES ('开票明细开票', @detailMenuId, 5, 'financial:detail:invoice', '#', 1, NOW(), 1, NOW(), '');