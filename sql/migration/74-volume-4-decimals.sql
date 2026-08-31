-- ============================================================
-- 74: 体积统一保留 4 位小数
-- 背景：volume / total_volume 原为 decimal(10,3)，前端按 长×宽×高×数量 算出的
--       4 位精度在落库时被 MySQL 四舍五入到 3 位，重新加载后第 4 位丢失。
-- ============================================================

-- 1) 列宽扩到 4 位小数（保持原 NULL 约束与默认值）
ALTER TABLE declaration_carton
    MODIFY COLUMN volume DECIMAL(12, 4) NOT NULL COMMENT '该箱总体积(CBM) = 单箱体积 × 数量';

ALTER TABLE declaration_product
    MODIFY COLUMN volume DECIMAL(12, 4) NOT NULL COMMENT '体积(CBM)';

ALTER TABLE declaration_form
    MODIFY COLUMN total_volume DECIMAL(12, 4) NULL DEFAULT 0.0000 COMMENT '总体积(CBM)';

-- 2) 回填：长宽高齐全的历史箱子行，按 4 位精度重算体积（修复被截断的数据）
UPDATE declaration_carton
SET volume = ROUND(length_cm * width_cm * height_cm * quantity / 1000000, 4)
WHERE length_cm IS NOT NULL
  AND width_cm IS NOT NULL
  AND height_cm IS NOT NULL
  AND length_cm > 0
  AND width_cm > 0
  AND height_cm > 0;

-- 3) 回填：上述箱子所属申报单的总体积按箱子累加重算（与前端保存口径一致）
UPDATE declaration_form f
SET f.total_volume = (
        SELECT ROUND(COALESCE(SUM(c.volume), 0), 4)
        FROM declaration_carton c
        WHERE c.form_id = f.id
    )
WHERE EXISTS (
        SELECT 1
        FROM declaration_carton c2
        WHERE c2.form_id = f.id
          AND c2.length_cm IS NOT NULL
          AND c2.width_cm IS NOT NULL
          AND c2.height_cm IS NOT NULL
    );
