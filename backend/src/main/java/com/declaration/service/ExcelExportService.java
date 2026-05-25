package com.declaration.service;

import com.declaration.entity.DeclarationAttachment;
import com.declaration.entity.DeclarationForm;

/**
 * Excel导出服务类
 */
public interface ExcelExportService {

    /**
     * 为申报单生成全套单证并保存
     *
     * @param form 完整的申报单信息（包含产品和箱子）
     * @return 生成的附件信息
     */
    DeclarationAttachment generateAndSaveExportDocuments(DeclarationForm form) throws java.io.IOException;

    /**
     * 生成并保存水单记录Excel
     *
     * @param remittance 水单信息
     * @param form       申报单
     * @return 附件记录
     */
    DeclarationAttachment generateAndSaveRemittanceReport(com.declaration.entity.DeclarationRemittance remittance, DeclarationForm form) throws java.io.IOException;

    /**
     * 为申报单生成全套单证(基于alltemple_template.xlsx)并保存
     *
     * @param form           完整的申报单信息
     * @param mergeProducts  是否合并同款商品（按 中文名称+英文名称+HS编码+单价 分组聚合）
     */
    DeclarationAttachment generateAndSaveAllTempleExportDocuments(DeclarationForm form, boolean mergeProducts) throws java.io.IOException;

    /**
     * 获取发票模板路径（供 Controller 层调用）
     *
     * @return 模板文件绝对路径，未找到返回 null
     */
    String getInvoiceTemplatePath();

}
