package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 合同生成记录实体
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Data
@TableName("contract_generation")
public class ContractGeneration {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 关联的申报单ID
     */
    private Long declarationFormId;

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * 生成的文件名
     */
    private String generatedFileName;

    /**
     * 生成的文件路径
     */
    private String generatedFilePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 生成人
     */
    private Long generatedBy;

    /**
     * 生成时间
     */
    private LocalDateTime generatedTime;

    /**
     * 状态 0-已删除 1-正常
     */
    private Integer status;

    /**
     * 备注
     */
    private String remarks;
}