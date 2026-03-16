package com.declaration.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.declaration.entity.ExportDataRequest;
import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.apache.fesod.sheet.write.metadata.fill.FillConfig;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.ClassPathResource;


import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Excel导出控制器 - 基于FastExcel实现
 */
@RestController
@RequestMapping("/excel")
public class ExcelExportController {

    /**
     * 导出商业发票和装箱单
     */
    @PostMapping("/export-documents")
    public void exportDocuments(@RequestBody Map<String, Object> requestData, HttpServletResponse response) throws IOException {
        try {
            System.out.println("接收到导出请求: " + requestData);
            
            // 调试信息
            System.out.println("箱子数据: " + requestData.get("cartons"));
            System.out.println("产品数据: " + requestData.get("products"));
            
            // 使用Fesod的模板填充功能
            String templatePath = getTemplatePath();
            if (templatePath == null) {
                throw new RuntimeException("模板文件不存在");
            }
                        // 设置响应头
            String fileName = "Export_Documents_" + System.currentTimeMillis() + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()));
 
            // 准备填充数据
            Map<String, Object> fillData = prepareFillData(requestData);
            List<ExportDataRequest.ProductInfo> productList = createProductListData(requestData);
            
            // 创建ExcelWriter
            try (ExcelWriter writer = FesodSheet.write(response.getOutputStream()).withTemplate(templatePath).build()) {
                
                // 第一个工作表：商业发票
                WriteSheet invoiceSheet = FesodSheet.writerSheet(0).build(); // 第一个sheet
                FillConfig invoiceConfig = FillConfig.builder()
                    .forceNewRow(false)
                    .build();
                
                // 填充商业发票数据
                writer.fill(fillData, invoiceSheet);
                writer.fill(productList, invoiceConfig, invoiceSheet);
                
                // 第二个工作表：装箱单
                WriteSheet packingSheet = FesodSheet.writerSheet(1).build(); // 第二个sheet
                FillConfig packingConfig = FillConfig.builder()
                    .forceNewRow(false)
                    .build();
                
                // 准备装箱单专用数据
                Map<String, Object> packingData = preparePackingListData(requestData);

                
                // 填充装箱单数据
                writer.fill(packingData, packingSheet);
                
                // 使用Fesod的列表填充功能
                writer.fill(productList, packingConfig, packingSheet);
                
                writer.finish();
            }
            

