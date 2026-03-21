-- 完整的系统权限配置脚本
-- 包含所有模块的菜单权限和按钮权限配置

-- 1. 清理重复的权限配置（如果需要）
-- DELETE FROM sys_menu WHERE menu_code LIKE 'business:declaration:%' AND menu_type = 3;

-- 2. 申报管理模块权限配置
-- 申报管理主菜单权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '申报管理', 'business_declaration', 1, '/business/declaration', 'Layout', '', 'FileTextOutlined', 3, 1);

-- 获取申报管理根菜单ID
SET @declaration_root_id = (SELECT id FROM sys_menu WHERE menu_code = 'business_declaration' AND menu_type = 1);

-- 申报管理子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@declaration_root_id, '申报列表', 'business_declaration_list', 2, 'list', 'declaration/manage/index', 'business:declaration:list', 'UnorderedListOutlined', 1, 1),
(@declaration_root_id, '申报统计', 'business_declaration_stats', 2, 'statistics', 'declaration/statistics/index', 'business:declaration:statistics', 'BarChartOutlined', 2, 1),
(@declaration_root_id, '申报审核', 'business_declaration_audit', 2, 'audit', 'declaration/audit/index', 'business:declaration:audit', 'AuditOutlined', 3, 1);

-- 申报列表操作权限（按钮级别）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '查询申报', 'business_declaration_query', 3, '', '', 'business:declaration:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '新增申报', 'business_declaration_add', 3, '', '', 'business:declaration:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '编辑申报', 'business_declaration_update', 3, '', '', 'business:declaration:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '删除申报', 'business_declaration_delete', 3, '', '', 'business:declaration:delete', '', 4, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '提交申报', 'business_declaration_submit', 3, '', '', 'business:declaration:submit', '', 5, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '导出申报', 'business_declaration_export', 3, '', '', 'business:declaration:export', '', 6, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '下载单证', 'business_declaration_download', 3, '', '', 'business:declaration:download', '', 7, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '生成合同', 'business_declaration_contract', 3, '', '', 'business:declaration:contract', '', 8, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '付款管理', 'business_declaration_payment', 3, '', '', 'business:declaration:payment', '', 9, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '提货单提交', 'business_declaration_pickup_submit', 3, '', '', 'business:declaration:pickup-submit', '', 10, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '提货单审核', 'business_declaration_pickup_audit', 3, '', '', 'business:declaration:pickup-audit', '', 11, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_declaration_list'), '提货单删除', 'business_declaration_pickup_delete', 3, '', '', 'business:declaration:pickup-delete', '', 12, 1);

-- 3. 税务退费模块权限配置
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '税务退费', 'business_tax_refund', 1, '/business/tax-refund', 'Layout', '', 'DollarOutlined', 4, 1);

-- 获取税务退费根菜单ID
SET @tax_refund_root_id = (SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund' AND menu_type = 1);

-- 税务退费子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@tax_refund_root_id, '退费申请', 'business_tax_refund_apply', 2, 'apply', 'tax-refund/apply/index', 'business:tax-refund:apply', 'FormOutlined', 1, 1),
(@tax_refund_root_id, '退费列表', 'business_tax_refund_list', 2, 'list', 'tax-refund/list/index', 'business:tax-refund:list', 'UnorderedListOutlined', 2, 1),
(@tax_refund_root_id, '财务审核', 'business_tax_refund_finance', 2, 'finance-review', 'tax-refund/finance-review/index', 'business:tax-refund:finance', 'AuditOutlined', 3, 1);

-- 税务退费操作权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_apply'), '申请查询', 'business_tax_refund_apply_query', 3, '', '', 'business:tax-refund:apply:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_apply'), '申请新增', 'business_tax_refund_apply_add', 3, '', '', 'business:tax-refund:apply:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_apply'), '申请编辑', 'business_tax_refund_apply_update', 3, '', '', 'business:tax-refund:apply:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_apply'), '申请删除', 'business_tax_refund_apply_delete', 3, '', '', 'business:tax-refund:apply:delete', '', 4, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), '列表查询', 'business_tax_refund_list_query', 3, '', '', 'business:tax-refund:list:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_tax_refund_list'), '审核操作', 'business_tax_refund_approve', 3, '', '', 'business:tax-refund:approve', '', 2, 1);

-- 4. 系统管理扩展权限
-- 银行账户管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), '银行账户查询', 'system_bank_account_query', 3, '', '', 'system:bank-account:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), '银行账户新增', 'system_bank_account_add', 3, '', '', 'system:bank-account:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), '银行账户编辑', 'system_bank_account_update', 3, '', '', 'system:bank-account:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_bank_account'), '银行账户删除', 'system_bank_account_delete', 3, '', '', 'system:bank-account:delete', '', 4, 1);

-- 国家信息管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'system_country'), '国家信息查询', 'system_country_query', 3, '', '', 'system:country:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_country'), '国家信息新增', 'system_country_add', 3, '', '', 'system:country:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_country'), '国家信息编辑', 'system_country_update', 3, '', '', 'system:country:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'system_country'), '国家信息删除', 'system_country_delete', 3, '', '', 'system:country:delete', '', 4, 1);

