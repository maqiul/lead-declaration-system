-- ============================================================
-- 申报资料「自定义资料项」操作权限
-- 控制范围：
--   1) 申报主表单资料区域「新增自定义资料项」按钮
--   2) 资料项行「更多操作」下拉菜单（编辑名称/说明、清除附件、删除资料项）
-- 前端 v-permission / checkPermission 引用：business:declaration:material:customize
-- ============================================================

INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`) VALUES
 (81012, '自定义资料项', 'business-declaration-material-customize', 202, 3, NULL, NULL, 'business:declaration:material:customize', NULL, 52, 0, 0, 1, 1, 0);

-- 默认授权给超级管理员角色（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
 (1, 81012);
