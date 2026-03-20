package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 税务退费申请附件实体
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Data
@TableName("tax_refund_attachment")
public class TaxRefundAttachment {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 申请ID
     */
    private Long applicationId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 附件类型(INVOICE:发票,CONTRACT:合同,OTHER:其他)
     */
    private String attachmentType;

    /**
     * 附件描述
     */
    private String description;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 上传人ID
     */
    private Long uploaderId;

    /**
     * 上传人姓名
     */
    private String uploaderName;

    /**
     * 删除标识
     */
    private Integer deleted;
}