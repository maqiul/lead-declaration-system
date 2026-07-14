-- ============================================================
-- form_section 字典项扩展 remark JSON 配置
-- 为资料类区块（material/supplement/invoice）添加 UI 配置和流程映射
-- submitKey:            Flowable 提交任务 key
-- auditTaskKey:         Flowable 审核任务 key
-- templateStage:        资料模板的 stage 字段值
-- auditBt:              审核记录 business_type
-- attachmentMode:       附件判断模式 fileUrl / attachment
-- requireAnyAttachment: 是否要求至少上传一个附件
-- checkSchema:          是否校验结构化字段
-- checkApprovedRemittance: 审核通过前是否校验已审核收汇水单
-- ============================================================

UPDATE sys_dict_item
SET remark = '{"sectionTitle":"申报资料","cardTitle":"资料上传进度","submitKey":"materialSubmit","auditTaskKey":"materialAudit","btnText":"提交资料审核","btnColor":"#FF8F00","templateStage":"MATERIAL_SUBMIT","auditBt":"DECLARATION_MATERIAL_AUDIT","attachmentMode":"fileUrl","requireAnyAttachment":false,"checkSchema":true,"checkApprovedRemittance":false}'
WHERE dict_code = 'form_section' AND item_value = 'material';

UPDATE sys_dict_item
SET remark = '{"sectionTitle":"补充资料","cardTitle":"补充资料上传进度","submitKey":"supplementSubmit","auditTaskKey":"supplementAudit","btnText":"提交补充资料审核","btnColor":"#FF8F00","templateStage":"SUPPLEMENT","auditBt":"DECLARATION_SUPPLEMENT_AUDIT","attachmentMode":"attachment","requireAnyAttachment":true,"checkSchema":false,"checkApprovedRemittance":false}'
WHERE dict_code = 'form_section' AND item_value = 'supplement';

UPDATE sys_dict_item
SET remark = '{"sectionTitle":"发票资料","cardTitle":"发票上传进度","submitKey":"invoiceSubmit","auditTaskKey":"invoiceAudit","btnText":"提交发票审核","btnColor":"#FF8F00","templateStage":"INVOICE","auditBt":"DECLARATION_INVOICE_AUDIT","attachmentMode":"attachment","requireAnyAttachment":true,"checkSchema":false,"checkApprovedRemittance":true}'
WHERE dict_code = 'form_section' AND item_value = 'invoice';
