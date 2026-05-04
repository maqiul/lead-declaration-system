package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
