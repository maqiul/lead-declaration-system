package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报关发票实体类
 */
@Data
@TableName("financial_customs_invoice")
public class FinancialCustomsInvoice {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long declarationFormId;
    
    private String declarationFormCode;
    
    private String invoiceNo;
    
    private LocalDate invoiceDate;
    
    private BigDecimal amount;
    
    private String currency;
    
    private String customsBroker;
    
    private String brokerTaxNo;
    
    private String invoiceFilePath;
    
    private String invoiceFileName;
    
    /**
     * 状态 0:待审核 1:已审核 2:已驳回
     */
    private Integer status;
    
    private Long auditUserId;
    
    private String auditUserName;
    
    private LocalDateTime auditTime;
    
    private String auditRemark;
    
    private String remark;
    
    private Long orgId;
    
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}