            // 删除临时文件
            // tempFile.delete();

            
            System.out.println("Excel模板填充导出成功");
            
        } catch (Exception e) {
            System.err.println("导出失败: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"code\":500,\"msg\":\"导出失败: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 获取模板路径
     */
    private String getTemplatePath() {
        // 尝试从classpath获取模板
        ClassPathResource resource = new ClassPathResource("templates/temple.xlsx");
        if (resource.exists()) {
            try {
                return resource.getFile().getAbsolutePath();
            } catch (IOException e) {
                System.err.println("获取模板文件路径失败: " + e.getMessage());
            }
        }
        
        // 尝试从文件系统获取模板
        java.io.File templateFile = new java.io.File("templates/temple.xlsx");
        if (templateFile.exists()) {
            return templateFile.getAbsolutePath();
        }
        
        System.err.println("模板文件不存在");
        return null;
    }
    
    /**
     * 准备填充数据
     */
    private Map<String, Object> prepareFillData(Map<String, Object> requestData) {
        Map<String, Object> fillData = new HashMap<>();
        
        // 基本信息
        fillData.put("invoiceNo", requestData.get("invoiceNo"));
        fillData.put("date", requestData.get("date"));
        fillData.put("shipperCompany", requestData.get("shipperCompany"));
        fillData.put("shipperAddress", requestData.get("shipperAddress"));
        fillData.put("consigneeCompany", requestData.get("consigneeCompany"));
        fillData.put("consigneeAddress", requestData.get("consigneeAddress"));
        fillData.put("currency",requestData.get("currency"));
        
        // 运输信息
        fillData.put("transportMode", requestData.get("transportMode"));
        fillData.put("departureCity", requestData.get("departureCity"));
        fillData.put("destinationRegion", requestData.get("destinationRegion"));
        
        // 总计信息
        fillData.put("totalAmount", requestData.get("totalAmount"));
        fillData.put("totalAmountWords", requestData.get("totalAmountWords"));
        fillData.put("totalCartons",requestData.get("totalCartons"));
        fillData.put("totalGrossWeight",requestData.get("totalGrossWeight"));
        fillData.put("totalNetWeight",requestData.get("totalNetWeight"));
        fillData.put("totalVolume",requestData.get("totalVolume"));
        fillData.put("totalQuantity",requestData.get("totalQuantity"));
//        List<Map<String, Object>> products = (List<Map<String, Object>>) requestData.get("products");
//        fillData.put("products",products);
        
        return fillData;
    }
    
     /**
      * 创建产品列表数据
      */
     private List<ExportDataRequest.ProductInfo> createProductListData(Map<String, Object> requestData) {
         List<ExportDataRequest.ProductInfo> productList = new ArrayList<>();
         List<Map<String, Object>> products = (List<Map<String, Object>>) requestData.get("products");

         for (Map<String, Object> product : products) {
             ExportDataRequest.ProductInfo productInfo = new ExportDataRequest.ProductInfo();
             productInfo.setProductName((String) product.get("productName"));
             productInfo.setHsCode((String) product.get("hsCode"));  // 设置HS编码
             // 设置申报要素
             @SuppressWarnings("unchecked")
             List<Map<String, Object>> declarationElements = (List<Map<String, Object>>) product.get("declarationElements");
             productInfo.setDeclarationElements(declarationElements);
             productInfo.setQuantity((Integer) product.get("quantity"));
             productInfo.setUnit((String) product.get("unit"));
             productInfo.setUnitPrice((BigDecimal) product.get("unitPrice"));
             productInfo.setAmount((String) product.get("amount"));
             productInfo.setGrossWeight((BigDecimal) product.get("grossWeight"));
             productInfo.setNetWeight((BigDecimal) product.get("netWeight"));
             productInfo.setCartons((Integer) product.get("cartons"));
             productInfo.setVolume((BigDecimal) product.get("volume"));
             productInfo.setCurrency(requestData.get("currency").toString());
             productInfo.setWgt("KGS");
                         
             // 设置箱子信息
             @SuppressWarnings("unchecked")
             Map<String, Object> cartonInfo = (Map<String, Object>) product.get("cartonInfo");
             if (cartonInfo != null) {
                 productInfo.setCartonNo((String) cartonInfo.get("cartonNo"));
                 productInfo.setCartonQuantity((Integer) cartonInfo.get("cartonQuantity"));
                 productInfo.setCartonVolume(new BigDecimal(cartonInfo.get("cartonVolume").toString()));
             }

             productList.add(productInfo);
         }

         return productList;
     }





    /**
     * 数字转英文大写
     */
    private String convertToWords(double amount) {
        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE"};
        String[] teens = {"TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};
        
        int wholePart = (int) amount;
        
        if (wholePart == 0) return "ZERO";
        if (wholePart < 10) return ones[wholePart];
        if (wholePart < 20) return teens[wholePart - 10];
        if (wholePart < 100) {
            int ten = wholePart / 10;
            int one = wholePart % 10;
            return tens[ten] + (one > 0 ? " " + ones[one] : "");
        }
        if (wholePart < 1000) {
            int hundred = wholePart / 100;
            int remainder = wholePart % 100;
            String result = ones[hundred] + " HUNDRED";
            if (remainder > 0) {
                result += " " + convertToWords(remainder);
            }
            return result;
        }
        return "LARGE AMOUNT";
    }

    /**
     * 格式化日期
     */
    private String formatDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return DateUtil.format(new Date(), "MMMM. dd, yyyy");
        }
        try {
            Date date = DateUtil.parse(dateStr, "yyyy-MM-dd");
            return DateUtil.format(date, "MMMM. dd, yyyy");
        } catch (Exception e) {
            return DateUtil.format(new Date(), "MMMM. dd, yyyy");
        }
    }
    
    /**
     * 准备装箱单填充数据
     */
    private Map<String, Object> preparePackingListData(Map<String, Object> requestData) {
        Map<String, Object> packingData = new HashMap<>();
        
        // 基本信息
        packingData.put("invoiceNo", requestData.get("invoiceNo"));
        packingData.put("date", requestData.get("date"));
        packingData.put("shipperCompany", requestData.get("shipperCompany"));
        packingData.put("shipperAddress", requestData.get("shipperAddress"));
        packingData.put("consigneeCompany", requestData.get("consigneeCompany"));
        packingData.put("consigneeAddress", requestData.get("consigneeAddress"));
        packingData.put("currency", requestData.get("currency"));
        
        // 运输信息
        packingData.put("transportMode", requestData.get("transportMode"));
        packingData.put("departureCity", requestData.get("departureCity"));
        packingData.put("destinationRegion", requestData.get("destinationRegion"));
        
        // 装箱单统计信息
        packingData.put("totalCartons", requestData.get("totalCartons"));
        packingData.put("totalGrossWeight", requestData.get("totalGrossWeight"));
        packingData.put("totalNetWeight", requestData.get("totalNetWeight"));
        packingData.put("totalVolume", requestData.get("totalVolume"));
        packingData.put("totalQuantity", requestData.get("totalQuantity"));
        
        return packingData;
    }
    
    /**
     * 创建装箱单产品数据
     */
    private List<ExportDataRequest.ProductInfo> createPackingListData(Map<String, Object> requestData) {
        // 装箱单可以使用相同的产品数据，或者根据需要进行特殊处理
        return createProductListData(requestData);
    }
    

    
}