package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 出口申报单实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@TableName("declaration_form")
public class DeclarationForm {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申报单号
     */
    private String formNo;

    /**
     * 发货人公司名
     */
    private String shipperCompany;

    /**
     * 发货人地址
     */
    private String shipperAddress;

    /**
     * 收货人公司名
     */
    private String consigneeCompany;

    /**
     * 收货人地址
     */
    private String consigneeAddress;

    /**
     * 发票号
     */
    private String invoiceNo = "";

    /**
     * 申报日期
     */
    private LocalDate declarationDate;

    /**
     * 运输方式
     */
    private String transportMode;

    /**
     * 出发城市
     */
    private String departureCity;

    /**
     * 目的地区域
     */
    private String destinationRegion;

    /**
     * 币种
     */
    private String currency;

    /**
     * 总数量
     */
    private Integer totalQuantity;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 总箱数
     */
    private Integer totalCartons;

    /**
     * 总毛重(KGS)
     */
    private BigDecimal totalGrossWeight;

    /**
     * 总净重(KGS)
     */
    private BigDecimal totalNetWeight;

    /**
     * 总体积(CBM)
     */
    private BigDecimal totalVolume;

    /**
     * 状态 0-草稿 1-已提交 2-已审核 3-已完成
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 删除标志 0-正常 1-删除
     */
    @TableLogic
    private Integer delFlag;

    /**
     * 附件列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationAttachment> attachments;

    /**
     * 产品明细列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationProduct> products;

    /**
     * 箱子信息列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationCarton> cartons;

    /**
     * 箱子产品关联列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationCartonProduct> cartonProducts;

    /**
     * 水单信息列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<DeclarationRemittance> remittances;
}