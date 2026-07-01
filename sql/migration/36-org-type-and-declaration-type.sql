-- 组织表新增机构类型字段
ALTER TABLE `sys_org`
  ADD COLUMN `org_type` VARCHAR(20) DEFAULT 'EXTERNAL' COMMENT '机构类型: INTERNAL-内部机构, EXTERNAL-外部机构' AFTER `status`;

-- 申报单表新增申报类型字段
ALTER TABLE `declaration_form`
  ADD COLUMN `declaration_type` VARCHAR(20) DEFAULT 'EXTERNAL' COMMENT '申报类型: SELF-自用(内部), EXTERNAL-外部';
