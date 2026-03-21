-- 为退税申请表添加组织ID字段
ALTER TABLE tax_refund_application ADD COLUMN org_id BIGINT DEFAULT NULL COMMENT '所属组织ID' AFTER initiator_name;

-- 创建索引
CREATE INDEX idx_tax_refund_org_id ON tax_refund_application (org_id);

-- 更新已有数据的 org_id（根据申请人关联）
UPDATE tax_refund_application t 
SET t.org_id = (SELECT u.org_id FROM sys_user u WHERE u.id = t.initiator_id)
WHERE t.org_id IS NULL AND t.initiator_id IS NOT NULL;
