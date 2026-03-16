package com.declaration.dto;

import lombok.Data;

/**
 * 配置选项数据传输对象
 *
 * @author Administrator
 * @since 2026-03-14
 */
@Data
public class ConfigOptionDTO {
    /**
     * 选项显示标签
     */
    private String label;
    
    /**
     * 选项值
     */
    private String value;
    
    /**
     * 是否禁用
     */
    private Boolean disabled = false;
    
    /**
     * 分组名称（用于分组下拉框）
     */
    private String group;
}