-- HS商品类型表（重新设计）
-- 如果表已存在，先删除
DROP TABLE IF EXISTS `product_type_config`;

CREATE TABLE `product_type_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `hs_code` varchar(20) NOT NULL COMMENT 'HS编码',
  `english_name` varchar(255) NOT NULL COMMENT '英文名称',
  `chinese_name` varchar(255) NOT NULL COMMENT '中文名称',
  `elements_config` json COMMENT '申报要素配置JSON',
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

-- 插入默认商品类型配置
INSERT INTO `product_type_config` (`hs_code`, `english_name`, `chinese_name`, `elements_config`, `sort`) VALUES
('85235210', 'RFID STICKER or RFID LABEL', 'RFID标签', '[
  {"key": "0", "label": "用途", "type": "text", "defaultValue": "提供唯一数据", "placeholder": "提供唯一数据", "editable": false, "required": true},
  {"key": "1", "label": "是否录制", "type": "select", "defaultValue": "否", "placeholder": "请选择", "options": ["是", "否"], "editable": true, "required": true},
  {"key": "2", "label": "品牌", "type": "text", "defaultValue": "", "placeholder": "请输入品牌", "editable": true, "required": false},
  {"key": "3", "label": "型号", "type": "text", "defaultValue": "", "placeholder": "请输入型号", "editable": true, "required": false},
  {"key": "4", "label": "品牌类型", "type": "text", "defaultValue": "", "placeholder": "请输入品牌类型", "editable": true, "required": false},
  {"key": "5", "label": "出口享惠情况", "type": "select", "defaultValue": "不确定", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "editable": true, "required": true}
]', 1),

('85235290', 'Handheld reader或Fixed reader', 'RFID扫描器', '[
  {"key": "0", "label": "用途", "type": "text", "placeholder": "扫描唯一码", "value": "扫描唯一码", "required": true},
  {"key": "1", "label": "原理", "type": "text", "placeholder": "电子射频扫描", "value": "电子射频扫描", "required": true},
  {"key": "2", "label": "功能", "type": "text", "placeholder": "获取无源射频标签数据", "value": "获取无源射频标签数据", "required": true},
  {"key": "3", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "4", "label": "型号", "type": "text", "placeholder": "请输入型号", "value": "", "required": false},
  {"key": "5", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "6", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 2),

('39269090', 'PLASTIC SEAL', '吊粒', '[
  {"key": "0", "label": "用途", "type": "text", "placeholder": "用于衣服上的吊粒", "value": "用于衣服上的吊粒", "required": true},
  {"key": "1", "label": "材质", "type": "text", "placeholder": "塑料", "value": "塑料", "required": true},
  {"key": "2", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "3", "label": "型号", "type": "text", "placeholder": "请输入型号", "value": "", "required": false},
  {"key": "4", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "5", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 3),

('48192000', 'PAPER BAG', '纸袋', '[
  {"key": "0", "label": "用途", "type": "text", "placeholder": "用于包装衣服", "value": "用于包装衣服", "required": true},
  {"key": "1", "label": "材质", "type": "text", "placeholder": "牛皮纸", "value": "牛皮纸", "required": true},
  {"key": "2", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "3", "label": "规格", "type": "text", "placeholder": "请输入规格", "value": "", "required": false},
  {"key": "4", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "5", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 4),

('58071000', 'WOVEN LABEL', '织标', '[
  {"key": "0", "label": "织造方法", "type": "text", "placeholder": "机织", "value": "机织", "required": true},
  {"key": "1", "label": "成分含量", "type": "text", "placeholder": "100%涤纶", "value": "100%涤纶", "required": true},
  {"key": "2", "label": "非刺绣", "type": "checkbox", "placeholder": "非刺绣", "value": true, "required": false},
  {"key": "3", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "4", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "5", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 5),

('48211000', 'HANG TAG或Card', '吊牌或纸卡', '[
  {"key": "0", "label": "材质", "type": "text", "placeholder": "纸质", "value": "纸质", "required": true},
  {"key": "1", "label": "加工程度", "type": "text", "placeholder": "印制", "value": "印制", "required": true},
  {"key": "2", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "3", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "4", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 6),

('39232900', 'Polybag', '塑料袋', '[
  {"key": "0", "label": "用途", "type": "text", "placeholder": "用于衣服包装", "value": "用于衣服包装", "required": true},
  {"key": "1", "label": "材质", "type": "text", "placeholder": "塑料", "value": "塑料", "required": true},
  {"key": "2", "label": "品牌", "type": "text", "placeholder": "请输入品牌", "value": "", "required": false},
  {"key": "3", "label": "型号", "type": "text", "placeholder": "请输入型号", "value": "", "required": false},
  {"key": "4", "label": "品牌类型", "type": "text", "placeholder": "请输入品牌类型", "value": "", "required": false},
  {"key": "5", "label": "出口享惠情况", "type": "select", "placeholder": "请选择", "options": ["确定", "不确定", "不适用"], "value": "不确定", "required": true}
]', 7);
