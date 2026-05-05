-- 申报资料项增加行级创建人/更新人审计字段
-- 语义区分：
--   upload_by / upload_time  = 谁上传了附件
--   create_by / update_by    = 谁新增/最后修改这行资料项（自定义资料项尤其明显）
ALTER TABLE `declaration_material_item`
  ADD COLUMN `create_by` BIGINT(20) NULL COMMENT '创建人ID' AFTER `update_time`,
  ADD COLUMN `update_by` BIGINT(20) NULL COMMENT '最后更新人ID' AFTER `create_by`;
