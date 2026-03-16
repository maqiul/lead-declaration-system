-- 出口申报单相关表结构

-- 申报单主表
DROP TABLE IF EXISTS `declaration_form`;
CREATE TABLE `declaration_form` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `form_no` varchar(50) NOT NULL COMMENT '申报单号',
  `shipper_company` varchar(255) NOT NULL COMMENT '发货人公司名',
  `shipper_address` text NOT NULL COMMENT '发货人地址',
  `consignee_company` varchar(255) NOT NULL COMMENT '收货人公司名',
  `consignee_address` text NOT NULL COMMENT '收货人地址',
  `invoice_no` varchar(50) NOT NULL COMMENT '发票号',
  `declaration_date` date NOT NULL COMMENT '申报日期',
  `transport_mode` varchar(50) NOT NULL COMMENT '运输方式',
  `departure_city` varchar(100) NOT NULL COMMENT '出发城市',
  `destination_region` varchar(100) NOT NULL COMMENT '目的地区域',
  `currency` varchar(10) NOT NULL COMMENT '币种',
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
  `unit_price` decimal(15,4) NOT NULL COMMENT '单价',
  `amount` decimal(15,2) NOT NULL COMMENT '金额',
  `gross_weight` decimal(10,2) NOT NULL COMMENT '毛重(KGS)',
  `net_weight` decimal(10,2) NOT NULL COMMENT '净重(KGS)',
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
  CONSTRAINT `fk_carton_product_carton` FOREIGN KEY (`carton_id`) REFERENCES `declaration_carton` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_carton_product_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='箱子产品关联表';

-- 申报要素填写记录表
DROP TABLE IF EXISTS `declaration_element_value`;
CREATE TABLE `declaration_element_value` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `element_name` varchar(100) NOT NULL COMMENT '要素名称',
  `element_value` text COMMENT '要素值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  CONSTRAINT `fk_declaration_element_product` FOREIGN KEY (`product_id`) REFERENCES `declaration_product` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报要素填写记录表';