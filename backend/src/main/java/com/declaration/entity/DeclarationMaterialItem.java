package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * 申报资料项实例（每申报单一组）
 */
@Data
@TableName("declaration_material_item")
public class DeclarationMaterialItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申报单ID */
    private Long formId;

    /** 来源模板ID（NULL 表示单据内手动新增） */
    private Long templateId;

    /** 资料编码 */
    private String code;

    /** 资料显示名 */
    private String name;

    /** 是否必填 0-否 1-是 */
    private Integer required;

    /** 排序 */
    private Integer sort;

    /** 说明 */
    private String remark;

    /** 结构化字段配置（从模板克隆可单据内覆盖） */
    private String formSchema;

    /** 附件名 */
    private String fileName;

    /** 附件下载地址 */
    private String fileUrl;

    /** 上传人ID */
    private Long uploadBy;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 金额（发票类使用） */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 发票号 */
    private String invoiceNo;

    /** 开票日期 */
    private LocalDate invoiceDate;

    /** 其他扩展字段 JSON */
    private String extraData;

    /** 状态 0-未上传 1-已上传 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 行级创建人（新增这行资料项的用户） */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /** 行级最后更新人 */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 创建人昵称（非持久化，Service 层回填） */
    @TableField(exist = false)
    private String createByName;

    /** 更新人昵称（非持久化，Service 层回填） */
    @TableField(exist = false)
    private String updateByName;
}
