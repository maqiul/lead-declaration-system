-- ============================================================
-- 老申报单流程迁移（业务状态 + 资料环节）
-- 执行前请先备份 declaration_form / declaration_material_item
--
-- 说明：
-- 1) 若未执行过 24-flow-supplement-invoice-amount.sql，先执行该脚本完成 status 数字迁移
-- 2) 本脚本仅修正「业务 status」与资料环节，Flowable 流程实例需通过接口恢复：
--    POST /api/v1/declarations/migrate-flow/batch?dryRun=true   （预览）
--    POST /api/v1/declarations/migrate-flow/batch?dryRun=false  （执行）
--    或单条：POST /api/v1/declarations/{id}/resume-flow
-- ============================================================

SET NAMES utf8mb4;

-- ---------- A. 旧版 status 数字迁移 ----------
-- 必须先执行 migration/24-flow-supplement-invoice-amount.sql（仅可执行一次）。
-- 切勿在本脚本重复执行 24 的 UPDATE，否则会把新版 status=9（待发票审核）误改成 11。

-- ---------- B. 资料审核已通过但仍停在 status=3 → 改为待补充资料提交(4) ----------
UPDATE `declaration_form` f
SET f.`status` = 4
WHERE f.`status` = 3
  AND EXISTS (
    SELECT 1 FROM `business_audit_record` r
    WHERE r.`business_id` = f.`id`
      AND r.`business_type` = 'DECLARATION_MATERIAL_AUDIT'
      AND r.`audit_status` = 1
  );

-- ---------- C. 老流程直接进入发票(8) 且从未上传补充资料 → 回到补充资料(4) ----------
UPDATE `declaration_form` f
SET f.`status` = 4
WHERE f.`status` = 8
  AND EXISTS (
    SELECT 1 FROM `business_audit_record` r
    WHERE r.`business_id` = f.`id`
      AND r.`business_type` = 'DECLARATION_MATERIAL_AUDIT'
      AND r.`audit_status` = 1
  )
  AND NOT EXISTS (
    SELECT 1 FROM `declaration_material_item` i
    WHERE i.`form_id` = f.`id`
      AND i.`stage` = 'SUPPLEMENT'
      AND i.`status` = 1
  );

-- ---------- D. 资料项 stage 兜底（未执行 21 时） ----------
-- 若列已存在且默认 MATERIAL_SUBMIT，以下 UPDATE 仅修正发票类模板编码
UPDATE `declaration_material_template`
SET `stage` = 'INVOICE'
WHERE `code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
  AND (`stage` IS NULL OR `stage` = '' OR `stage` = 'MATERIAL_SUBMIT');

UPDATE `declaration_material_item` i
INNER JOIN `declaration_material_template` t ON i.`template_id` = t.`id`
SET i.`stage` = t.`stage`
WHERE t.`code` IN ('FREIGHT_INVOICE', 'CUSTOMS_AGENT_INVOICE')
  AND (i.`stage` IS NULL OR i.`stage` = '' OR i.`stage` = 'MATERIAL_SUBMIT');

-- ---------- E. 诊断：待恢复流程的申报单（无活跃 Flowable 实例） ----------
-- SELECT f.id, f.form_no, f.status
-- FROM declaration_form f
-- WHERE f.status BETWEEN 1 AND 9
--   AND f.deleted = 0
--   AND NOT EXISTS (
--     SELECT 1 FROM act_ru_execution e
--     INNER JOIN act_ru_execution root ON e.proc_inst_id_ = root.proc_inst_id_
--     WHERE root.business_key_ = CAST(f.id AS CHAR)
--   );
