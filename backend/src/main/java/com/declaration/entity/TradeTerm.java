package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 贸易方式(Incoterms)配置实体
 */
@Data
@TableName("trade_term")
public class TradeTerm {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 贸易方式代码(如 EXW/FOB/CIF) */
    private String code;

    /** 英文名称 */
    private String name;

    /** 中文名称 */
    private String chineseName;

    /** 适用运输方式(展示用,如"任何运输方式"/"仅海运/内河") */
    private String transportScope;

    /** 分组名称(如 E组/F组/C组/D组) */
    private String groupName;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态 0-禁用 1-启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableLogic
    private Integer delFlag;

    /** 关联的运输方式代码列表(非数据库字段) */
    @TableField(exist = false)
    private List<String> transportModes;
}
