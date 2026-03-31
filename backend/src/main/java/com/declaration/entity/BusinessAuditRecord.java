package com.declaration.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用业务审核记录实体类
 *
 * @author Administrator
 * @since 2026-03-30
 */
@Data
@TableName("business_audit_record")
public class BusinessAuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务记录主键 (如申报单ID)
     */
    private Long businessId;

    /**
     * 业务类型 (如 DECLARATION_RETURN)
     */
    private String businessType;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请原因
     */
    private String applyReason;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核状态: 0-待审核, 1-通过, 2-驳回
     */
    private Integer auditStatus;

    /**
     * 审核备注/原因
     */
    private String auditRemark;

    /**
     * 审核完成时间
     */
    private LocalDateTime auditTime;

    /**
     * 申请前的原始业务状态
     */
    private Integer preStatus;
}
