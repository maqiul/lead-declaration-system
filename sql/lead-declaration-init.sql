-- ============================================================
-- Lead Declaration System - 完整数据库初始化脚本
-- 生成时间: 2026-03-20
-- 从运行数据库导出
-- MySQL版本要求: 8.0+
-- 字符集: utf8mb4_general_ci
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `lead_declaration` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lead_declaration`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、系统基础表
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE COMMENT '用户名唯一索引',
  KEY `idx_org_id` (`org_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统用户表';

-- 2. 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '角色描述',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据权限范围 1-全部 2-本级 3-本级及下级 4-自定义',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_code` (`role_code`) USING BTREE COMMENT '角色编码唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色表';

-- 3. 菜单权限表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜单编码',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `menu_type` tinyint NOT NULL DEFAULT '1' COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由地址',
  `component` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图标',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `is_external` tinyint NOT NULL DEFAULT '0' COMMENT '是否外链 0-否 1-是',
  `is_cache` tinyint NOT NULL DEFAULT '0' COMMENT '是否缓存 0-否 1-是',
  `is_show` tinyint NOT NULL DEFAULT '1' COMMENT '是否显示 0-隐藏 1-显示',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_permission` (`permission`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';

-- 4. 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`) USING BTREE COMMENT '用户角色唯一索引',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户角色关联表';

