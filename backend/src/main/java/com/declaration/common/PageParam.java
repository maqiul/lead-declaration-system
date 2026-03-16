package com.declaration.common;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.Serializable;

/**
 * 分页查询参数基类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
public class PageParam implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer current = 1;
    
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 10;
    
    private String orderBy;
    
    private String orderDirection = "ASC";
}