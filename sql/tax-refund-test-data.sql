-- 退税功能测试数据

-- 1. 插入测试退税申请数据
INSERT INTO `tax_refund_application` (
  `application_no`, `declaration_form_id`, `initiator_id`, `initiator_name`, 
  `department_id`, `department_name`, `application_type`, `amount`, 
  `invoice_no`, `invoice_amount`, `tax_rate`, `description`, `status`,
  `create_by`, `update_by`
) VALUES 
('TR20260317001', 1, 1, '张三', 1, '财务部', 'EXPORT_REFUND', 50000.00, 
 'INV20260317001', 500000.00, 13.00, '出口商品退税申请', 1, 1, 1),

('TR20260317002', 2, 2, '李四', 2, '销售部', 'VAT_REFUND', 30000.00, 
 'INV20260317002', 300000.00, 13.00, '增值税退税申请', 0, 2, 2),

('TR20260317003', 3, 3, '王五', 1, '财务部', 'EXPORT_REFUND', 80000.00, 
 'INV20260317003', 800000.00, 13.00, '大额出口退税申请', 2, 3, 3),

('TR20260317004', 4, 1, '张三', 1, '财务部', 'OTHER_REFUND', 15000.00, 
 'INV20260317004', 150000.00, 13.00, '其他类型退税申请', 4, 1, 1);

-- 2. 插入测试附件数据
INSERT INTO `tax_refund_attachment` (
  `application_id`, `file_name`, `file_path`, `file_size`, `file_type`, 
  `attachment_type`, `description`, `uploader_id`, `uploader_name`
) VALUES 
(1, '发票扫描件.pdf', '/uploads/tax-refund/TR20260317001/invoice.pdf', 2048000, 'application/pdf', 
 'INVOICE', '出口发票扫描件', 1, '张三'),

(1, '合同副本.pdf', '/uploads/tax-refund/TR20260317001/contract.pdf', 1536000, 'application/pdf', 
 'CONTRACT', '出口合同副本', 1, '张三'),

(2, '增值税发票.jpg', '/uploads/tax-refund/TR20260317002/vat_invoice.jpg', 1024000, 'image/jpeg', 
 'INVOICE', '增值税专用发票', 2, '李四'),

(3, '大额出口发票.pdf', '/uploads/tax-refund/TR20260317003/large_export_invoice.pdf', 3072000, 'application/pdf', 
 'INVOICE', '大额出口发票', 3, '王五');

-- 3. 插入测试审核记录
INSERT INTO `tax_refund_review_record` (
  `application_id`, `review_type`, `reviewer_id`, `reviewer_name`, 
  `review_result`, `review_opinion`, `next_approver_id`, `next_approver_name`
) VALUES 
(1, 'FIRST_REVIEW', 2, '李四', 'APPROVED', '资料齐全，符合退税条件，建议通过', 3, '王五'),
(3, 'FIRST_REVIEW', 2, '李四', 'APPROVED', '大额退税申请，资料完整，同意初审通过', 3, '王五'),
(4, 'FIRST_REVIEW', 2, '李四', 'RETURNED', '需要补充出口报关单据', 1, '张三');

-- 4. 插入流程节点记录
INSERT INTO `tax_refund_process_node` (
  `application_id`, `node_type`, `operator_id`, `operator_name`, `operation_result`, `remark`
) VALUES 
(1, 'SUBMIT', 1, '张三', 'SUCCESS', '提交退税申请'),
(1, 'FIRST_REVIEW', 2, '李四', 'APPROVED', '初审通过'),
(2, 'SUBMIT', 2, '李四', 'SUCCESS', '提交退税申请草稿'),
(3, 'SUBMIT', 3, '王五', 'SUCCESS', '提交大额退税申请'),
(3, 'FIRST_REVIEW', 2, '李四', 'APPROVED', '初审通过'),
(4, 'SUBMIT', 1, '张三', 'SUCCESS', '提交退税申请'),
(4, 'FIRST_REVIEW', 2, '李四', 'RETURNED', '退回补充材料');

-- 5. 验证插入的数据
SELECT 
  '退税申请数据' as '数据类型',
  COUNT(*) as '记录数'
FROM tax_refund_application
UNION ALL
SELECT 
  '退税附件数据' as '数据类型',
  COUNT(*) as '记录数'
FROM tax_refund_attachment
UNION ALL
SELECT 
  '审核记录数据' as '数据类型',
  COUNT(*) as '记录数'
FROM tax_refund_review_record
UNION ALL
SELECT 
  '流程节点数据' as '数据类型',
  COUNT(*) as '记录数'
FROM tax_refund_process_node;

-- 6. 查看详细测试数据
SELECT 
  '退税申请详情' as '信息类型',
  application_no as '申请编号',
  initiator_name as '申请人',
  application_type as '申请类型',
  amount as '申请金额',
  status as '状态',
  create_time as '创建时间'
FROM tax_refund_application
ORDER BY create_time DESC;

SELECT 
  '附件详情' as '信息类型',
  a.application_no as '申请编号',
  att.file_name as '文件名',
  att.attachment_type as '附件类型',
  att.file_size as '文件大小',
  att.upload_time as '上传时间'
FROM tax_refund_attachment att
JOIN tax_refund_application a ON att.application_id = a.id
ORDER BY att.upload_time DESC;