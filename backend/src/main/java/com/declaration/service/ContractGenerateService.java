package com.declaration.service;

import com.declaration.entity.ContractGeneration;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.Map;

/**
 * 合同生成服务接口
 *
 * @author Administrator
 * @since 2026-03-17
 */
public interface ContractGenerateService {

    /**
     * 生成合同文档
     *
     * @param templateId 模板ID
     * @param declarationFormId 申报单ID
     * @param dataMap 模板数据
     * @param generatedBy 生成人
     * @return 合同生成记录
     */
    ContractGeneration generateContract(Long templateId, Long declarationFormId, Map<String, Object> dataMap, Long generatedBy);

    /**
     * 上传合同模板文件
     *
     * @param file 模板文件
     * @param templateId 模板ID
     * @return 文件保存路径
     */
    String uploadTemplateFile(MultipartFile file, Long templateId);

    /**
     * 获取合同文件
     *
     * @param contractId 合同ID
     * @return 合同文件
     */
    File getContractFile(Long contractId);

    /**
     * 生成合同编号
     *
     * @param templateType 模板类型
     * @return 合同编号
     */
    String generateContractNumber(String templateType);
}