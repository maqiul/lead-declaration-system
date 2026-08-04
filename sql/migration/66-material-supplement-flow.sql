-- 66-material-supplement-flow.sql
-- 独立资料补交流程：申报单资料提交后可发起补交，增量资料审核通过才转正
-- 权限点 business:declaration:supplement:initiate 已在 65-declaration-data-scope-permission.sql 种子

-- ---------- 1. 补交记录主表 ----------
CREATE TABLE IF NOT EXISTS `declaration_material_supplement` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `form_id`       BIGINT       NOT NULL COMMENT '申报单ID',
    `reason`        VARCHAR(500) NULL COMMENT '补交原因',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-补交中 1-通过 2-驳回',
    `initiator_id`  BIGINT       NULL COMMENT '发起人ID',
    `auditor_id`    BIGINT       NULL COMMENT '审核人ID',
    `audit_remark`  VARCHAR(500) NULL COMMENT '审核备注',
    `create_time`   DATETIME     NULL COMMENT '发起时间',
    `audit_time`    DATETIME     NULL COMMENT '审核时间',
    PRIMARY KEY (`id`),
    KEY `idx_form_status` (`form_id`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='资料补交记录（独立于主流程）';

-- ---------- 2. 附件表增加补交标记（非空=补交增量，审核通过后清除转正） ----------
ALTER TABLE `declaration_material_attachment`
    ADD COLUMN `supplement_id` BIGINT NULL COMMENT '所属补交单ID（非空=补交增量）' AFTER `stage`;

ALTER TABLE `declaration_material_attachment`
    ADD KEY `idx_supplement` (`supplement_id`);

-- ---------- 3. 资料项表增加补交标记（补交新增的资料项） ----------
ALTER TABLE `declaration_material_item`
    ADD COLUMN `supplement_id` BIGINT NULL COMMENT '所属补交单ID（非空=补交增量）' AFTER `status`;

ALTER TABLE `declaration_material_item`
    ADD KEY `idx_supplement` (`supplement_id`);
