-- ============================================================
-- 补充资料阶段模板数据
-- 环节：SUPPLEMENT（补充资料）
-- 位置：在资料上传之后、业务发票之前
-- 5个资料项，全部必填
-- ============================================================

INSERT INTO `declaration_material_template` (`code`, `name`, `required`, `sort`, `remark`, `form_schema`, `enabled`, `stage`) VALUES
('SUPPLEMENT_CUSTOMS_PRE_ENTRY', '报关预录入单', 1, 10, '报关预录入单证文件', NULL, 1, 'SUPPLEMENT'),
('SUPPLEMENT_RELEASE_ORDER', '放行单', 1, 20, '海关放行单', NULL, 1, 'SUPPLEMENT'),
('SUPPLEMENT_CUSTOMS_AGREEMENT', '委托报关协议', 1, 30, '委托报关协议文件', NULL, 1, 'SUPPLEMENT'),
('SUPPLEMENT_DELIVERY_ORDER', '提货单', 1, 40, '提货单/提单', NULL, 1, 'SUPPLEMENT'),
('SUPPLEMENT_CUSTOMS_RECEIPT', '海关回执文件', 1, 50, '海关回执/回执单', NULL, 1, 'SUPPLEMENT');
