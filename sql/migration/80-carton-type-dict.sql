-- ============================================================
-- 箱子类型字典化（carton_type）
--   原先前端写死「纸箱 / 托盘」两个选项，现改为字典维护，并新增「木箱」
--   item_value = 英文前缀：落库 declaration_carton.type_english，同时作为箱号前缀与导出单证的包装种类
--   item_label = 中文名：落库 declaration_carton.type_chinese，作为下拉选中键
--   约束：英文前缀必须以复数 S 结尾 —— 导出单证在总箱数为 1 时会截掉末位字母生成单数（CARTONS -> CARTON）
--   前端在字典缺失或接口异常时回退内置选项（纸箱/木箱/托盘），不会导致下拉不可用
-- ============================================================

INSERT INTO `sys_dict` (`dict_code`, `dict_name`, `status`, `remark`)
SELECT 'carton_type', '箱子类型', 1, '申报箱子/包装类型。值=英文前缀（必须以复数S结尾，导出单证在总箱数为1时会截掉末位字母生成单数），显示文本=中文名（下拉选中值）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict` WHERE `dict_code` = 'carton_type');

INSERT INTO `sys_dict_item` (`dict_code`, `item_value`, `item_label`, `sort_order`, `status`)
SELECT 'carton_type', 'CARTONS', '纸箱', 1, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_item` WHERE `dict_code` = 'carton_type' AND `item_value` = 'CARTONS');

INSERT INTO `sys_dict_item` (`dict_code`, `item_value`, `item_label`, `sort_order`, `status`)
SELECT 'carton_type', 'CASES', '木箱', 2, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_item` WHERE `dict_code` = 'carton_type' AND `item_value` = 'CASES');

INSERT INTO `sys_dict_item` (`dict_code`, `item_value`, `item_label`, `sort_order`, `status`)
SELECT 'carton_type', 'PALLETS', '托盘', 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_dict_item` WHERE `dict_code` = 'carton_type' AND `item_value` = 'PALLETS');

-- ============================================================
-- 历史数据修正：早期箱子 type_english 存在拼写错误 CARTRONS，
-- 会导致导出单证上的包装种类拼错，统一修正为 CARTONS
-- ============================================================
UPDATE `declaration_carton`
SET `type_english` = 'CARTONS'
WHERE `type_english` = 'CARTRONS';
