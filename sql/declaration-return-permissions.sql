-- 申报管理 (id=201) 增加退回草稿相关权限按钮
INSERT INTO sys_menu (id, menu_name, parent_id, menu_type, path, component, permission, sort, is_show, status, deleted, create_time, update_time) VALUES
(2026, '申请退回草稿', 201, 3, '', '', 'business:declaration:return-apply', 17, 1, 1, 0, NOW(), NOW()),
(2027, '退回申请审核', 201, 3, '', '', 'business:declaration:return-audit', 18, 1, 1, 0, NOW(), NOW());

-- 为超级管理员 (role_id=1) 自动分配新增权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time) VALUES 
(1, 2026, NOW()),
(1, 2027, NOW());
