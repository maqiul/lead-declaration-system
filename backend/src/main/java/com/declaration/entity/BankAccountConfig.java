package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 银行账户配置实体
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Data
@TableName("bank_account_config")
public class BankAccountConfig {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 银行名称
     */
    private String bankName;

    /**
     * 银行代码
     */
    private String bankCode;

    /**
     * 银行账号
     */
    private String accountNumber;

    /**
     * SWIFT代码
     */
    private String swiftCode;

    /**
     * IBAN号码
     */
    private String iban;

    /**
     * 账户持有人
     */
    private String accountHolder;

    /**
     * 账户币种
     */
    private String currency;

    /**
     * 支行名称
     */
    private String branchName;

    /**
     * 支行地址
     */
    private String branchAddress;

    /**
     * 是否默认账户 0-否 1-是
     */
    private Integer isDefault;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 手续费率（如 0.001 表示千分之一）
     */
    private BigDecimal serviceFeeRate;

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
}