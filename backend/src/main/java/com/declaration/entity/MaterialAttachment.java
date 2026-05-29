package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资料项附件（多文件）
 */
@Data
@TableName("declaration_material_attachment")
public class MaterialAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 资料项实例ID */
    private Long itemId;

    /** 原始文件名 */
    private String fileName;

    /** 下载地址 */
    private String fileUrl;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 发票金额（仅发票类资料项使用） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 发票号 */
    private String invoiceNo;

    /** 开票日期 */
    private LocalDate invoiceDate;

    /** 扩展字段 JSON */
    private String extraData;

    /** 上传人ID */
    private Long uploadBy;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 创建人ID */
    private Long createBy;

    /** 更新人ID */
    private Long updateBy;

    /** 所属环节 */
    private String stage;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 上传人显示名（非持久化，服务层回填） */
    @TableField(exist = false)
    private String uploadByName;

    /** 创建人显示名（非持久化） */
    @TableField(exist = false)
    private String createByName;

    /** 更新人显示名（非持久化） */
    @TableField(exist = false)
    private String updateByName;
}
