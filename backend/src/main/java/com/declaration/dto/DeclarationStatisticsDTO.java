package com.declaration.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 申报单统计数据DTO
 */
@Data
public class DeclarationStatisticsDTO {

    /**
     * 总申报单数
     */
    private Long totalForms;

    /**
     * 本月申报数
     */
    private Long monthForms;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 平均金额
     */
    private BigDecimal avgAmount;

    /**
     * 按状态统计
     */
    private List<StatusStat> statusStats;

    /**
     * 按产品统计
     */
    private List<ProductStat> productStats;

    /**
     * 按目的地统计
     */
    private List<DestinationStat> destinationStats;

    /**
     * 状态统计项
     */
    @Data
    public static class StatusStat {
        private String status;
        private Long count;
        private BigDecimal amount;

        public StatusStat() {}

        public StatusStat(String status, Long count, BigDecimal amount) {
            this.status = status;
            this.count = count;
            this.amount = amount;
        }
    }

    /**
     * 产品统计项
     */
    @Data
    public static class ProductStat {
        private String productName;
        private String hsCode;
        private Long count;
        private BigDecimal totalAmount;

        public ProductStat() {}

        public ProductStat(String productName, String hsCode, Long count, BigDecimal totalAmount) {
            this.productName = productName;
            this.hsCode = hsCode;
            this.count = count;
            this.totalAmount = totalAmount;
        }
    }

    /**
     * 目的地统计项
     */
    @Data
    public static class DestinationStat {
        private String destination;
        private Long count;
        private BigDecimal totalAmount;

        public DestinationStat() {}

        public DestinationStat(String destination, Long count, BigDecimal totalAmount) {
            this.destination = destination;
            this.count = count;
            this.totalAmount = totalAmount;
        }
    }
}
