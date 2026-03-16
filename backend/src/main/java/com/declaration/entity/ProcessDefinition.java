package com.declaration.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 流程定义实体类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "流程定义对象", name = "流程定义表")
public class ProcessDefinition extends BaseEntity {


    @Schema(description = "流程定义名称")
    private String name;

    @Schema(description = "流程定义KEY")
    private String processKey;

    @Schema(description = "流程名称")
    private String processName;

    @Schema(description = "流程XML内容")
    private String bpmnXml;

    @Schema(description = "状态 0:停用 1:启用")
    private Integer status;

    @Schema(description = "流程定义版本")
    private int version;

    @Schema(description = "部署ID")
    private String deploymentId;

    @Schema(description = "流程定义描述")
    private String description;

    @Schema(description = "分类")
    private String category;
}