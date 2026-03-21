-- 完整的Lead Declaration系统数据库初始化脚本
-- 包含所有必要的表结构和基础数据

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `lead_declaration` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lead_declaration`;

-- ====================================================================
-- 系统基础表
-- ====================================================================

-- 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT '0' COMMENT '性别 0-未知 1-男 2-女',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `role_desc` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据范围 1-全部数据 2-本部门及以下 3-本部门 4-本人数据',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 菜单表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(100) NOT NULL COMMENT '菜单编码',
  `menu_type` tinyint NOT NULL COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `permission` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(50) DEFAULT NULL COMMENT '菜单图标',
  `redirect` varchar(255) DEFAULT NULL COMMENT '重定向地址',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否可见 0-隐藏 1-显示',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_menu_type` (`menu_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- 部门表
DROP TABLE IF EXISTS `sys_organization`;
CREATE TABLE `sys_organization` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父部门ID',
  `org_name` varchar(100) NOT NULL COMMENT '部门名称',
  `org_code` varchar(50) NOT NULL COMMENT '部门编码',
  `org_type` tinyint NOT NULL DEFAULT '1' COMMENT '部门类型 1-公司 2-部门 3-小组',
  `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_code` (`org_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织机构表';

-- 用户角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`,`role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`,`menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 用户部门关联表
DROP TABLE IF EXISTS `sys_user_org`;
CREATE TABLE `sys_user_org` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `org_id` bigint NOT NULL COMMENT '部门ID',
  `is_primary` tinyint NOT NULL DEFAULT '0' COMMENT '是否主部门 0-否 1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_org` (`user_id`,`org_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_org_id` (`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关联表';

-- 系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_value` text COMMENT '配置值',
  `input_type` tinyint DEFAULT '1' COMMENT '配置输入类型 1-文本框 2-下拉框 3-开关 4-数字输入框',
  `select_options` json COMMENT '下拉框选项JSON(当input_type=2时使用)',
  `is_system_param` tinyint DEFAULT '0' COMMENT '是否系统内置参数 0-否 1-是',
  `config_type` tinyint NOT NULL DEFAULT '1' COMMENT '配置类型 1-系统配置 2-UI配置 3-业务配置',
  `config_group` varchar(50) NOT NULL COMMENT '配置分组',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注说明',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ====================================================================
-- 业务表
-- ====================================================================

-- HS商品类型配置表
DROP TABLE IF EXISTS `product_type_config`;
CREATE TABLE `product_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hs_code` varchar(20) NOT NULL COMMENT 'HS编码',
  `english_name` varchar(255) NOT NULL COMMENT '英文名称',
  `chinese_name` varchar(255) NOT NULL COMMENT '中文名称',
  `elements_config` json COMMENT '申报要素配置JSON',
  `unit_type` varchar(20) COMMENT '计量单位类型',
  `unit_code` varchar(10) COMMENT '计量单位代码',
  `unit_name` varchar(50) COMMENT '计量单位名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hs_code` (`hs_code`),
  KEY `idx_english_name` (`english_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HS商品类型配置表';

-- 计量单位表
DROP TABLE IF EXISTS `measurement_units`;
CREATE TABLE `measurement_units` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `unit_code` varchar(10) NOT NULL COMMENT '单位代码',
  `unit_name` varchar(50) NOT NULL COMMENT '单位名称',
  `unit_name_en` varchar(50) COMMENT '英文单位名称',
  `unit_type` varchar(20) NOT NULL COMMENT '单位类型',
  `abbreviation` varchar(10) COMMENT '缩写',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 0-禁用 1-启用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_unit_code` (`unit_code`),
  KEY `idx_unit_type` (`unit_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计量单位表';

-- 出口申报单主表
DROP TABLE IF EXISTS `declaration_form`;
CREATE TABLE `declaration_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_no` varchar(50) NOT NULL COMMENT '申报单号',
  `shipper_company` varchar(255) NOT NULL COMMENT '发货人公司名',
  `shipper_address` text NOT NULL COMMENT '发货人地址',
  `consignee_company` varchar(255) NOT NULL COMMENT '收货人公司名',
  `consignee_address` text NOT NULL COMMENT '收货人地址',
  `departure_city` varchar(100) COMMENT '出发城市',
  `destination_region` varchar(100) COMMENT '到达地区',
  `currency` varchar(10) NOT NULL DEFAULT 'USD' COMMENT '币种',
  `total_quantity` int NOT NULL DEFAULT '0' COMMENT '总数量',
  `total_gross_weight` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '总毛重(KGS)',
  `total_net_weight` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '总净重(KGS)',
  `total_volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '总体积(CBM)',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '总金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0-草稿 1-已提交 2-已审核 3-已完成',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_no` (`form_no`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出口申报单主表';

-- 申报单产品明细表
DROP TABLE IF EXISTS `declaration_product`;
CREATE TABLE `declaration_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `product_name` varchar(255) NOT NULL COMMENT '产品名称',
  `hs_code` varchar(20) NOT NULL COMMENT 'HS编码',
  `quantity` int NOT NULL COMMENT '数量',
  `unit` varchar(20) NOT NULL COMMENT '单位',
  `unit_price` decimal(10,2) NOT NULL COMMENT '单价',
  `amount` decimal(15,2) NOT NULL COMMENT '金额',
  `gross_weight` decimal(10,3) NOT NULL COMMENT '毛重(KGS)',
  `net_weight` decimal(10,3) NOT NULL COMMENT '净重(KGS)',
  `cartons` int NOT NULL DEFAULT '1' COMMENT '箱数',
  `volume` decimal(10,3) NOT NULL DEFAULT '0.000' COMMENT '体积(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_hs_code` (`hs_code`),
  CONSTRAINT `fk_declaration_product_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报单产品明细表';

-- 申报单箱子信息表
DROP TABLE IF EXISTS `declaration_carton`;
CREATE TABLE `declaration_carton` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `carton_no` varchar(50) NOT NULL COMMENT '箱号',
  `quantity` int NOT NULL COMMENT '箱子数量',
  `volume` decimal(10,3) NOT NULL COMMENT '总体积(CBM)',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_carton_no` (`carton_no`),
  CONSTRAINT `fk_declaration_carton_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报单箱子信息表';

-- 箱子产品关联表
DROP TABLE IF EXISTS `declaration_carton_product`;
CREATE TABLE `declaration_carton_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `carton_id` bigint NOT NULL COMMENT '箱子ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `quantity` int NOT NULL COMMENT '该箱中产品数量',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_carton_id` (`carton_id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_declaration_carton_product_carton` FOREIGN KEY (`carton_id`) REFERENCES `declaration_carton` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_declaration_carton_product_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='箱子产品关联表';

-- 申报要素表
DROP TABLE IF EXISTS `declaration_element`;
CREATE TABLE `declaration_element` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_type_id` bigint NOT NULL COMMENT '商品类型ID',
  `element_key` varchar(50) NOT NULL COMMENT '要素键',
  `element_label` varchar(100) NOT NULL COMMENT '要素标签',
  `element_type` varchar(20) NOT NULL COMMENT '要素类型 text/select/checkbox',
  `required` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填 0-否 1-是',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_type_id` (`product_type_id`),
  CONSTRAINT `fk_declaration_element_product_type` FOREIGN KEY (`product_type_id`) REFERENCES `product_type_config` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报要素表';

-- 申报要素值表
DROP TABLE IF EXISTS `declaration_element_value`;
CREATE TABLE `declaration_element_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_id` bigint NOT NULL COMMENT '申报单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `element_id` bigint NOT NULL COMMENT '要素ID',
  `element_value` text COMMENT '要素值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_id` (`form_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_element_id` (`element_id`),
  CONSTRAINT `fk_declaration_element_value_form` FOREIGN KEY (`form_id`) REFERENCES `declaration_form` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_declaration_element_value_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_declaration_element_value_element` FOREIGN KEY (`element_id`) REFERENCES `declaration_element` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报要素值表';

-- 税务退费申请表
DROP TABLE IF EXISTS `tax_refund_application`;
CREATE TABLE `tax_refund_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_no` varchar(50) NOT NULL COMMENT '申请编号',
  `initiator_id` bigint NOT NULL COMMENT '发起人ID',
  `initiator_name` varchar(50) NOT NULL COMMENT '发起人姓名',
  `department_id` bigint DEFAULT NULL COMMENT '发起部门ID',
  `department_name` varchar(100) DEFAULT NULL COMMENT '发起部门名称',
  `application_type` varchar(50) NOT NULL COMMENT '申请类型',
  `amount` decimal(15,2) NOT NULL COMMENT '申请金额',
  `description` text COMMENT '申请说明',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '当前状态：0-草稿 1-部门提交 2-财务初审 3-文件生成 4-退回补充 5-发票提交 6-财务复审 7-已完成 8-已拒绝',
  `first_review_opinion` text COMMENT '财务初审意见',
  `first_reviewer_id` bigint DEFAULT NULL COMMENT '财务初审人ID',
  `first_reviewer_name` varchar(50) DEFAULT NULL COMMENT '财务初审人姓名',
  `first_review_time` datetime DEFAULT NULL COMMENT '财务初审时间',
  `generated_file_path` varchar(255) DEFAULT NULL COMMENT '生成的文件路径',
  `invoice_no` varchar(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_amount` decimal(15,2) DEFAULT NULL COMMENT '发票金额',
  `tax_rate` decimal(5,2) DEFAULT NULL COMMENT '税率',
  `refund_amount` decimal(15,2) DEFAULT NULL COMMENT '退税金额',
  `invoice_image_path` varchar(255) DEFAULT NULL COMMENT '发票图片路径',
  `final_review_opinion` text COMMENT '财务复审意见',
  `final_reviewer_id` bigint DEFAULT NULL COMMENT '财务复审人ID',
  `final_reviewer_name` varchar(50) DEFAULT NULL COMMENT '财务复审人姓名',
  `final_review_time` datetime DEFAULT NULL COMMENT '财务复审时间',
  `archive_file_path` varchar(255) DEFAULT NULL COMMENT '归档文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人',
  `update_by` bigint DEFAULT NULL COMMENT '更新人',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志 0-正常 1-删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_no` (`application_no`),
  KEY `idx_initiator_id` (`initiator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费申请表';

-- 税务退费申请附件表
DROP TABLE IF EXISTS `tax_refund_attachment`;
CREATE TABLE `tax_refund_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_url` varchar(500) NOT NULL COMMENT '文件URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人ID',
  `uploader_name` varchar(50) DEFAULT NULL COMMENT '上传人姓名',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费申请附件表';

-- 税务退费操作历史表
DROP TABLE IF EXISTS `tax_refund_operation_history`;
CREATE TABLE `tax_refund_operation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `action` varchar(50) NOT NULL COMMENT '操作类型：submit-提交 approve-审核通过 reject-审核拒绝 return-退回补充 complete-流程完成',
  `remark` text COMMENT '操作备注',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='税务退费操作历史表';

-- Flowable流程相关表（如果未自动创建）
-- 这些表通常由Flowable自动创建，这里仅作参考
/*
CREATE TABLE ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
);

CREATE TABLE ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ LONGBLOB,
    GENERATED_ TINYINT,
    primary key (ID_)
);
*/

-- ====================================================================
-- 初始化基础数据
-- ====================================================================

-- 插入默认用户
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `real_name`, `email`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9kIuXpeDGLJQvri.vvBg/bxWX.6.lZy33LFvYMz/7WmJUmO', '管理员', '系统管理员', 'admin@example.com', 1),
(2, 'finance', '$2a$10$N.zmdr9kIuXpeDGLJQvri.vvBg/bxWX.6.lZy33LFvYMz/7WmJUmO', '财务员', '张财务', 'finance@example.com', 1),
(3, 'user1', '$2a$10$N.zmdr9kIuXpeDGLJQvri.vvBg/bxWX.6.lZy33LFvYMz/7WmJUmO', '普通用户', '李用户', 'user1@example.com', 1);

-- 插入默认角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `role_desc`, `data_scope`, `sort`) VALUES
(1, '超级管理员', 'admin', '系统超级管理员，拥有所有权限', 1, 1),
(2, '财务人员', 'finance', '财务部门人员', 2, 2),
(3, '普通用户', 'user', '普通业务用户', 3, 3);

-- 插入默认菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort`, `status`) VALUES
-- 顶级菜单
(1, 0, '系统管理', 'system', 1, '/system', 'Layout', '', 'SettingOutlined', 1, 1),
(2, 0, '业务管理', 'business', 1, '/business', 'Layout', '', 'AppstoreOutlined', 2, 1),

-- 系统管理子菜单
(101, 1, '用户管理', 'system_user', 2, '/system/user', 'system/user/index', 'system:user:list', 'UserOutlined', 1, 1),
(102, 1, '角色管理', 'system_role', 2, '/system/role', 'system/role/index', 'system:role:list', 'TeamOutlined', 2, 1),
(103, 1, '菜单管理', 'system_menu', 2, '/system/menu', 'system/menu/index', 'system:menu:list', 'MenuOutlined', 3, 1),
(104, 1, '部门管理', 'system_org', 2, '/system/org', 'system/org/index', 'system:org:list', 'ApartmentOutlined', 4, 1),
(105, 1, '系统配置', 'system_config', 2, '/system/config', 'system/config/index', 'system:config:list', 'SettingOutlined', 5, 1),

-- 业务管理子菜单
(201, 2, '出口申报', 'business_declaration', 2, '/business/declaration', 'business/declaration/index', 'business:declaration:list', 'FileTextOutlined', 1, 1),
(202, 2, '税务退费', 'business_tax_refund', 2, '/business/tax-refund', 'business/tax-refund/index', 'business:tax-refund:list', 'DollarCircleOutlined', 2, 1),

-- 按钮权限
(1001, 101, '用户查询', 'user_query', 3, '', '', 'system:user:query', '', 1, 1),
(1002, 101, '用户新增', 'user_add', 3, '', '', 'system:user:add', '', 2, 1),
(1003, 101, '用户修改', 'user_update', 3, '', '', 'system:user:update', '', 3, 1),
(1004, 101, '用户删除', 'user_delete', 3, '', '', 'system:user:delete', '', 4, 1);

-- 用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), -- admin -> 超级管理员
(2, 2), -- finance -> 财务人员
(3, 3); -- user1 -> 普通用户

-- 角色菜单关联（简化示例，实际需要配置完整权限）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 2), (1, 101), (1, 102), (1, 103), (1, 104), (1, 105), (1, 201), (1, 202), -- 管理员所有权限
(2, 2), (2, 202), -- 财务人员权限
(3, 2), (3, 201); -- 普通用户权限

