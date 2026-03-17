-- 申报单草稿箱表
CREATE TABLE IF NOT EXISTS `declaration_draft` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `org_id` bigint DEFAULT NULL COMMENT '所属组织ID',
  `form_no` varchar(50) DEFAULT NULL COMMENT '申报单号',
  `shipper_company` varchar(255) DEFAULT NULL COMMENT '发货人',
  `consignee_company` varchar(255) DEFAULT NULL COMMENT '收货人',
  `total_amount` decimal(15,2) DEFAULT '0.00' COMMENT '总金额',
  `form_data` longtext NOT NULL COMMENT '整个表单的JSON数据',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_org` (`user_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='申报单草稿箱表';
