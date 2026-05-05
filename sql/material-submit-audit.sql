-- ============================================================
-- 申报资料提交/审核 节点配套 SQL
-- 1. 建表：资料项模板（全局）+ 资料项实例（每单）
-- 2. 初始化 4 条默认模板
-- 3. 菜单权限（资料项模板配置 + 申报模块按钮权限）
-- ============================================================

-- ---------- 1. 资料项模板表 ----------
DROP TABLE IF EXISTS `declaration_material_template`;
CREATE TABLE `declaration_material_template` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `code`        VARCHAR(64)  NOT NULL COMMENT '资料编码（唯一）',
    `name`        VARCHAR(100) NOT NULL COMMENT '资料显示名',
    `required`    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否必填 0-否 1-是',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '说明',
    `form_schema` TEXT         DEFAULT NULL COMMENT '结构化字段配置 JSON（如发票金额/发票号等）',
    `enabled`     TINYINT      NOT NULL DEFAULT 1 COMMENT '启用 0-停用 1-启用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报资料项模板';

-- ---------- 2. 资料项实例表（每申报单一组） ----------
DROP TABLE IF EXISTS `declaration_material_item`;
CREATE TABLE `declaration_material_item` (
    `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `form_id`      BIGINT        NOT NULL COMMENT '申报单ID',
    `template_id`  BIGINT        DEFAULT NULL COMMENT '来源模板ID（NULL=单据内手动新增）',
    `code`         VARCHAR(64)   DEFAULT NULL COMMENT '资料编码',
    `name`         VARCHAR(100)  NOT NULL COMMENT '资料显示名',
    `required`     TINYINT       NOT NULL DEFAULT 1 COMMENT '是否必填',
    `sort`         INT           NOT NULL DEFAULT 0 COMMENT '排序',
    `remark`       VARCHAR(500)  DEFAULT NULL COMMENT '说明',
    `form_schema`  TEXT          DEFAULT NULL COMMENT '结构化字段配置（从模板克隆可单据内覆盖）',
    `file_name`    VARCHAR(255)  DEFAULT NULL COMMENT '附件名',
    `file_url`     VARCHAR(500)  DEFAULT NULL COMMENT '附件下载地址',
    `upload_by`    BIGINT        DEFAULT NULL COMMENT '上传人ID',
    `upload_time`  DATETIME      DEFAULT NULL COMMENT '上传时间',
    `amount`       DECIMAL(18,4) DEFAULT NULL COMMENT '金额（发票类使用）',
    `currency`     VARCHAR(10)   DEFAULT NULL COMMENT '币种',
    `invoice_no`   VARCHAR(100)  DEFAULT NULL COMMENT '发票号',
    `invoice_date` DATE          DEFAULT NULL COMMENT '开票日期',
    `extra_data`   TEXT          DEFAULT NULL COMMENT '其他扩展字段 JSON',
    `status`       TINYINT       NOT NULL DEFAULT 0 COMMENT '状态 0-未上传 1-已上传',
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_form_id` (`form_id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报资料项实例';

-- ---------- 3. 初始化默认模板 ----------
-- 发票类 form_schema 预设：金额/发票号/开票日期 都必填（货代/报关代理发票按人民币结算，不需币种）
SET @INVOICE_SCHEMA := '[{"key":"amount","label":"发票金额","type":"number","required":true},{"key":"invoiceNo","label":"发票号","type":"text","required":true},{"key":"invoiceDate","label":"开票日期","type":"date","required":true}]';

INSERT INTO `declaration_material_template` (`code`, `name`, `required`, `sort`, `remark`, `form_schema`, `enabled`) VALUES
 ('CONTRACT',               '合同',         1, 10, '商务合同扫描件',       NULL,            1),
 ('PACKING_LIST',           '装箱单',       1, 20, '装箱单扫描件',         NULL,            1),
 ('FREIGHT_INVOICE',        '货代发票',     1, 30, '货代公司开具发票',     @INVOICE_SCHEMA, 1),
 ('CUSTOMS_AGENT_INVOICE',  '报关代理发票', 1, 40, '报关代理公司开具发票', @INVOICE_SCHEMA, 1);

-- ---------- 4. 菜单：系统管理 -> 资料项模板 ----------
-- 父菜单为 100 系统管理。占用 ID 8100 段，避免与现有占用冲突
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`)
VALUES
 (8100, '资料项模板', 'system-material-template', 100, 2, 'material-template', '@/views/system/material-template/index.vue', NULL, 'FileProtectOutlined', 20, 0, 0, 1, 1, 0);

-- 按钮权限（系统管理 - 资料项模板）
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`) VALUES
 (81001, '查看模板', 'system-material-template-view',   8100, 3, NULL, NULL, 'system:material:template:view',   NULL, 1, 0, 0, 1, 1, 0),
 (81002, '新增模板', 'system-material-template-add',    8100, 3, NULL, NULL, 'system:material:template:add',    NULL, 2, 0, 0, 1, 1, 0),
 (81003, '编辑模板', 'system-material-template-edit',   8100, 3, NULL, NULL, 'system:material:template:edit',   NULL, 3, 0, 0, 1, 1, 0),
 (81004, '删除模板', 'system-material-template-delete', 8100, 3, NULL, NULL, 'system:material:template:delete', NULL, 4, 0, 0, 1, 1, 0);

-- ---------- 5. 申报管理按钮权限（资料提交/资料审核） ----------
-- 父菜单 ID 为申报管理页面菜单 ID（查询结果为 200 段，如与实际不符请调整 parent_id）
-- 在此仅插入权限按钮，前端 v-permission 引用
INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`) VALUES
 (81010, '提交资料', 'business-declaration-material-submit', 202, 3, NULL, NULL, 'business:declaration:material:submit', NULL, 50, 0, 0, 1, 1, 0),
 (81011, '资料审核', 'business-declaration-audit-material',  202, 3, NULL, NULL, 'business:declaration:audit:material',  NULL, 51, 0, 0, 1, 1, 0);

-- ---------- 6. 默认授权给超级管理员角色（role_id=1） ----------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
 (1, 8100), (1, 81001), (1, 81002), (1, 81003), (1, 81004),
 (1, 81010), (1, 81011);

-- ---------- 7. 存量数据迁移：旧 status=2（"已完成"）→ 4 ----------
-- 旧语义 2=已完成，新语义 2=待资料提交/3=待资料审核/4=已完成
-- 如果你希望历史单据进入新流程重审，可注释掉这条；默认一次性迁移到 4
UPDATE `declaration_form` SET `status` = 4 WHERE `status` = 2;
