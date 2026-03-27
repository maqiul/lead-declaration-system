-- 为申报单箱子表添加类型字段
ALTER TABLE `declaration_carton`
ADD COLUMN `type_chinese` VARCHAR(50) COMMENT '类型中文名（箱子/托盘）',
ADD COLUMN `type_english` VARCHAR(50) COMMENT '类型英文名（Carton/Pallet）';