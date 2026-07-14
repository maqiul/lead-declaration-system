-- ============================================
-- 箱子产品关联表增加毛重、净重字段
-- 用于箱子绑定产品时分别设置每个产品的数量、毛重、净重
-- ============================================

ALTER TABLE `declaration_carton_product`
  ADD COLUMN `gross_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '该产品在该箱中的毛重(KGS)' AFTER `quantity`,
  ADD COLUMN `net_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '该产品在该箱中的净重(KGS)' AFTER `gross_weight`;
