package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 资料模板绑定实体（流程 + 运输方式）
 */
@Data
@TableName("declaration_material_template_binding")
public class MaterialTemplateBinding implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 资料模板ID */
    private Long templateId;

    /** 流程模板编码（空=任意流程） */
    private String flowTemplateCode;

    /** 运输方式编码（空=任意运输方式） */
    private String transportModeCode;

    /** 是否必填（null=使用模板默认值，1=必填，0=选填） */
    private Integer required;
}
