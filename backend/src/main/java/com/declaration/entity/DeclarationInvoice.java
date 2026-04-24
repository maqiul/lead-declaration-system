package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("declaration_invoice")
@Schema(description = "发票记录表")
public class DeclarationInvoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联申报单ID (业务发票必填，财务发票选填)")
    private Long formId;

    @Schema(description = "发票大类: 1-业务发票, 2-财务留底")
    private Integer category;

    @Schema(description = "发票类型: 1-进项发票, 2-出项发票")
    private Integer invoiceType;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票名称")
    private String invoiceName;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "不含税金额")
    private BigDecimal amount;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "价税合计")
    private BigDecimal totalAmount;

    @Schema(description = "税率(%)")
    private BigDecimal taxRate;

    @Schema(description = "购方名称")
    private String buyerName;

    @Schema(description = "销方名称")
    private String sellerName;

    @Schema(description = "发票文件URL")
    private String fileUrl;

    @Schema(description = "发票文件名")
    private String fileName;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "申报单号 (非数据库字段,用于前端展示)")
    @TableField(exist = false)
    private String formNo;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
