-- =============================================================
-- 55-menu-tidy.sql 菜单整理
-- 1. 顶部一级菜单：业务优先，系统管理靠后
-- 2. 系统管理内部：按 权限/系统 → 业务配置 → 基础数据 → 流程 → 开发工具 重排（保持平铺）
-- 3. 贸易方式 path/component 格式与其他菜单统一
-- 4. 内外部申报子菜单：补充资料审核移到紧随补充资料（按业务流程排序）
-- 5. 申报权限按钮 sort 重号（27 重复）→ 按现序连续重编号
-- 6. 清理 sys_role_menu 中指向已不存在菜单的孤儿绑定
-- =============================================================

-- 1. 顶部一级菜单顺序
UPDATE sys_menu SET sort = 1  WHERE id = 1;    -- 首页
UPDATE sys_menu SET sort = 2  WHERE id = 900;  -- 梓熠、理德申报
UPDATE sys_menu SET sort = 3  WHERE id = 901;  -- 集洛申报
UPDATE sys_menu SET sort = 4  WHERE id = 500;  -- 合同管理
UPDATE sys_menu SET sort = 5  WHERE id = 600;  -- 水单管理
UPDATE sys_menu SET sort = 6  WHERE id = 800;  -- 出款水单管理
UPDATE sys_menu SET sort = 7  WHERE id = 700;  -- 发票管理
UPDATE sys_menu SET sort = 8  WHERE id = 1200; -- 常用客户
UPDATE sys_menu SET sort = 9  WHERE id = 400;  -- 工作流
UPDATE sys_menu SET sort = 10 WHERE id = 100;  -- 系统管理

-- 2. 系统管理内部排序（平铺，按类别聚拢）
-- 权限与系统
UPDATE sys_menu SET sort = 1  WHERE id = 101;  -- 用户管理
UPDATE sys_menu SET sort = 2  WHERE id = 102;  -- 角色管理
UPDATE sys_menu SET sort = 3  WHERE id = 103;  -- 组织管理
UPDATE sys_menu SET sort = 4  WHERE id = 104;  -- 菜单管理
UPDATE sys_menu SET sort = 5  WHERE id = 109;  -- 系统配置
UPDATE sys_menu SET sort = 6  WHERE id = 2070; -- 字典管理
-- 业务配置
UPDATE sys_menu SET sort = 7  WHERE id = 105;  -- 银行账户
UPDATE sys_menu SET sort = 8  WHERE id = 116;  -- 主体配置
UPDATE sys_menu SET sort = 9  WHERE id = 115;  -- 资料模板
UPDATE sys_menu SET sort = 10 WHERE id = 107;  -- HS商品维护
-- 基础数据
UPDATE sys_menu SET sort = 11 WHERE id = 106;  -- 国家信息
UPDATE sys_menu SET sort = 12 WHERE id = 114;  -- 城市管理
UPDATE sys_menu SET sort = 13 WHERE id = 110;  -- 运输方式
UPDATE sys_menu SET sort = 14 WHERE id = 81076;-- 贸易方式
UPDATE sys_menu SET sort = 15 WHERE id = 111;  -- 支付方式
UPDATE sys_menu SET sort = 16 WHERE id = 112;  -- 货币管理
UPDATE sys_menu SET sort = 17 WHERE id = 113;  -- 计量单位
-- 流程配置
UPDATE sys_menu SET sort = 18 WHERE id = 2050; -- 流程模板
UPDATE sys_menu SET sort = 19 WHERE id = 2060; -- 流程节点
-- 开发工具垫底
UPDATE sys_menu SET sort = 20 WHERE id = 108;  -- API测试

-- 3. 贸易方式：path/component 与兄弟菜单统一（相对路径 + @/views 前缀）
UPDATE sys_menu SET path = 'trade-term', component = '@/views/system/trade-term/index.vue' WHERE id = 81076;

-- 4. 申报子菜单按业务流程排序（补充资料审核紧随补充资料）
-- 内部申报（900）
UPDATE sys_menu SET sort = 1  WHERE id = 902;  -- 申报录入
UPDATE sys_menu SET sort = 2  WHERE id = 903;  -- 资料提交
UPDATE sys_menu SET sort = 3  WHERE id = 904;  -- 补充资料
UPDATE sys_menu SET sort = 4  WHERE id = 922;  -- 补充资料审核
UPDATE sys_menu SET sort = 5  WHERE id = 905;  -- 开票金额
UPDATE sys_menu SET sort = 6  WHERE id = 906;  -- 发票提交
UPDATE sys_menu SET sort = 7  WHERE id = 907;  -- 归档查询
UPDATE sys_menu SET sort = 8  WHERE id = 908;  -- 财务单证
UPDATE sys_menu SET sort = 9  WHERE id = 909;  -- 申报管理
UPDATE sys_menu SET sort = 10 WHERE id = 910;  -- 申报统计
UPDATE sys_menu SET sort = 11 WHERE id = 911;  -- 申报表单（隐藏）
-- 外部申报（901）
UPDATE sys_menu SET sort = 1  WHERE id = 912;  -- 申报录入
UPDATE sys_menu SET sort = 2  WHERE id = 913;  -- 资料提交
UPDATE sys_menu SET sort = 3  WHERE id = 914;  -- 补充资料
UPDATE sys_menu SET sort = 4  WHERE id = 923;  -- 补充资料审核
UPDATE sys_menu SET sort = 5  WHERE id = 915;  -- 开票金额
UPDATE sys_menu SET sort = 6  WHERE id = 916;  -- 发票提交
UPDATE sys_menu SET sort = 7  WHERE id = 917;  -- 归档查询
UPDATE sys_menu SET sort = 8  WHERE id = 918;  -- 财务单证
UPDATE sys_menu SET sort = 9  WHERE id = 919;  -- 申报管理
UPDATE sys_menu SET sort = 10 WHERE id = 920;  -- 申报统计
UPDATE sys_menu SET sort = 11 WHERE id = 921;  -- 申报表单（隐藏）

-- 5. 申报权限按钮 sort 按现序连续重编号（消除 27 重复）
UPDATE sys_menu m
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY sort, id) AS rn
    FROM sys_menu
    WHERE parent_id = 909 AND menu_type = 3 AND deleted = 0
) t ON m.id = t.id
SET m.sort = t.rn;

UPDATE sys_menu m
JOIN (
    SELECT id, ROW_NUMBER() OVER (ORDER BY sort, id) AS rn
    FROM sys_menu
    WHERE parent_id = 919 AND menu_type = 3 AND deleted = 0
) t ON m.id = t.id
SET m.sort = t.rn;

-- 6. 清理指向已不存在菜单的角色绑定
DELETE FROM sys_role_menu WHERE menu_id NOT IN (SELECT id FROM sys_menu);
