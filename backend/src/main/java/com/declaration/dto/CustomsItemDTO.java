package com.declaration.dto;

import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.apache.fesod.sheet.annotation.format.NumberFormat;

import java.math.BigDecimal;

@Data
public class CustomsItemDTO {

    @ExcelProperty("项号")
    @NumberFormat("#")
    private Integer no;

    @ExcelProperty("商品编号")
    private String hsCode;

    @ExcelProperty("商品名称")
    private String nameCh;

    @ExcelProperty("规格型号")
    private String specAndModel;

    @ExcelProperty("数量")
    private String quantityStr; // 你的数据里是 "1 个"

    @ExcelProperty("单价")
    @NumberFormat("#.######")
    private BigDecimal unitPrice;

    @ExcelProperty("总价")
    @NumberFormat("#.######")
    private BigDecimal totalPrice;

    @ExcelProperty("币制")
    private String currency;

    @ExcelProperty("原产国")
    private String originCountry;

    @ExcelProperty("目的国")
    private String destinationCountry;

    @ExcelProperty("货源地")
    private String sourceRegion;

    @ExcelProperty("征免")
    private String exemptionType;

    @ExcelProperty("数量")
    private String statQuantity;

}
