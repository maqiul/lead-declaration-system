package com.declaration.entity;

import lombok.Data;

/**
 * 申报要素实体
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
public class DeclarationElement {
    
    /**
     * 要素键值（如0, 1, 2...）
     */
    private String key;
    
    /**
     * 要素标签（如用途、品牌、型号等）
     */
    private String label;
    
    /**
     * 要素类型（text/select/checkbox）
     */
    private String type;
    
    /**
     * 要素值
     */
    private String value;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 是否可编辑
     */
    private Boolean editable = false;
    
    /**
     * 占位符文本
     */
    private String placeholder;
    
    /**
     * 选项列表（用于select类型）
     */
    private String[] options;
    
    /**
     * 是否必填
     */
    private Boolean required;
}