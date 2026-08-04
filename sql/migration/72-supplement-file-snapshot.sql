-- ============================================================
-- 72: 补交文件快照表
-- 背景：补交审核通过后附件 supplement_id 清标转正、驳回后增量删除，
--       "哪一次补交了哪些文件"的关联会丢失。
-- 方案：审核结果落地时（通过/驳回均记录）将本次补交的增量文件快照留档。
-- ============================================================

CREATE TABLE IF NOT EXISTS declaration_material_supplement_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    supplement_id BIGINT       NOT NULL COMMENT '补交单ID（declaration_material_supplement.id）',
    form_id       BIGINT       NOT NULL COMMENT '申报单ID',
    item_id       BIGINT       DEFAULT NULL COMMENT '资料项实例ID（驳回删除后仅作历史引用）',
    item_name     VARCHAR(255) DEFAULT NULL COMMENT '资料项名称快照',
    attachment_id BIGINT       DEFAULT NULL COMMENT '附件ID（驳回删除后仅作历史引用）',
    file_name     VARCHAR(500) DEFAULT NULL COMMENT '文件名快照',
    file_url      VARCHAR(1000) DEFAULT NULL COMMENT '下载地址快照',
    file_size     BIGINT       DEFAULT NULL COMMENT '文件大小(byte)',
    stage         VARCHAR(64)  DEFAULT NULL COMMENT '所属环节快照',
    upload_by     BIGINT       DEFAULT NULL COMMENT '上传人ID',
    upload_time   DATETIME     DEFAULT NULL COMMENT '上传时间',
    create_time   DATETIME     DEFAULT NULL COMMENT '快照时间（审核落地时间）',
    PRIMARY KEY (id),
    KEY idx_supplement_id (supplement_id),
    KEY idx_form_id (form_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '资料补交文件快照（每次补交审核落地时留档）';
