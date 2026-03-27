package com.declaration.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Excel导出数据请求实体类
 */
@Data
public class ExportDataRequest {
    
    // 基本信息
    private String shipperCompany;
    private String shipperAddress;
    private String consigneeCompany;
    private String consigneeAddress;
    private String invoiceNo;
    private String date;
    private String transportMode;
    private String departureCity;
    private String destinationRegion;
    private String currency;
    
    // 产品列表
    private List<ProductInfo> products;
    

    
    // 统计信息
    private Integer totalQuantity;
    private Double totalAmount;
    private String totalAmountWords;
    private Integer totalCartons;
    private Double totalGrossWeight;
    private Double totalNetWeight;
    private Double totalVolume;
    
    // 内部类：产品信息
    public static class ProductInfo {
        private String productName;
        private String hsCode;  // HS编码
        private List<Map<String, Object>> declarationElements;  // 申报要素
        private Integer quantity;
        private String unit;
        private BigDecimal unitPrice;
        private String amount;
        private BigDecimal grossWeight;
        private BigDecimal netWeight;
        private Integer cartons;
        private BigDecimal volume;
        private String wgt = "kgs";
        private String cbm = "CBM";
        
        // 箱子信息（扁平化到产品中）
        private String cartonNo;
        private Integer cartonQuantity;
        private BigDecimal cartonVolume;
        private String contonEN;
        public String getCartonType() {
            return contonEN;
        }
        public void setCartonType(String cartonType) {
            this.contonEN = cartonType;
        }
     
        
        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        private String currency;
        // 构造函数
        public ProductInfo() {}
        
        // Getter和Setter方法
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public String getHsCode() {
            return hsCode;
        }
        
        public void setHsCode(String hsCode) {
            this.hsCode = hsCode;
        }
        
        public List<Map<String, Object>> getDeclarationElements() {
            return declarationElements;
        }
        
        public void setDeclarationElements(List<Map<String, Object>> declarationElements) {
            this.declarationElements = declarationElements;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public String getUnit() {
            return unit;
        }
        
        public void setUnit(String unit) {
            this.unit = unit;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
        
        public String getAmount() {
            return amount;
        }
        
        public void setAmount(String amount) {
            this.amount = amount;
        }
        
        public BigDecimal getGrossWeight() {
            return grossWeight;
        }
        
        public void setGrossWeight(BigDecimal grossWeight) {
            this.grossWeight = grossWeight;
        }
        
        public BigDecimal getNetWeight() {
            return netWeight;
        }
        
        public void setNetWeight(BigDecimal netWeight) {
            this.netWeight = netWeight;
        }
        
        public Integer getCartons() {
            return cartons;
        }
        
        public void setCartons(Integer cartons) {
            this.cartons = cartons;
        }
        
        public BigDecimal getVolume() {
            return volume;
        }
        
        public void setVolume(BigDecimal volume) {
            this.volume = volume;
        }

        public String getWgt() {
            return wgt;
        }

        public void setWgt(String wgt) {
            this.wgt = wgt;
        }

        public String getCbm() {
            return cbm;
        }

        public void setCbm(String cbm) {
            this.cbm = cbm;
        }
        
        // 箱子信息的Getter和Setter方法
        public String getCartonNo() {
            return cartonNo;
        }
        
        public void setCartonNo(String cartonNo) {
            this.cartonNo = cartonNo;
        }
        
        public Integer getCartonQuantity() {
            return cartonQuantity;
        }
        
        public void setCartonQuantity(Integer cartonQuantity) {
            this.cartonQuantity = cartonQuantity;
        }
        
        public BigDecimal getCartonVolume() {
            return cartonVolume;
        }
        
        public void setCartonVolume(BigDecimal cartonVolume) {
            this.cartonVolume = cartonVolume;
        }
    }
    
    // 主类的构造函数
    public ExportDataRequest() {}
    
    // Getter和Setter方法
    public String getShipperCompany() {
        return shipperCompany;
    }
    
    public void setShipperCompany(String shipperCompany) {
        this.shipperCompany = shipperCompany;
    }
    
    public String getShipperAddress() {
        return shipperAddress;
    }
    
    public void setShipperAddress(String shipperAddress) {
        this.shipperAddress = shipperAddress;
    }
    
    public String getConsigneeCompany() {
        return consigneeCompany;
    }
    
    public void setConsigneeCompany(String consigneeCompany) {
        this.consigneeCompany = consigneeCompany;
    }
    
    public String getConsigneeAddress() {
        return consigneeAddress;
    }
    
    public void setConsigneeAddress(String consigneeAddress) {
        this.consigneeAddress = consigneeAddress;
    }
    
    public String getInvoiceNo() {
        return invoiceNo;
    }
    
    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTransportMode() {
        return transportMode;
    }
    
    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }
    
    public String getDepartureCity() {
        return departureCity;
    }
    
    public void setDepartureCity(String departureCity) {
        this.departureCity = departureCity;
    }
    
    public String getDestinationRegion() {
        return destinationRegion;
    }
    
    public void setDestinationRegion(String destinationRegion) {
        this.destinationRegion = destinationRegion;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public List<ProductInfo> getProducts() {
        return products;
    }
    
    public void setProducts(List<ProductInfo> products) {
        this.products = products;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getTotalAmountWords() {
        return totalAmountWords;
    }
    
    public void setTotalAmountWords(String totalAmountWords) {
        this.totalAmountWords = totalAmountWords;
    }
    
    public Integer getTotalCartons() {
        return totalCartons;
    }
    
    public void setTotalCartons(Integer totalCartons) {
        this.totalCartons = totalCartons;
    }
    
    public Double getTotalGrossWeight() {
        return totalGrossWeight;
    }
    
    public void setTotalGrossWeight(Double totalGrossWeight) {
        this.totalGrossWeight = totalGrossWeight;
    }
    
    public Double getTotalNetWeight() {
        return totalNetWeight;
    }
    
    public void setTotalNetWeight(Double totalNetWeight) {
        this.totalNetWeight = totalNetWeight;
    }
    
    public Double getTotalVolume() {
        return totalVolume;
    }
    
    public void setTotalVolume(Double totalVolume) {
        this.totalVolume = totalVolume;
    }
}