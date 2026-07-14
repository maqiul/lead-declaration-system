package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 申报资料项模板（全局）
 */
@Data
@TableName("declaration_material_template")
public class DeclarationMaterialTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 资料编码（唯一） */
    private String code;

    /** 资料显示名 */
    private String name;

    /** 是否必填 0-否 1-是 */
    private Integer required;

    /** 排序 */
    private Integer sort;

    /** 说明 */
    private String remark;

    /** 结构化字段配置 JSON（如发票金额/发票号等） */
    private String formSchema;

    /** 启用 0-停用 1-启用 */
    private Integer enabled;

    /** 所属环节：MATERIAL_SUBMIT-资料上传 / INVOICE-业务发票 / FINANCE_SUPPLEMENT-财务补充 */
    private String stage;

    /** 发票模式: 0-普通附件 1-附件级金额/发票号/日期 */
    private Integer invoiceMode;

    /** 发票分类: DEDUCTION-扣款 INPUT-进项 (仅 invoice_mode=1 时有效) */
    private String invoiceCategory;

    /** 绑定规则（非DB字段，API 返回用） */
    @TableField(exist = false)
    private List<MaterialTemplateBinding> bindings;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
