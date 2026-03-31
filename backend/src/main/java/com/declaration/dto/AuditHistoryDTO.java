package com.declaration.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审核历史记录 DTO（包含用户名称）
 *
 * @author Administrator
 * @since 2026-03-30
 */
@Data
public class AuditHistoryDTO {
    
    /**
     * 主键 ID
     */
    private Long id;
    
    /**
     * 业务记录主键 (如申报单 ID)
     */
    private Long businessId;
    
    /**
     * 业务类型 (如 DECLARATION_RETURN)
     */
    private String businessType;
    
    /**
     * 申请人 ID
     */
    private Long applicantId;
    
    /**
     * 申请人名称
     */
    private String applicantName;
    
    /**
     * 申请原因
     */
    private String applyReason;
    
    /**
     * 申请时间
     */
    private LocalDateTime applyTime;
    
    /**
     * 审核人 ID
     */
    private Long auditorId;
    
    /**
     * 审核人名称
     */
    private String auditorName;
    
    /**
     * 审核状态：0-待审核，1-通过，2-驳回
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