-- 插入系统配置
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `input_type`, `config_type`, `config_group`, `is_system_param`, `remark`, `sort`) VALUES
('system.name', '系统名称', '线索申报系统', 1, 1, 'basic', 1, '系统显示名称', 1),
('system.version', '系统版本', '1.0.0', 1, 1, 'basic', 1, '系统版本号', 2),
('system.description', '系统描述', '企业线索申报管理系统', 1, 1, 'basic', 1, '系统功能描述', 3);

-- 插入计量单位数据
INSERT INTO `measurement_units` (`unit_code`, `unit_name`, `unit_name_en`, `unit_type`, `abbreviation`, `sort`) VALUES
('01', '个', 'PCS', 'COUNT', 'PCS', 1),
('02', '千克', 'KGS', 'WEIGHT', 'KGS', 2),
('03', '立方米', 'CBM', 'VOLUME', 'CBM', 3),
('04', '米', 'MTR', 'LENGTH', 'MTR', 4),
('05', '平方米', 'MTK', 'AREA', 'MTK', 5);

-- 插入HS商品类型数据（来自hs-product-type.sql的部分数据）
INSERT INTO `product_type_config` (`hs_code`, `english_name`, `chinese_name`, `elements_config`, `unit_type`, `unit_code`, `unit_name`, `sort`) VALUES
('85235210', 'RFID STICKER or RFID LABEL', 'RFID标签', '[{"key": "0", "label": "用途", "type": "text", "defaultValue": "提供唯一数据", "placeholder": "提供唯一数据", "editable": false, "required": true}]', 'COUNT', '01', '个', 1),
('39269090', 'PLASTIC SEAL', '吊粒', '[{"key": "0", "label": "用途", "type": "text", "placeholder": "用于衣服上的吊粒", "value": "用于衣服上的吊粒", "required": true}]', 'COUNT', '01', '个', 2);

SET FOREIGN_KEY_CHECKS = 1;

COMMIT;