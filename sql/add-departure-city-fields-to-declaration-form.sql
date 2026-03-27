-- 为申报单主表添加出发城市中英文字段
ALTER TABLE `declaration_form`
ADD COLUMN `departure_city_chinese` VARCHAR(100) COMMENT '出发城市中文名',
ADD COLUMN `departure_city_english` VARCHAR(200) COMMENT '出发城市英文名';