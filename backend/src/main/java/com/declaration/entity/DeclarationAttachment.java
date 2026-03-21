package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 申报单附件实体类
 */
@Data
@TableName("declaration_attachment")
public class DeclarationAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申报单ID
     */
    private Long formId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件下载路径
     */
    private String fileUrl;

    /**
     * 文件类型 (Invoice, PackingList, etc.)
     */
    private String fileType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