-- HS商品类型管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'product_maintenance'), '商品查询', 'system_product_query', 3, '', '', 'system:product:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'product_maintenance'), '商品新增', 'system_product_add', 3, '', '', 'system:product:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'product_maintenance'), '商品编辑', 'system_product_update', 3, '', '', 'system:product:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'product_maintenance'), '商品删除', 'system_product_delete', 3, '', '', 'system:product:delete', '', 4, 1);

-- 5. 合同管理权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(0, '合同管理', 'business_contract', 1, '/business/contract', 'Layout', '', 'FileTextOutlined', 5, 1);

-- 获取合同管理根菜单ID
SET @contract_root_id = (SELECT id FROM sys_menu WHERE menu_code = 'business_contract' AND menu_type = 1);

-- 合同管理子菜单
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
(@contract_root_id, '模板管理', 'business_contract_template', 2, 'template', 'contract/template/index', 'business:contract:template', 'ProfileOutlined', 1, 1),
(@contract_root_id, '合同生成', 'business_contract_generation', 2, 'generation', 'contract/generation/index', 'business:contract:generation', 'FileDoneOutlined', 2, 1);

-- 合同操作权限
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_template'), '模板查询', 'business_contract_template_query', 3, '', '', 'business:contract:template:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_template'), '模板新增', 'business_contract_template_add', 3, '', '', 'business:contract:template:add', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_template'), '模板编辑', 'business_contract_template_update', 3, '', '', 'business:contract:template:update', '', 3, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_template'), '模板删除', 'business_contract_template_delete', 3, '', '', 'business:contract:template:delete', '', 4, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_template'), '模板上传', 'business_contract_template_upload', 3, '', '', 'business:contract:template:upload', '', 5, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_generation'), '合同查询', 'business_contract_generation_query', 3, '', '', 'business:contract:generation:query', '', 1, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_generation'), '合同下载', 'business_contract_download', 3, '', '', 'business:contract:download', '', 2, 1),
((SELECT id FROM sys_menu WHERE menu_code = 'business_contract_generation'), '合同删除', 'business_contract_generation_delete', 3, '', '', 'business:contract:generation:delete', '', 3, 1);

-- 6. 为管理员角色分配所有权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT 1, id, NOW()
FROM sys_menu 
WHERE menu_code LIKE 'business_%'
   OR menu_code LIKE 'system_%'
   OR parent_id IN (
       SELECT id FROM sys_menu WHERE menu_code LIKE 'business_%' OR menu_code LIKE 'system_%'
   )
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 7. 为普通用户角色分配基础权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT (SELECT id FROM sys_role WHERE role_code = 'USER'), id, NOW()
FROM sys_menu 
WHERE (menu_code LIKE 'business_declaration_list' 
       OR menu_code LIKE 'business_tax_refund_apply')
   AND menu_type IN (2, 3)
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 8. 为财务角色分配相关权限
INSERT INTO sys_role_menu (role_id, menu_id, create_time)
SELECT (SELECT id FROM sys_role WHERE role_code = 'FINANCE'), id, NOW()
FROM sys_menu 
WHERE menu_code LIKE 'business_tax_refund_%'
   OR menu_code LIKE 'business_declaration_payment'
   OR menu_code LIKE 'business_declaration_audit'
ON DUPLICATE KEY UPDATE create_time = NOW();

-- 9. 验证权限配置结果
SELECT 
    '权限配置验证' as section,
    r.role_name,
    COUNT(rm.menu_id) as permission_count,
    GROUP_CONCAT(
        CASE 
            WHEN m.menu_type = 1 THEN CONCAT('[目录]', m.menu_name)
            WHEN m.menu_type = 2 THEN CONCAT('[菜单]', m.menu_name)
            WHEN m.menu_type = 3 THEN CONCAT('[按钮]', m.menu_name)
        END 
        ORDER BY m.menu_type, m.sort 
        SEPARATOR ', '
    ) as permissions
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.id = rm.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.id
WHERE r.role_code IN ('ADMIN', 'USER', 'FINANCE')
GROUP BY r.id, r.role_name
ORDER BY r.id;

-- 10. 显示详细的菜单结构
SELECT 
    CONCAT(REPEAT('  ', CASE WHEN m.menu_type = 1 THEN 0 WHEN m.menu_type = 2 THEN 1 ELSE 2 END), m.menu_name) as menu_structure,
    m.menu_code,
    CASE m.menu_type 
        WHEN 1 THEN '目录'
        WHEN 2 THEN '菜单' 
        WHEN 3 THEN '按钮'
    END as menu_type,
    m.permission,
    m.path,
    m.component
FROM sys_menu m
WHERE m.menu_code LIKE 'business_%' OR m.menu_code LIKE 'system_%'
ORDER BY m.menu_type, m.sort;