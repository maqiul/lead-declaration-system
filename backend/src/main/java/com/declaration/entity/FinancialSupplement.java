package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("financial_supplement")
@Schema(description = "财务开票补充表")
public class FinancialSupplement {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联申报单ID")
    private Long formId;

    @Schema(description = "关联申报单号")
    private String formNo;

    @Schema(description = "货代金额")
    private BigDecimal freightAmount;

    @Schema(description = "货代发票号")
    private String freightInvoiceNo;

    @Schema(description = "货代发票文件名")
    private String freightFileName;

    @Schema(description = "货代发票URL")
    private String freightFileUrl;

    @Schema(description = "报关代理金额")
    private BigDecimal customsAmount;

    @Schema(description = "报关单发票号")
    private String customsInvoiceNo;

    @Schema(description = "报关代理发票文件名")
    private String customsFileName;

    @Schema(description = "报关代理发票URL")
    private String customsFileUrl;

    @Schema(description = "海关回执文件名")
    private String customsReceiptFileName;

    @Schema(description = "海关回执文件URL")
    private String customsReceiptFileUrl;

    @Schema(description = "退税金额")
    private BigDecimal taxRefundAmount;

    @Schema(description = "退税点(%)")
    private BigDecimal taxRefundRate;

    @Schema(description = "外汇银行ID")
    private Long foreignExchangeBankId;

    @Schema(description = "外汇银行名称")
    private String foreignExchangeBank;

    @Schema(description = "外汇银行手续费率(%)")
    private BigDecimal bankFeeRate;

    @Schema(description = "开票明细金额")
    private BigDecimal detailsAmount;

    @Schema(description = "开票号")
    private String detailsInvoiceNo;

    @Schema(description = "开票明细文件名")
    private String detailsFileName;

    @Schema(description = "开票明细URL")
    private String detailsFileUrl;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "状态 0-待上传, 1-已提交")
    private Integer status;

    @Schema(description = "计算明细JSON")
    private String calculationDetail;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
}
