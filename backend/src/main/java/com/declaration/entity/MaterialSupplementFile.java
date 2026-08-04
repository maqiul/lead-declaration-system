package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 资料补交文件快照
 * 补交审核落地时（通过/驳回）将本次补交的增量文件留档，
 * 避免通过后 supplement_id 清标、驳回后增量删除导致"哪次补交了哪些文件"丢失
 */
@Data
@TableName("declaration_material_supplement_file")
public class MaterialSupplementFile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 补交单ID */
    private Long supplementId;

    /** 申报单ID */
    private Long formId;

    /** 资料项实例ID（驳回删除后仅作历史引用） */
    private Long itemId;

    /** 资料项名称快照 */
    private String itemName;

    /** 附件ID（驳回删除后仅作历史引用） */
    private Long attachmentId;

    /** 文件名快照 */
    private String fileName;

    /** 下载地址快照 */
    private String fileUrl;

    /** 文件大小(byte) */
    private Long fileSize;

    /** 所属环节快照 */
    private String stage;

    /** 上传人ID */
    private Long uploadBy;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    /** 快照时间（审核落地时间） */
    private LocalDateTime createTime;

    /** 上传人显示名（非持久化，服务层回填） */
    @TableField(exist = false)
    private String uploadByName;
}
