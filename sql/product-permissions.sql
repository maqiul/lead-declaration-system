-- 插入HS商品维护菜单权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `sort`, `status`) VALUES
(0, 'HS商品维护', 'product_maintenance', 1, '/system/product', '', 'system:product:list', 11, 1),
(110, '商品列表', 'product_list', 2, '/system/product/list', 'system/product/index', 'system:product:list', 1, 1),
(110, '新增商品', 'product_add', 2, '/system/product/add', '', 'system:product:add', 2, 1),
(110, '编辑商品', 'product_edit', 2, '/system/product/edit', '', 'system:product:update', 3, 1),
(110, '删除商品', 'product_delete', 2, '/system/product/delete', '', 'system:product:delete', 4, 1),
(110, '查看详情', 'product_query', 2, '/system/product/query', '', 'system:product:query', 5, 1);