-- 5. 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`) USING BTREE COMMENT '角色菜单唯一索引',
  KEY `idx_role_id` (`role_id`) USING BTREE,
  KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色菜单关联表';

-- 6. 组织机构表
CREATE TABLE IF NOT EXISTS `sys_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `org_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织名称',
  `org_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织编码',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `level` int NOT NULL DEFAULT '1' COMMENT '组织层级',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `leader` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_org_code` (`org_code`) USING BTREE COMMENT '组织编码唯一索引',
  KEY `idx_parent_id` (`parent_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='组织机构表';

-- 7. 用户组织关联表
CREATE TABLE IF NOT EXISTS `sys_user_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '组织ID',
  `is_main` tinyint NOT NULL DEFAULT '0' COMMENT '是否主组织 0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_org` (`user_id`,`org_id`) USING BTREE COMMENT '用户组织唯一索引',
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_org_id` (`org_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户组织关联表';

-- ============================================================
-- 二、业务表
-- ============================================================

-- 1. 申报单主表
CREATE TABLE IF NOT EXISTS `declaration_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申报单号',
  `shipper_company` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发货人公司名',
  `shipper_address` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '发货人地址',
  `consignee_company` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人公司名',
  `consignee_address` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '收货人地址',
  `invoice_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发票号',
  `declaration_date` date NOT NULL COMMENT '申报日期',
  `transport_mode` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '运输方式',
  `departure_city` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '出发城市',
  `destination_country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '目的地区域',
  `destination_country_code` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目的国代码',
  `destination_country_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '目的国名称',
  `currency` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '币种',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总数量',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `total_cartons` int NOT NULL DEFAULT '0' COMMENT '总箱数',
  `total_gross_weight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总毛重(KGS)',
  `total_net_weight` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '总净重(KGS)',
  `total_volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '总体积(CBM)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-草稿 1-已提交 2-已审核 3-已完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_no` (`form_no`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`),
  KEY `idx_dest_country_code` (`destination_country_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出口申报单主表';

-- 2. 申报单产品明细表
CREATE TABLE IF NOT EXISTS `declaration_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `product_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '产品名称',
  `hs_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HS编码',
  `quantity` int NOT NULL COMMENT '数量',
  `unit` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位',
  `unit_price` decimal(15,4) NOT NULL COMMENT '单价',
  `amount` decimal(15,2) NOT NULL COMMENT '金额',
  `gross_weight` decimal(10,2) NOT NULL COMMENT '毛重(KGS)',
  `net_weight` decimal(10,2) NOT NULL COMMENT '净重(KGS)',
  `cartons` int NOT NULL DEFAULT '1' COMMENT '箱数',
  `volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '体积(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `image_id` bigint DEFAULT NULL COMMENT '产品图片ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_hs_code` (`hs_code`),
  CONSTRAINT `fk_declaration_product_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报单产品明细表';

-- 3. 申报单附件表
CREATE TABLE IF NOT EXISTS `declaration_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint DEFAULT NULL COMMENT '申报单ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件下载路径',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报单附件表';

-- 4. 水单信息表
CREATE TABLE IF NOT EXISTS `declaration_remittance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `remittance_type` tinyint NOT NULL DEFAULT '1' COMMENT '水单类型: 1-定金, 2-尾款',
  `remittance_name` varchar(100) NOT NULL COMMENT '收汇名称',
  `remittance_date` date NOT NULL COMMENT '收汇日期',
  `remittance_amount` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '收汇金额($)',
  `exchange_rate` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '当日汇率',
  `bank_fee` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '手续费($)',
  `credited_amount` decimal(18,4) NOT NULL DEFAULT '0.0000' COMMENT '入账金额($)',
  `remarks` text COMMENT '备注',
  `photo_url` varchar(500) DEFAULT NULL COMMENT '水单照片URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='水单信息表';

-- 5. 申报单草稿箱表
CREATE TABLE IF NOT EXISTS `declaration_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `form_no` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申报单号',
  `shipper_company` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发货人',
  `consignee_company` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '收货人',
  `total_amount` decimal(15,2) DEFAULT '0.00' COMMENT '总金额',
  `form_data` longtext COLLATE utf8mb4_general_ci NOT NULL COMMENT '整个表单的JSON数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_org` (`user_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报单草稿箱表';

-- 6. 申报单箱子信息表
CREATE TABLE IF NOT EXISTS `declaration_carton` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `carton_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '箱号',
  `quantity` int NOT NULL COMMENT '箱子数量',
  `volume` decimal(10,3) NOT NULL COMMENT '单箱体积(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_carton_no` (`carton_no`),
  CONSTRAINT `fk_declaration_carton_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报单箱子信息表';

-- 7. 箱子产品关联表
CREATE TABLE IF NOT EXISTS `declaration_carton_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `carton_id` bigint NOT NULL COMMENT '箱子ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `quantity` int NOT NULL COMMENT '该箱中产品数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_carton_id` (`carton_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_carton_product_carton` FOREIGN KEY (`carton_id`) REFERENCES `declaration_carton` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_carton_product_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='箱子产品关联表';

-- 8. 申报要素填写记录表
CREATE TABLE IF NOT EXISTS `declaration_element_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `element_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '要素名称',
  `element_value` text COLLATE utf8mb4_general_ci COMMENT '要素值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_declaration_element_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报要素填写记录表';

-- 9. 申报历史记录表
CREATE TABLE IF NOT EXISTS `declaration_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `declaration_id` bigint NOT NULL COMMENT '申报数据ID',
  `version` int NOT NULL COMMENT '版本号',
  `hs_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HS编码',
  `english_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名称',
  `chinese_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名称',
  `declaration_elements` json DEFAULT NULL COMMENT '申报要素JSON数据',
  `created_by` bigint NOT NULL COMMENT '创建人',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_declaration_id` (`declaration_id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_history_version` (`declaration_id`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='申报历史记录表';

-- 10. 商品申报数据表
CREATE TABLE IF NOT EXISTS `product_declaration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `serial_number` int NOT NULL COMMENT '序号',
  `hs_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HS编码',
  `english_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名称',
  `chinese_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名称',
  `declaration_elements` json DEFAULT NULL COMMENT '申报要素JSON数据',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-草稿 1-已提交 2-已审核',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_hs_code` (`hs_code`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_product_decl_hs_status` (`hs_code`,`status`),
  KEY `idx_product_decl_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品申报数据表';

-- ============================================================
-- 三、配置表
-- ============================================================

-- 1. 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `config_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置名称',
  `config_value` text COLLATE utf8mb4_general_ci COMMENT '配置值',
  `input_type` tinyint DEFAULT '1' COMMENT '配置输入类型 1-文本框 2-下拉框 3-开关 4-数字输入框',
  `select_options` json DEFAULT NULL COMMENT '下拉框选项JSON',
  `is_system_param` tinyint DEFAULT '0' COMMENT '是否系统内置参数 0-否 1-是',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '配置类型 1-系统配置 2-UI配置 3-业务配置',
  `config_group` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置分组',
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统配置表';

-- 2. 银行账户配置表
CREATE TABLE IF NOT EXISTS `bank_account_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '账户名称',
  `bank_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '银行名称',
  `bank_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '银行代码',
  `account_number` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '银行账号',
  `swift_code` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'SWIFT代码',
  `iban` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'IBAN号码',
  `account_holder` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '账户持有人',
  `currency` varchar(10) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USD' COMMENT '账户币种',
  `branch_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支行名称',
  `branch_address` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '支行地址',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认账户 0-否 1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `remarks` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_bank_name` (`bank_name`),
  KEY `idx_account_holder` (`account_holder`),
  KEY `idx_currency` (`currency`),
  KEY `idx_is_default` (`is_default`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行账户配置表';

-- 3. 国家信息表
CREATE TABLE IF NOT EXISTS `country_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `country_code` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家代码(ISO 3166-1 alpha-3)',
  `chinese_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名称',
  `english_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名称',
  `abbreviation` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '简称/缩写',
  `continent` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '所属洲',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_code` (`country_code`),
  KEY `idx_abbreviation` (`abbreviation`),
  KEY `idx_chinese_name` (`chinese_name`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_continent` (`continent`),
  KEY `idx_status_del` (`status`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='国家信息表';

-- 4. 计量单位配置表
CREATE TABLE IF NOT EXISTS `measurement_units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `unit_code` varchar(10) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位代码',
  `unit_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位名称',
  `unit_name_en` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '英文单位名称',
  `unit_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '单位类型',
  `description` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '单位描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  KEY `idx_unit_type` (`unit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='计量单位配置表';

-- 5. HS商品类型配置表
CREATE TABLE IF NOT EXISTS `product_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hs_code` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'HS编码',
  `english_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '英文名称',
  `chinese_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名称',
  `elements_config` json DEFAULT NULL COMMENT '申报要素配置JSON',
  `unit_type` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位类型',
  `unit_code` varchar(10) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位代码',
  `unit_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '计量单位名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_status` (`status`),
  KEY `uk_hs_code` (`hs_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='HS商品类型配置表';

-- ============================================================
-- 四、退税相关表
-- ============================================================

-- 1. 退税申请表
CREATE TABLE IF NOT EXISTS `tax_refund_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请编号',
  `declaration_form_id` bigint NOT NULL COMMENT '关联申报单ID',
  `initiator_id` bigint NOT NULL COMMENT '申请人ID',
  `initiator_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请人姓名',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `department_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `department_name` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '申请部门名称',
  `application_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请类型',
  `amount` decimal(15,2) NOT NULL COMMENT '申请金额',
  `invoice_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '发票号码',
  `invoice_amount` decimal(15,2) NOT NULL COMMENT '发票金额',
  `tax_rate` decimal(5,2) NOT NULL COMMENT '税率(%)',
  `expected_refund_amount` decimal(15,2) GENERATED ALWAYS AS (((`invoice_amount` * `tax_rate`) / 100)) STORED COMMENT '预计退税金额',
  `description` text COLLATE utf8mb4_general_ci COMMENT '申请说明',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态(0:草稿,1:已提交,2:财务初审,3:文件生成,4:退回补充,5:发票提交,6:财务复审,7:已完成,8:已拒绝)',
  `current_approver_id` bigint DEFAULT NULL COMMENT '当前审批人ID',
  `current_approver_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前审批人姓名',
  `reject_reason` text COLLATE utf8mb4_general_ci COMMENT '拒绝原因',
  `return_reason` text COLLATE utf8mb4_general_ci COMMENT '退回补充原因',
  `first_review_opinion` text COLLATE utf8mb4_general_ci COMMENT '初审意见',
  `final_review_opinion` text COLLATE utf8mb4_general_ci COMMENT '复审意见',
  `file_path` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成的退税文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识(0:未删除,1:已删除)',
  `first_reviewer_id` bigint DEFAULT NULL COMMENT '初审人ID',
  `first_reviewer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '初审人名称',
  `first_review_time` datetime DEFAULT NULL COMMENT '初审时间',
  `final_reviewer_id` bigint DEFAULT NULL COMMENT '复审人ID',
  `final_reviewer_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '复审人名称',
  `final_review_time` datetime DEFAULT NULL COMMENT '复审时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_tax_refund_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退税申请表';

-- 2. 退税申请附件表
CREATE TABLE IF NOT EXISTS `tax_refund_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件名',
  `file_path` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件类型',
  `attachment_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '附件类型',
  `description` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '附件描述',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `uploader_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '上传人姓名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退税申请附件表';

-- 3. 退税流程节点表
CREATE TABLE IF NOT EXISTS `tax_refund_process_node` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `node_type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点类型',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '操作人姓名',
  `operation_result` varchar(20) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '操作结果',
  `remark` text COLLATE utf8mb4_general_ci COMMENT '备注',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退税流程节点表';

-- 4. 退税审核记录表
CREATE TABLE IF NOT EXISTS `tax_refund_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '退税申请ID',
  `review_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审核类型',
  `reviewer_id` bigint NOT NULL COMMENT '审核人ID',
  `reviewer_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审核人姓名',
  `review_result` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT '审核结果',
  `review_opinion` text COLLATE utf8mb4_general_ci COMMENT '审核意见',
  `review_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `next_approver_id` bigint DEFAULT NULL COMMENT '下一审批人ID',
  `next_approver_name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '下一审批人姓名',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_review_time` (`review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退税审核记录表';

-- ============================================================
-- 五、合同相关表
-- ============================================================

-- 1. 合同模板表
CREATE TABLE IF NOT EXISTS `contract_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(200) NOT NULL COMMENT '模板名称',
  `template_code` varchar(100) DEFAULT NULL COMMENT '模板编码',
  `template_type` varchar(50) DEFAULT 'EXPORT' COMMENT '模板类型',
  `file_name` varchar(300) DEFAULT NULL COMMENT '文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小',
  `description` text COMMENT '描述',
  `STATUS` int DEFAULT '1' COMMENT '0禁用1启用',
  `sort` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `del_flag` int DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同模板表';

-- 2. 合同生成记录表
CREATE TABLE IF NOT EXISTS `contract_generation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `contract_no` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '合同编号',
  `declaration_form_id` bigint NOT NULL COMMENT '关联的申报单ID',
  `template_id` bigint NOT NULL COMMENT '使用的模板ID',
  `generated_file_name` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '生成的文件名',
  `generated_file_path` varchar(500) COLLATE utf8mb4_general_ci NOT NULL COMMENT '生成的文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `generated_by` bigint NOT NULL COMMENT '生成人',
  `generated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-已删除 1-正常',
  `remarks` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_declaration_form_id` (`declaration_form_id`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_generated_by` (`generated_by`),
  KEY `idx_generated_time` (`generated_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='合同生成记录表';

-- ============================================================
-- 六、工作流相关表
-- ============================================================

-- 1. 流程定义表
CREATE TABLE IF NOT EXISTS `wf_process_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程描述',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程分类',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `bpmn_xml` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '流程XML内容',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_process_key_version` (`process_key`,`version`) USING BTREE COMMENT '流程KEY和版本唯一索引',
  KEY `idx_process_key` (`process_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程定义表';

-- 2. 流程实例表
CREATE TABLE IF NOT EXISTS `wf_process_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程实例ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义ID',
  `process_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义KEY',
  `process_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '流程名称',
  `business_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务KEY',
  `starter_id` bigint DEFAULT NULL COMMENT '发起人ID',
  `starter_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发起人姓名',
  `current_activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前节点ID',
  `current_activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前节点名称',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '流程状态 0-运行中 1-已完成 2-已终止 3-已挂起',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_instance_id` (`instance_id`) USING BTREE COMMENT '流程实例ID唯一索引',
  KEY `idx_definition_id` (`definition_id`) USING BTREE,
  KEY `idx_process_key` (`process_key`) USING BTREE,
  KEY `idx_business_key` (`business_key`) USING BTREE,
  KEY `idx_starter_id` (`starter_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='流程实例表';

-- 3. 任务实例表
CREATE TABLE IF NOT EXISTS `wf_task_instance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务描述',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程实例ID',
  `definition_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '流程定义ID',
  `activity_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '节点ID',
  `activity_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点名称',
  `assignee_id` bigint DEFAULT NULL COMMENT '办理人ID',
  `assignee_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '办理人姓名',
  `candidate_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '候选人IDs',
  `candidate_group_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '候选组IDs',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态 0-待办 1-已办 2-已撤回 3-已终止',
  `priority` int NOT NULL DEFAULT '50' COMMENT '优先级',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `claim_time` datetime DEFAULT NULL COMMENT '签收时间',
  `end_time` datetime DEFAULT NULL COMMENT '完成时间',
  `due_time` datetime DEFAULT NULL COMMENT '到期时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '删除标识 0-未删除 1-已删除',
  `create_time_db` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据库创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_task_id` (`task_id`) USING BTREE COMMENT '任务ID唯一索引',
  KEY `idx_instance_id` (`instance_id`) USING BTREE,
  KEY `idx_definition_id` (`definition_id`) USING BTREE,
  KEY `idx_activity_id` (`activity_id`) USING BTREE,
  KEY `idx_assignee_id` (`assignee_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='任务实例表';

-- 4. 业务流程定义扩展记录表
CREATE TABLE IF NOT EXISTS `process_definition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) DEFAULT NULL COMMENT '流程定义名称',
  `process_key` varchar(100) NOT NULL COMMENT '流程定义KEY',
  `process_name` varchar(100) DEFAULT NULL COMMENT '流程名称',
  `bpmn_xml` longtext COMMENT '流程XML内容',
  `version` int DEFAULT '1' COMMENT '流程定义版本',
  `deployment_id` varchar(64) DEFAULT NULL COMMENT '部署ID',
  `description` varchar(500) DEFAULT NULL COMMENT '流程定义描述',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:停用 1:启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务流程定义扩展记录表';

-- ============================================================
-- 七、初始化数据
-- ============================================================

-- 7.1 默认管理员用户
-- 注意: password为MD5加密后的值, admin用户密码为admin
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `org_id`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '管理员', '13800138000', 'admin@example.com', NULL, 1, 1, 0, NOW(), NOW()),
(2, 'Finance01', '250dfb30df927257fe9401c0c89c0134', 'Finance01', '', '', NULL, 4, 1, 0, NOW(), NOW()),
(3, 'user1', '8a13a81b63c9f02d897e8b39dd21372f', 'user1', '', '', NULL, 5, 1, 0, NOW(), NOW());

-- 7.2 角色
INSERT IGNORE INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `data_scope`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(1, '超级管理员', 'ADMIN', '系统超级管理员，拥有所有权限', 1, 1, 0, NOW(), NOW()),
(4, '财务', 'FINANCE', '财务权限,用于审核', 1, 1, 0, NOW(), NOW()),
(5, '业务员', 'SALESPERSON', '', 1, 1, 0, NOW(), NOW());

-- 7.3 组织机构
INSERT IGNORE INTO `sys_org` (`id`, `org_name`, `org_code`, `parent_id`, `level`, `sort`, `leader`, `phone`, `email`, `status`, `deleted`, `create_time`, `update_time`) VALUES
(1, '系统', 'SYSTEM', 0, 1, 1, '张三', '13800138000', 'company@example.com', 1, 0, NOW(), NOW()),
(2, '宁波梓熠科技有限公司', 'NINGBO ZIYI', 1, 2, 1, '李四', '13800138001', 'tech@example.com', 1, 0, NOW(), NOW()),
(3, '杭州集洛', 'HANGZHOU JILUO', 1, 2, 2, '王五', '13800138002', 'sales@example.com', 1, 0, NOW(), NOW()),
(4, '财务部', 'ZIYI FINANCE', 2, 3, 0, '李四', '13800138001', 'tech@example.com', 1, 0, NOW(), NOW()),
(5, '业务部', 'BUSINESS', 3, 3, 0, '', '', '', 1, 0, NOW(), NOW());

-- 7.4 用户角色关联
INSERT IGNORE INTO `sys_user_role` (`id`, `user_id`, `role_id`, `create_time`) VALUES
(1, 1, 1, NOW()),
(3, 2, 4, NOW()),
(4, 3, 5, NOW());

-- 7.5 用户组织关联
INSERT IGNORE INTO `sys_user_org` (`id`, `user_id`, `org_id`, `is_main`, `create_time`) VALUES
(1, 1, 1, 1, NOW());

-- 7.6 菜单权限（完整菜单树）
INSERT IGNORE INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `parent_id`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `is_external`, `is_cache`, `is_show`, `status`, `deleted`) VALUES
-- 一级菜单
(1, '首页', 'dashboard', 0, 1, '/', 'Layout', NULL, 'HomeOutlined', 1, 0, 0, 0, 1, 0),
(2, '首页', 'dashboard-index', 0, 2, '/dashboard', '@/views/dashboard/simple.vue', NULL, 'HomeOutlined', 0, 0, 0, 1, 1, 0),
(3, '个人中心', 'profile', 0, 1, '/profile', 'Layout', NULL, 'UserOutlined', 99, 0, 0, 0, 1, 0),
(100, '系统管理', 'system', 0, 1, '/system', 'Layout', NULL, 'SettingOutlined', 2, 0, 0, 1, 1, 0),
(200, '出口申报', 'declaration', 0, 1, '/declaration', 'Layout', NULL, 'FileProtectOutlined', 3, 0, 0, 1, 1, 0),
(300, '税务退费', 'tax-refund', 0, 1, '/tax-refund', 'Layout', NULL, 'DollarOutlined', 4, 0, 0, 1, 1, 0),
(400, '工作流', 'workflow', 0, 1, '/workflow', 'Layout', NULL, 'BranchesOutlined', 5, 0, 0, 1, 1, 0),
(500, '合同管理', 'contract', 0, 1, '/contract', 'Layout', NULL, 'FileTextOutlined', 6, 0, 0, 1, 1, 0),
-- 个人中心子菜单
(4, '个人信息', 'profile-index', 3, 2, 'index', '@/views/profile/index.vue', NULL, 'UserOutlined', 1, 0, 0, 0, 1, 0),
-- 系统管理子菜单
(101, '用户管理', 'system-user', 100, 2, 'user', '@/views/system/user/index.vue', NULL, 'UserOutlined', 1, 0, 0, 1, 1, 0),
(102, '角色管理', 'system-role', 100, 2, 'role', '@/views/system/role/index.vue', NULL, 'TeamOutlined', 2, 0, 0, 1, 1, 0),
(103, '组织管理', 'system-org', 100, 2, 'org', '@/views/system/org/index.vue', NULL, 'ApartmentOutlined', 3, 0, 0, 1, 1, 0),
(104, '菜单管理', 'system-menu', 100, 2, 'menu', '@/views/system/menu/index.vue', NULL, 'MenuOutlined', 4, 0, 0, 1, 1, 0),
(105, '银行账户', 'system-bank-account', 100, 2, 'bank-account', '@/views/system/bank-account/index.vue', NULL, 'BankOutlined', 5, 0, 0, 1, 1, 0),
(106, '国家信息', 'system-country', 100, 2, 'country', '@/views/system/country/index.vue', NULL, 'GlobalOutlined', 6, 0, 0, 1, 1, 0),
(107, 'HS商品维护', 'system-product', 100, 2, 'product', '@/views/system/product/index.vue', NULL, 'DatabaseOutlined', 7, 0, 0, 1, 1, 0),
(108, 'API测试', 'system-api-test', 100, 2, 'api-test', '@/views/system/api-test/index.vue', NULL, 'ApiOutlined', 8, 0, 0, 1, 1, 0),
(109, '系统配置', 'system-config', 100, 2, 'config', '@/views/system/config/index.vue', NULL, 'SettingOutlined', 9, 0, 0, 1, 1, 0),
-- 出口申报子菜单
(201, '申报管理', 'declaration-manage', 200, 2, 'manage', '@/views/declaration/manage/index.vue', 'business:declaration:list', 'ContainerOutlined', 1, 0, 0, 1, 1, 0),
(202, '申报表单', 'declaration-form', 200, 2, 'form', '@/views/declaration/form/index.vue', NULL, 'FileTextOutlined', 2, 0, 0, 0, 1, 0),
(203, '支付凭证', 'declaration-payment', 200, 2, 'payment', '@/views/declaration/payment/index.vue', NULL, 'AccountBookOutlined', 3, 0, 0, 0, 1, 0),
(204, '申报统计', 'declaration-statistics', 200, 2, 'statistics', '@/views/declaration/statistics/index.vue', 'business:declaration:statistics', 'BarChartOutlined', 4, 0, 0, 1, 1, 0),
-- 税务退费子菜单
(301, '退税申请', 'tax-refund-apply', 300, 2, 'apply', '@/views/tax-refund/apply/index.vue', 'business:tax-refund:apply', 'PlusOutlined', 1, 0, 0, 0, 1, 0),
(302, '申请列表', 'tax-refund-list', 300, 2, 'list', '@/views/tax-refund/list/index.vue', 'business:tax-refund:list', 'UnorderedListOutlined', 2, 0, 0, 1, 1, 0),
(303, '申请详情', 'tax-refund-detail', 300, 2, 'detail', '@/views/tax-refund/detail/index.vue', 'business:tax-refund:detail', 'FileSearchOutlined', 3, 0, 0, 0, 1, 0),
-- 工作流子菜单
(401, '流程定义', 'workflow-definition', 400, 2, 'definition', '@/views/workflow/definition/index.vue', 'workflow:definition:list', 'FileDoneOutlined', 1, 0, 0, 1, 1, 0),
(402, '流程设计', 'workflow-modeler', 400, 2, 'modeler', '@/views/workflow/modeler/index.vue', 'workflow:modeler:view', 'EditOutlined', 2, 0, 0, 1, 1, 0),
(403, '流程监控', 'workflow-monitor', 400, 2, 'monitor', '@/views/workflow/monitor/index.vue', 'workflow:monitor:view', 'FundViewOutlined', 3, 0, 0, 1, 1, 0),
(404, '流程实例', 'workflow-instance', 400, 2, 'instance', '@/views/workflow/instance/index.vue', 'workflow:instance:list', 'ProfileOutlined', 4, 0, 0, 1, 1, 0),
(405, '我的任务', 'workflow-task', 400, 2, 'task', '@/views/workflow/task/index.vue', 'workflow:task:list', 'CheckCircleOutlined', 5, 0, 0, 1, 1, 0),
-- 合同管理子菜单
(501, '模板管理', 'contract-template', 500, 2, 'template', '@/views/contract/template/index.vue', 'business:contract:template', 'FileAddOutlined', 1, 0, 0, 1, 1, 0),
(502, '合同列表', 'contract-generation', 500, 2, 'generation', '@/views/contract/generation/index.vue', 'business:contract:generation', 'HistoryOutlined', 2, 0, 0, 1, 1, 0),
-- 用户管理按钮权限
(1011, '用户查询', 'user-query', 101, 3, NULL, NULL, 'user:query', NULL, 1, 0, 0, 1, 1, 0),
(1012, '用户新增', 'user-add', 101, 3, NULL, NULL, 'user:add', NULL, 2, 0, 0, 1, 1, 0),
(1013, '用户编辑', 'user-update', 101, 3, NULL, NULL, 'user:update', NULL, 3, 0, 0, 1, 1, 0),
(1014, '用户删除', 'user-delete', 101, 3, NULL, NULL, 'user:delete', NULL, 4, 0, 0, 1, 1, 0),
(1015, '用户列表', 'user-list', 101, 3, NULL, NULL, 'user:list', NULL, 5, 0, 0, 1, 1, 0),
(1016, '密码重置', 'user-resetPwd', 101, 3, NULL, NULL, 'user:resetPwd', NULL, 6, 0, 0, 1, 1, 0),
-- 角色管理按钮权限
(1021, '角色查询', 'role-query', 102, 3, NULL, NULL, 'role:query', NULL, 1, 0, 0, 1, 1, 0),
(1022, '角色新增', 'role-add', 102, 3, NULL, NULL, 'role:add', NULL, 2, 0, 0, 1, 1, 0),
(1023, '角色编辑', 'role-update', 102, 3, NULL, NULL, 'role:update', NULL, 3, 0, 0, 1, 1, 0),
(1024, '角色删除', 'role-delete', 102, 3, NULL, NULL, 'role:delete', NULL, 4, 0, 0, 1, 1, 0),
(1025, '角色列表', 'role-list', 102, 3, NULL, NULL, 'role:list', NULL, 5, 0, 0, 1, 1, 0),
(1026, '用户角色管理', 'role-user', 102, 3, NULL, NULL, 'role:user', NULL, 6, 0, 0, 1, 1, 0),
(1027, '角色分配', 'role-assign', 102, 3, NULL, NULL, 'role:assign', NULL, 7, 0, 0, 1, 1, 0),
(1028, '菜单权限管理', 'role-menu', 102, 3, NULL, NULL, 'role:menu', NULL, 8, 0, 0, 1, 1, 0),
-- 组织管理按钮权限
(1031, '组织查询', 'org-query', 103, 3, NULL, NULL, 'org:query', NULL, 1, 0, 0, 1, 1, 0),
(1032, '组织新增', 'org-add', 103, 3, NULL, NULL, 'org:add', NULL, 2, 0, 0, 1, 1, 0),
(1033, '组织编辑', 'org-update', 103, 3, NULL, NULL, 'org:update', NULL, 3, 0, 0, 1, 1, 0),
(1034, '组织删除', 'org-delete', 103, 3, NULL, NULL, 'org:delete', NULL, 4, 0, 0, 1, 1, 0),
(1035, '组织列表', 'org-list', 103, 3, NULL, NULL, 'org:list', NULL, 5, 0, 0, 1, 1, 0),
(1036, '组织用户管理', 'org-user', 103, 3, NULL, NULL, 'org:user', NULL, 6, 0, 0, 1, 1, 0),
-- 菜单管理按钮权限
(1041, '菜单查询', 'menu-query', 104, 3, NULL, NULL, 'menu:query', NULL, 1, 0, 0, 1, 1, 0),
(1042, '菜单新增', 'menu-add', 104, 3, NULL, NULL, 'menu:add', NULL, 2, 0, 0, 1, 1, 0),
(1043, '菜单编辑', 'menu-update', 104, 3, NULL, NULL, 'menu:update', NULL, 3, 0, 0, 1, 1, 0),
(1044, '菜单删除', 'menu-delete', 104, 3, NULL, NULL, 'menu:delete', NULL, 4, 0, 0, 1, 1, 0),
(1045, '菜单列表', 'menu-list', 104, 3, NULL, NULL, 'menu:list', NULL, 5, 0, 0, 1, 1, 0),
-- 银行账户按钮权限
(1051, '银行账户列表', 'bank-account-list', 105, 3, NULL, NULL, 'system:bank-account:query', NULL, 1, 0, 0, 1, 1, 0),
(1052, '银行账户新增', 'bank-account-add', 105, 3, NULL, NULL, 'system:bank-account:add', NULL, 2, 0, 0, 1, 1, 0),
(1053, '银行账户编辑', 'bank-account-update', 105, 3, NULL, NULL, 'system:bank-account:update', NULL, 3, 0, 0, 1, 1, 0),
(1054, '银行账户删除', 'bank-account-delete', 105, 3, NULL, NULL, 'system:bank-account:delete', NULL, 4, 0, 0, 1, 1, 0),
(1055, '银行账户查看', 'bank-account-view', 105, 3, NULL, NULL, 'system:bank-account:view', NULL, 5, 0, 0, 1, 1, 0),
-- 国家信息按钮权限
(1061, '国家信息列表', 'country-list', 106, 3, NULL, NULL, 'system:country:list', NULL, 1, 0, 0, 1, 1, 0),
(1062, '国家信息新增', 'country-add', 106, 3, NULL, NULL, 'system:country:add', NULL, 2, 0, 0, 1, 1, 0),
(1063, '国家信息编辑', 'country-edit', 106, 3, NULL, NULL, 'system:country:edit', NULL, 3, 0, 0, 1, 1, 0),
(1064, '国家信息删除', 'country-delete', 106, 3, NULL, NULL, 'system:country:delete', NULL, 4, 0, 0, 1, 1, 0),
(1065, '国家信息查看', 'country-view', 106, 3, NULL, NULL, 'system:country:view', NULL, 5, 0, 0, 1, 1, 0),
-- 商品管理按钮权限
(1071, '商品查询', 'product-query', 107, 3, NULL, NULL, 'system:product:query', NULL, 1, 0, 0, 1, 1, 0),
(1072, '商品新增', 'product-add', 107, 3, NULL, NULL, 'system:product:add', NULL, 2, 0, 0, 1, 1, 0),
(1073, '商品编辑', 'product-update', 107, 3, NULL, NULL, 'system:product:update', NULL, 3, 0, 0, 1, 1, 0),
(1074, '商品删除', 'product-delete', 107, 3, NULL, NULL, 'system:product:delete', NULL, 4, 0, 0, 1, 1, 0),
(1075, '商品列表', 'product-list', 107, 3, NULL, NULL, 'system:product:list', NULL, 5, 0, 0, 1, 1, 0),
-- 系统配置按钮权限
(1091, '配置查询', 'config-query', 109, 3, NULL, NULL, 'system:config:query', NULL, 1, 0, 0, 1, 1, 0),
(1092, '配置新增', 'config-add', 109, 3, NULL, NULL, 'system:config:add', NULL, 2, 0, 0, 1, 1, 0),
(1093, '配置编辑', 'config-update', 109, 3, NULL, NULL, 'system:config:update', NULL, 3, 0, 0, 1, 1, 0),
(1094, '配置删除', 'config-delete', 109, 3, NULL, NULL, 'system:config:delete', NULL, 4, 0, 0, 1, 1, 0),
(1095, '配置列表', 'config-list', 109, 3, NULL, NULL, 'system:config:list', NULL, 5, 0, 0, 1, 1, 0),
-- 申报管理按钮权限
(2011, '申报查询', 'declaration-list', 201, 3, NULL, NULL, 'business:declaration:list', NULL, 1, 0, 0, 1, 1, 0),
(2012, '新增申报', 'declaration-add', 201, 3, NULL, NULL, 'business:declaration:add', NULL, 2, 0, 0, 1, 1, 0),
(2013, '编辑申报', 'declaration-update', 201, 3, NULL, NULL, 'business:declaration:update', NULL, 3, 0, 0, 1, 1, 0),
(2014, '删除申报', 'declaration-delete', 201, 3, NULL, NULL, 'business:declaration:delete', NULL, 4, 0, 0, 1, 1, 0),
(2015, '查看申报', 'declaration-view', 201, 3, NULL, NULL, 'business:declaration:view', NULL, 5, 0, 0, 1, 1, 0),
(2016, '申报详情查询', 'declaration-query', 201, 3, NULL, NULL, 'business:declaration:query', NULL, 6, 0, 0, 1, 1, 0),
(2017, '提交申报', 'declaration-submit', 201, 3, NULL, NULL, 'business:declaration:submit', NULL, 7, 0, 0, 1, 1, 0),
(2018, '申报审核', 'declaration-audit', 201, 3, NULL, NULL, 'business:declaration:audit', NULL, 8, 0, 0, 1, 1, 0),
(2019, '导出申报', 'declaration-export', 201, 3, NULL, NULL, 'business:declaration:export', NULL, 9, 0, 0, 1, 1, 0),
(2020, '下载单证', 'declaration-download', 201, 3, NULL, NULL, 'business:declaration:download', NULL, 10, 0, 0, 1, 1, 0),
(2021, '生成合同', 'declaration-contract', 201, 3, NULL, NULL, 'business:declaration:contract', NULL, 11, 0, 0, 1, 1, 0),
(2022, '付款管理', 'declaration-payment', 201, 3, NULL, NULL, 'business:declaration:payment', NULL, 12, 0, 0, 1, 1, 0),
(2023, '申报编辑权限', 'declaration-edit', 201, 3, NULL, NULL, 'business:declaration:edit', NULL, 13, 0, 0, 1, 1, 0),
(2024, '提货单提交', 'declaration-pickup-submit', 201, 3, NULL, NULL, 'business:declaration:pickup-submit', NULL, 14, 0, 0, 1, 1, 0),
(2025, '提货单审核', 'declaration-pickup-audit', 201, 3, NULL, NULL, 'business:declaration:pickup-audit', NULL, 15, 0, 0, 1, 1, 0),
(2026, '提货单删除', 'declaration-pickup-delete', 201, 3, NULL, NULL, 'business:declaration:pickup-delete', NULL, 16, 0, 0, 1, 1, 0),
-- 退税申请按钮权限
(3011, '申请查询', 'tax-refund-apply-query', 301, 3, NULL, NULL, 'business:tax-refund:apply:query', NULL, 1, 0, 0, 1, 1, 0),
(3012, '申请新增', 'tax-refund-apply-add', 301, 3, NULL, NULL, 'business:tax-refund:apply:add', NULL, 2, 0, 0, 1, 1, 0),
(3013, '申请编辑', 'tax-refund-apply-update', 301, 3, NULL, NULL, 'business:tax-refund:apply:update', NULL, 3, 0, 0, 1, 1, 0),
(3014, '申请删除', 'tax-refund-apply-delete', 301, 3, NULL, NULL, 'business:tax-refund:apply:delete', NULL, 4, 0, 0, 1, 1, 0),
(3015, '新增退费', 'tax-refund-add', 301, 3, NULL, NULL, 'business:tax-refund:add', NULL, 5, 0, 0, 1, 1, 0),
-- 申请列表按钮权限
(3021, '列表查询', 'tax-refund-list-query', 302, 3, NULL, NULL, 'business:tax-refund:list:query', NULL, 1, 0, 0, 1, 1, 0),
(3022, '审核操作', 'tax-refund-approve', 302, 3, NULL, NULL, 'business:tax-refund:approve', NULL, 2, 0, 0, 1, 1, 0),
(3023, '申请审核', 'tax-refund-sys-approve', 302, 3, NULL, NULL, 'system:tax-refund:approve', NULL, 3, 0, 0, 1, 1, 0),
(3024, '财务初审', 'tax-refund-first-review', 302, 3, NULL, NULL, 'system:tax-refund:first-review', NULL, 4, 0, 0, 1, 1, 0),
(3025, '财务复审', 'tax-refund-final-review', 302, 3, NULL, NULL, 'system:tax-refund:final-review', NULL, 5, 0, 0, 1, 1, 0),
(3026, '财务审核', 'tax-refund-finance', 302, 3, NULL, NULL, 'business:tax-refund:finance', NULL, 6, 0, 0, 1, 1, 0),
(3027, '编辑退税申请', NULL, 302, 3, '', '', 'business:tax-refund:edit', '', 4, 0, 0, 1, 1, 0),
(3028, '删除退税申请', NULL, 302, 3, '', '', 'business:tax-refund:delete', '', 5, 0, 0, 1, 1, 0),
(3029, '提交退税申请', NULL, 302, 3, '', '', 'business:tax-refund:submit', '', 6, 0, 0, 1, 1, 0),
-- 工作流按钮权限
(4011, '流程部署', 'workflow-definition-deploy', 401, 3, NULL, NULL, 'workflow:definition:deploy', NULL, 2, 0, 0, 1, 1, 0),
(4012, '流程更新', 'workflow-definition-update', 401, 3, NULL, NULL, 'workflow:definition:update', NULL, 3, 0, 0, 1, 1, 0),
(4041, '流程实例终止', 'workflow-instance-terminate', 404, 3, NULL, NULL, 'workflow:instance:terminate', NULL, 2, 0, 0, 1, 1, 0),
(5024, '实例启动', NULL, 404, 3, NULL, NULL, 'workflow:instance:start', NULL, 3, 0, 0, 1, 1, 0),
(5025, '实例暂停', NULL, 404, 3, NULL, NULL, 'workflow:instance:suspend', NULL, 4, 0, 0, 1, 1, 0),
(5026, '实例激活', NULL, 404, 3, NULL, NULL, 'workflow:instance:activate', NULL, 5, 0, 0, 1, 1, 0),
(5027, '任务拒绝', NULL, 405, 3, NULL, NULL, 'workflow:task:reject', NULL, 3, 0, 0, 1, 1, 0),
(5028, '任务转派', NULL, 405, 3, NULL, NULL, 'workflow:task:transfer', NULL, 4, 0, 0, 1, 1, 0),
-- 合同管理按钮权限
(5011, '模板查询', 'contract-template-query', 501, 3, NULL, NULL, 'business:contract:template:query', NULL, 1, 0, 0, 1, 1, 0),
(5012, '模板新增', 'contract-template-add', 501, 3, NULL, NULL, 'business:contract:template:add', NULL, 2, 0, 0, 1, 1, 0),
(5013, '模板编辑', 'contract-template-update', 501, 3, NULL, NULL, 'business:contract:template:update', NULL, 3, 0, 0, 1, 1, 0),
(5014, '模板删除', 'contract-template-delete', 501, 3, NULL, NULL, 'business:contract:template:delete', NULL, 4, 0, 0, 1, 1, 0),
(5015, '模板上传', 'contract-template-upload', 501, 3, NULL, NULL, 'business:contract:template:upload', NULL, 5, 0, 0, 1, 1, 0),
(5016, '合同生成', 'contract-generate', 501, 3, NULL, NULL, 'business:contract:generate', NULL, 6, 0, 0, 1, 1, 0),
(5021, '合同查询', 'contract-generation-query', 502, 3, NULL, NULL, 'business:contract:generation:query', NULL, 1, 0, 0, 1, 1, 0),
(5022, '合同下载', 'contract-download', 502, 3, NULL, NULL, 'business:contract:download', NULL, 2, 0, 0, 1, 1, 0),
(5023, '合同删除', 'contract-generation-delete', 502, 3, NULL, NULL, 'business:contract:generation:delete', NULL, 3, 0, 0, 1, 1, 0);

-- 7.7 角色菜单关联 (ADMIN角色拥有所有菜单权限)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`) VALUES
-- ADMIN角色的菜单权限
(1, 1, NOW()), (1, 2, NOW()), (1, 3, NOW()), (1, 4, NOW()), (1, 100, NOW()),
(1, 101, NOW()), (1, 102, NOW()), (1, 103, NOW()), (1, 104, NOW()), (1, 105, NOW()),
(1, 106, NOW()), (1, 107, NOW()), (1, 108, NOW()), (1, 109, NOW()), (1, 200, NOW()),
(1, 201, NOW()), (1, 202, NOW()), (1, 203, NOW()), (1, 204, NOW()), (1, 300, NOW()),
(1, 301, NOW()), (1, 302, NOW()), (1, 303, NOW()), (1, 400, NOW()), (1, 401, NOW()),
(1, 402, NOW()), (1, 403, NOW()), (1, 404, NOW()), (1, 405, NOW()), (1, 500, NOW()),
(1, 501, NOW()), (1, 502, NOW()), (1, 1011, NOW()), (1, 1012, NOW()), (1, 1013, NOW()),
(1, 1014, NOW()), (1, 1015, NOW()), (1, 1016, NOW()), (1, 1021, NOW()), (1, 1022, NOW()),
(1, 1023, NOW()), (1, 1024, NOW()), (1, 1025, NOW()), (1, 1026, NOW()), (1, 1027, NOW()),
(1, 1028, NOW()), (1, 1031, NOW()), (1, 1032, NOW()), (1, 1033, NOW()), (1, 1034, NOW()),
(1, 1035, NOW()), (1, 1036, NOW()), (1, 1041, NOW()), (1, 1042, NOW()), (1, 1043, NOW()),
(1, 1044, NOW()), (1, 1045, NOW()), (1, 1051, NOW()), (1, 1052, NOW()), (1, 1053, NOW()),
(1, 1054, NOW()), (1, 1055, NOW()), (1, 1061, NOW()), (1, 1062, NOW()), (1, 1063, NOW()),
(1, 1064, NOW()), (1, 1065, NOW()), (1, 1071, NOW()), (1, 1072, NOW()), (1, 1073, NOW()),
(1, 1074, NOW()), (1, 1075, NOW()), (1, 1091, NOW()), (1, 1092, NOW()), (1, 1093, NOW()),
(1, 1094, NOW()), (1, 1095, NOW()), (1, 2011, NOW()), (1, 2012, NOW()), (1, 2013, NOW()),
(1, 2014, NOW()), (1, 2015, NOW()), (1, 2016, NOW()), (1, 2017, NOW()), (1, 2018, NOW()),
(1, 2019, NOW()), (1, 2020, NOW()), (1, 2021, NOW()), (1, 2022, NOW()), (1, 2023, NOW()),
(1, 2024, NOW()), (1, 2025, NOW()), (1, 2026, NOW()), (1, 3011, NOW()), (1, 3012, NOW()),
(1, 3013, NOW()), (1, 3014, NOW()), (1, 3015, NOW()), (1, 3021, NOW()), (1, 3022, NOW()),
(1, 3023, NOW()), (1, 3024, NOW()), (1, 3025, NOW()), (1, 3026, NOW()), (1, 3027, NOW()),
(1, 3028, NOW()), (1, 3029, NOW()), (1, 4011, NOW()), (1, 4012, NOW()), (1, 4041, NOW()),
(1, 5011, NOW()), (1, 5012, NOW()), (1, 5013, NOW()), (1, 5014, NOW()), (1, 5015, NOW()),
(1, 5016, NOW()), (1, 5021, NOW()), (1, 5022, NOW()), (1, 5023, NOW()), (1, 5024, NOW()),
(1, 5025, NOW()), (1, 5026, NOW()), (1, 5027, NOW()), (1, 5028, NOW()),
-- FINANCE角色的菜单权限
(4, 1, NOW()), (4, 2, NOW()), (4, 3, NOW()), (4, 4, NOW()), (4, 100, NOW()),
(4, 101, NOW()), (4, 103, NOW()), (4, 105, NOW()), (4, 106, NOW()), (4, 107, NOW()),
(4, 109, NOW()), (4, 200, NOW()), (4, 201, NOW()), (4, 202, NOW()), (4, 203, NOW()),
(4, 204, NOW()), (4, 300, NOW()), (4, 301, NOW()), (4, 302, NOW()), (4, 303, NOW()),
(4, 400, NOW()), (4, 402, NOW()), (4, 403, NOW()), (4, 405, NOW()), (4, 500, NOW()),
(4, 501, NOW()), (4, 502, NOW()), (4, 1011, NOW()), (4, 1012, NOW()), (4, 1013, NOW()),
(4, 1014, NOW()), (4, 1015, NOW()), (4, 1016, NOW()), (4, 1021, NOW()), (4, 1025, NOW()),
(4, 1031, NOW()), (4, 1032, NOW()), (4, 1033, NOW()), (4, 1034, NOW()), (4, 1035, NOW()),
(4, 1036, NOW()), (4, 1051, NOW()), (4, 1052, NOW()), (4, 1053, NOW()), (4, 1054, NOW()),
(4, 1055, NOW()), (4, 1061, NOW()), (4, 1062, NOW()), (4, 1063, NOW()), (4, 1064, NOW()),
(4, 1065, NOW()), (4, 1071, NOW()), (4, 1072, NOW()), (4, 1073, NOW()), (4, 1074, NOW()),
(4, 1075, NOW()), (4, 1091, NOW()), (4, 1092, NOW()), (4, 1093, NOW()), (4, 1094, NOW()),
(4, 1095, NOW()), (4, 2011, NOW()), (4, 2012, NOW()), (4, 2013, NOW()), (4, 2014, NOW()),
(4, 2015, NOW()), (4, 2016, NOW()), (4, 2017, NOW()), (4, 2018, NOW()), (4, 2019, NOW()),
(4, 2020, NOW()), (4, 2021, NOW()), (4, 2022, NOW()), (4, 2023, NOW()), (4, 2024, NOW()),
(4, 2025, NOW()), (4, 2026, NOW()), (4, 3011, NOW()), (4, 3012, NOW()), (4, 3013, NOW()),
(4, 3014, NOW()), (4, 3015, NOW()), (4, 3021, NOW()), (4, 3022, NOW()), (4, 3023, NOW()),
(4, 3024, NOW()), (4, 3025, NOW()), (4, 3026, NOW()), (4, 3027, NOW()), (4, 3028, NOW()),
(4, 3029, NOW()), (4, 5011, NOW()), (4, 5012, NOW()), (4, 5013, NOW()), (4, 5014, NOW()),
(4, 5015, NOW()), (4, 5016, NOW()), (4, 5021, NOW()), (4, 5022, NOW()), (4, 5023, NOW()),
(4, 5024, NOW()), (4, 5025, NOW()), (4, 5026, NOW()), (4, 5027, NOW()), (4, 5028, NOW()),
-- SALESPERSON角色的菜单权限
(5, 1, NOW()), (5, 2, NOW()), (5, 3, NOW()), (5, 4, NOW()), (5, 200, NOW()),
(5, 201, NOW()), (5, 202, NOW()), (5, 203, NOW()), (5, 300, NOW()), (5, 301, NOW()),
(5, 302, NOW()), (5, 400, NOW()), (5, 405, NOW()), (5, 500, NOW()), (5, 502, NOW()),
(5, 1061, NOW()), (5, 1065, NOW()), (5, 1071, NOW()), (5, 1075, NOW()), (5, 2011, NOW()),
(5, 2012, NOW()), (5, 2013, NOW()), (5, 2014, NOW()), (5, 2015, NOW()), (5, 2016, NOW()),
(5, 2017, NOW()), (5, 2019, NOW()), (5, 2020, NOW()), (5, 2021, NOW()), (5, 2022, NOW()),
(5, 2023, NOW()), (5, 2024, NOW()), (5, 2026, NOW()), (5, 3011, NOW()), (5, 3012, NOW()),
(5, 3013, NOW()), (5, 3014, NOW()), (5, 3015, NOW()), (5, 3021, NOW()), (5, 5016, NOW()),
(5, 5021, NOW()), (5, 5022, NOW()), (5, 3028, NOW()), (5, 3029, NOW());

-- 7.8 系统配置数据
INSERT IGNORE INTO `sys_config` (`id`, `config_key`, `config_name`, `config_value`, `input_type`, `select_options`, `is_system_param`, `config_type`, `config_group`, `remark`, `status`, `sort`, `create_time`, `update_time`, `del_flag`) VALUES
(1, 'system.name', '系统名称', '海关申报系统', 1, NULL, 1, 1, 'basic', '系统显示名称', 1, 1, NOW(), NOW(), 0),
(2, 'system.version', '系统版本', '1.0.0', 1, NULL, 1, 1, 'basic', '系统版本号', 1, 2, NOW(), NOW(), 0),
(3, 'system.description', '系统描述', '海关申报系统', 1, NULL, 1, 1, 'basic', '系统功能描述', 1, 3, NOW(), NOW(), 0),
(4, 'system.company', '公司名称', '宁波梓熠科技有限公司', 1, NULL, 1, 1, 'basic', '系统所属公司', 1, 4, NOW(), NOW(), 0),
(5, 'system.copyright', '版权信息', '© 2026 宁波梓熠科技有限公司 版权所有', 1, NULL, 1, 1, 'basic', '系统版权信息', 1, 5, NOW(), NOW(), 0),
(6, 'ui.logo', 'Logo图片', '/logo.png', 1, NULL, 1, 2, 'ui', '系统ico图片URL', 1, 1, NOW(), NOW(), 0),
(7, 'ui.favicon', '网站图标', '/favicon.ico', 1, NULL, 1, 2, 'ui', '浏览器标签页图标', 1, 2, NOW(), NOW(), 0),
(8, 'ui.theme', '主题颜色', '#1890ff', 2, '[{"label": "默认蓝", "value": "#1890ff"}, {"label": "科技蓝", "value": "#001529"}, {"label": "活力橙", "value": "#fa8c16"}, {"label": "清新绿", "value": "#52c41a"}]', 1, 2, 'ui', '系统主题色', 1, 3, NOW(), NOW(), 0),
(9, 'ui.footer.text', '底部文字', '海关申报系统 ©2026 宁波梓熠科技有限公司', 1, NULL, 1, 2, 'ui', '页面底部显示文字', 1, 4, NOW(), NOW(), 0),
(10, 'ui.footer.show', '显示底部', 'true', 3, NULL, 1, 2, 'ui', '是否显示页面底部', 1, 5, NOW(), NOW(), 0),
(11, 'ui.sidebar.collapsed', '侧边栏折叠', 'false', 3, NULL, 1, 2, 'ui', '侧边栏默认是否折叠', 1, 6, NOW(), NOW(), 0),
(12, 'ui.language', '系统语言', 'zh-CN', 2, '[{"label": "简体中文", "value": "zh-CN"}, {"label": "English", "value": "en-US"}]', 0, 2, 'ui', '系统显示语言', 1, 7, NOW(), NOW(), 0),
(13, 'business.tax-refund.enabled', '税务退费功能', 'true', 3, NULL, 0, 3, 'business', '是否启用税务退费功能', 1, 1, NOW(), NOW(), 0),
(14, 'business.tax-refund.approval-level', '审批层级', '3', 2, '[{"label": "1级审批", "value": "1"}, {"label": "2级审批", "value": "2"}, {"label": "3级审批", "value": "3"}, {"label": "4级审批", "value": "4"}, {"label": "5级审批", "value": "5"}]', 0, 3, 'business', '税务退费审批层级数', 1, 2, NOW(), NOW(), 0),
(15, 'business.file.upload.max-size', '文件上传大小限制', '10MB', 1, NULL, 0, 3, 'business', '文件上传最大大小', 1, 3, NOW(), NOW(), 0),
(16, 'business.notification.email-enabled', '邮件通知', 'true', 3, NULL, 0, 3, 'business', '是否启用邮件通知', 1, 4, NOW(), NOW(), 0),
(18, 'Transport-details', '运输方式', 'AIR FREIGHT', 2, '[{"label": "AIR FREIGHT", "value": "AIR FREIGHT"}, {"label": "TRUCK", "value": "TRUCK"}]', 0, 3, 'business', '', 1, 0, NOW(), NOW(), 0);

-- 7.9 国家信息数据
INSERT IGNORE INTO `country_info` (`id`, `country_code`, `chinese_name`, `english_name`, `abbreviation`, `continent`, `status`, `sort`, `create_time`, `update_time`, `del_flag`) VALUES
(2, 'USA', '美国', 'United States', 'USA', '北美洲', 1, 2, NOW(), NOW(), 0),
(3, 'GBR', '英国', 'United Kingdom', 'GBR', '欧洲', 1, 3, NOW(), NOW(), 0),
(4, 'DEU', '德国', 'Germany', 'DEU', '欧洲', 1, 4, NOW(), NOW(), 0),
(5, 'FRA', '法国', 'France', 'FRA', '欧洲', 1, 5, NOW(), NOW(), 0),
(6, 'JPN', '日本', 'Japan', 'JPN', '亚洲', 1, 6, NOW(), NOW(), 0),
(7, 'KOR', '韩国', 'South Korea', 'KOR', '亚洲', 1, 7, NOW(), NOW(), 0),
(8, 'AUS', '澳大利亚', 'Australia', 'AUS', '大洋洲', 1, 8, NOW(), NOW(), 0),
(9, 'CAN', '加拿大', 'Canada', 'CAN', '北美洲', 1, 9, NOW(), NOW(), 0),
(10, 'SGP', '新加坡', 'Singapore', 'SGP', '亚洲', 1, 10, NOW(), NOW(), 0),
(11, 'MYS', '马来西亚', 'Malaysia', 'MYS', '亚洲', 1, 11, NOW(), NOW(), 0),
(12, 'THA', '泰国', 'Thailand', 'THA', '亚洲', 1, 12, NOW(), NOW(), 0),
(13, 'VNM', '越南', 'Vietnam', 'VNM', '亚洲', 1, 13, NOW(), NOW(), 0),
(14, 'IND', '印度', 'India', 'IND', '亚洲', 1, 14, NOW(), NOW(), 0),
(15, 'BRA', '巴西', 'Brazil', 'BRA', '南美洲', 1, 15, NOW(), NOW(), 0),
(16, 'RUS', '俄罗斯', 'Russia', 'RUS', '欧洲', 1, 16, NOW(), NOW(), 0),
(17, 'ITA', '意大利', 'Italy', 'ITA', '欧洲', 1, 17, NOW(), NOW(), 0),
(18, 'ESP', '西班牙', 'Spain', 'ESP', '欧洲', 1, 18, NOW(), NOW(), 0),
(19, 'NLD', '荷兰', 'Netherlands', 'NLD', '欧洲', 1, 19, NOW(), NOW(), 0),
(20, 'BEL', '比利时', 'Belgium', 'BEL', '欧洲', 1, 20, NOW(), NOW(), 0);

-- 7.10 计量单位数据
INSERT IGNORE INTO `measurement_units` (`id`, `unit_code`, `unit_name`, `unit_name_en`, `unit_type`, `description`, `status`, `sort`, `create_time`, `update_time`) VALUES
(1, '01', '个', 'PCS', '数量单位', '基本计数单位', 1, 1, NOW(), NOW()),
(2, '02', '台', 'SETS', '数量单位', '设备台数', 1, 2, NOW(), NOW()),
(3, '03', '套', 'SUITS', '数量单位', '成套设备', 1, 3, NOW(), NOW()),
(4, '04', '件', 'ITEMS', '数量单位', '一般物品件数', 1, 4, NOW(), NOW()),
(5, '05', '只', 'ONLY', '数量单位', '动物、器具等', 1, 5, NOW(), NOW()),
(6, '06', '张', 'SHEETS', '数量单位', '纸张、票证等', 1, 6, NOW(), NOW()),
(7, '07', '支', 'BRANCHES', '数量单位', '笔、管状物等', 1, 7, NOW(), NOW()),
(8, '08', '根', 'ROOTS', '数量单位', '棒状物', 1, 8, NOW(), NOW()),
(9, '09', '条', 'STRIPS', '数量单位', '长条形物品', 1, 9, NOW(), NOW()),
(10, '10', '块', 'BLOCKS', '数量单位', '块状物品', 1, 10, NOW(), NOW()),
(11, '11', '千克', 'KILOS', '重量单位', '公斤', 1, 11, NOW(), NOW()),
(12, '12', '克', 'GRAMS', '重量单位', '克', 1, 12, NOW(), NOW()),
(13, '13', '吨', 'TONS', '重量单位', '公吨', 1, 13, NOW(), NOW()),
(14, '14', '立方米', 'CBM', '体积单位', '立方米', 1, 14, NOW(), NOW()),
(15, '15', '平方米', 'SQM', '面积单位', '平方米', 1, 15, NOW(), NOW()),
(16, '16', '米', 'METERS', '长度单位', '米', 1, 16, NOW(), NOW()),
(17, '17', '厘米', 'CM', '长度单位', '厘米', 1, 17, NOW(), NOW()),
(18, '18', '毫米', 'MM', '长度单位', '毫米', 1, 18, NOW(), NOW()),
(19, '19', '升', 'LITERS', '容量单位', '升', 1, 19, NOW(), NOW()),
(20, '20', '毫升', 'ML', '容量单位', '毫升', 1, 20, NOW(), NOW());

-- 7.11 HS商品类型配置数据
INSERT IGNORE INTO `product_type_config` (`id`, `hs_code`, `english_name`, `chinese_name`, `elements_config`, `unit_type`, `unit_code`, `unit_name`, `sort`, `status`, `create_time`, `update_time`, `del_flag`) VALUES
(1, '8523591000', 'RFID STICKER', 'RFID标签', '[{"key": "0", "type": "text", "label": "用途", "value": "提供唯一数据", "editable": false, "required": true}, {"key": "1", "type": "select", "label": "是否录制", "value": "否", "options": ["是", "否"], "editable": false, "required": true}, {"key": "2", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "3", "type": "text", "label": "型号", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "5", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 0, 1, NOW(), NOW(), 0),
(2, '9031809090', 'Handheld reader', 'RFID扫描器', '[{"key": "0", "type": "text", "label": "用途", "value": "扫描唯一码", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "原理", "value": "电子射频扫描", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "功能", "value": "获取无源射频标签数据", "editable": false, "required": true}, {"key": "3", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "型号", "value": "", "editable": true, "required": false}, {"key": "5", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "6", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 2, 1, NOW(), NOW(), 0),
(3, '3926209000', 'PLASTIC SEAL', '吃粒', '[{"key": "0", "type": "text", "label": "用途", "value": "用于衣服上的吃粒", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "材质", "value": "塑料", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "3", "type": "text", "label": "型号", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "5", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 3, 1, NOW(), NOW(), 0),
(4, '4819400000', 'PAPER BAG', '纸袋', '[{"key": "0", "type": "text", "label": "用途", "value": "用于包装衣服", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "材质", "value": "牛皮纸", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "3", "type": "text", "label": "规格", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "5", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 4, 1, NOW(), NOW(), 0),
(5, '5807100000', 'WOVEN LABEL', '织标', '[{"key": "0", "type": "text", "label": "织造方法", "value": "机织", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "成分含量", "value": "100%涤纶", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "", "value": "非刺绣", "editable": false, "required": false}, {"key": "3", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "5", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 5, 1, NOW(), NOW(), 0),
(6, '4821100000', 'HANG TAG', '吃牌', '[{"key": "0", "type": "text", "label": "材质", "value": "纸质", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "加工程度", "value": "印制", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "3", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "4", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 6, 1, NOW(), NOW(), 0),
(7, '3923210000', 'POLYBAG', '塑料袋', '[{"key": "0", "type": "text", "label": "用途", "value": "用于衣服包装", "editable": false, "required": true}, {"key": "1", "type": "text", "label": "材质", "value": "塑料", "editable": false, "required": true}, {"key": "2", "type": "text", "label": "品牌", "value": "", "editable": true, "required": false}, {"key": "3", "type": "text", "label": "型号", "value": "", "editable": true, "required": false}, {"key": "4", "type": "text", "label": "品牌类型", "value": "", "editable": true, "required": false}, {"key": "5", "type": "select", "label": "出口享惠情况", "value": "不确定", "options": ["确定", "不确定", "不适用"], "editable": false, "required": true}]', NULL, NULL, NULL, 7, 1, NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 8. 系统配置 - 文件上传路径配置
-- 说明: 配置的路径为完整路径,系统不再自动拼接用户目录
-- 示例: Windows路径格式: "F:/uploads/templates", Linux/Unix路径格式: "/var/uploads/templates"
-- ============================================================
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `input_type`, `config_type`, `config_group`, `is_system_param`, `remark`, `sort`) VALUES
-- 模板文件路径配置
('file.upload.template-path', '模板文件路径', '/uploads/templates', 1, 3, 'file-upload', 1, '合同模板文件存储完整路径(Windows格式: F:/uploads/templates 或 Linux格式: /var/uploads/templates)', 1),
-- 合同生成路径配置
('file.upload.contract-path', '合同生成路径', '/uploads/contracts', 1, 3, 'file-upload', 1, '生成的合同文件存储完整路径', 2),
-- Excel导出路径配置
('file.upload.export-path', 'Excel导出路径', '/uploads/exports', 1, 3, 'file-upload', 1, 'Excel导出文件存储完整路径', 3)
ON DUPLICATE KEY UPDATE 
  config_name = VALUES(config_name),
  config_value = VALUES(config_value),
  remark = VALUES(remark),
  update_time = NOW();

-- ============================================================
-- 执行完成
-- ============================================================
-- 用户账户信息:
-- admin / admin (管理员, 密码MD5: 21232f297a57a5a743894a0e4a801fc3)
-- Finance01 / 123456 (财务, 密码MD5: 250dfb30df927257fe9401c0c89c0134)
-- user1 / 123456 (业务员, 密码MD5: 8a13a81b63c9f02d897e8b39dd21372f)
-- ============================================================

