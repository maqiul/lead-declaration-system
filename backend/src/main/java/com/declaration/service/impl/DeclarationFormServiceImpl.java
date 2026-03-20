package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.DeclarationProductDao;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationProduct;
import com.declaration.entity.DeclarationCarton;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.entity.DeclarationElementValue;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.entity.DeclarationAttachment;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationProductService;
import com.declaration.service.DeclarationCartonService;
import com.declaration.service.DeclarationCartonProductService;
import com.declaration.service.DeclarationElementValueService;
import com.declaration.service.DeclarationRemittanceService;
import com.declaration.service.DeclarationAttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出口申报单服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationFormServiceImpl extends ServiceImpl<DeclarationFormDao, DeclarationForm> implements DeclarationFormService {

    private final DeclarationProductService productService;
    private final DeclarationCartonService cartonService;
    private final DeclarationCartonProductService cartonProductService;
    private final DeclarationElementValueService elementValueService;
    private final DeclarationRemittanceService remittanceService;
    private final DeclarationAttachmentService attachmentService;
    private final DeclarationProductDao declarationProductDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDeclarationForm(DeclarationForm form) {
        try {
            // 生成申报单号
            if (form.getFormNo() == null || form.getFormNo().isEmpty()) {
                form.setFormNo(generateFormNo());
            }
            
            // 设置申报日期
            if (form.getDeclarationDate() == null) {
                form.setDeclarationDate(LocalDate.now());
            }
            
            // 设置默认状态
            if (form.getStatus() == null) {
                form.setStatus(0); // 草稿状态
            }
            
            // 设置组织ID (如果未提供)
            if (form.getOrgId() == null && StpUtil.isLogin()) {
                Object userOrgId = StpUtil.getSession().get("orgId");
                if (userOrgId != null) {
                    try {
                        Long orgId = Long.valueOf(userOrgId.toString());
                        form.setOrgId(orgId);
                        log.info("设置申报单组织ID: {}", orgId);
                    } catch (NumberFormatException e) {
                        log.warn("组织ID格式错误: {}", userOrgId);
                    }
                } else {
                    log.warn("用户会话中未找到组织ID");
                }
            } else if (form.getOrgId() != null) {
                log.info("申报单已提供组织ID: {}", form.getOrgId());
            }
            
            // 保存申报单主表
            boolean saved = this.save(form);
            if (!saved) {
                return false;
            }
            
            // 保存箱子信息
            List<DeclarationCarton> cartons = form.getCartons();
            Map<Long, Long> cartonIdMap = new HashMap<>(); // 临时ID到真实ID的映射
            if (cartons != null && !cartons.isEmpty()) {
                for (int i = 0; i < cartons.size(); i++) {
                    DeclarationCarton carton = cartons.get(i);
                    Long tempId = carton.getId(); // 临时ID
                    carton.setId(null); // 重要：设置为null，通过自增生成新ID
                    carton.setFormId(form.getId());
                    carton.setSortOrder(i);
                    cartonService.save(carton);
                    cartonIdMap.put(tempId, carton.getId()); // 建立映射
                }
            }
            
            // 保存产品明细
            List<DeclarationProduct> products = form.getProducts();
            Map<Long, Long> productIdMap = new HashMap<>(); // 临时ID到真实ID的映射
            if (products != null && !products.isEmpty()) {
                for (int i = 0; i < products.size(); i++) {
                    DeclarationProduct product = products.get(i);
                    Long tempId = product.getId(); // 临时ID
                    product.setId(null); // 重要：设置为null，通过自增生成新ID
                    product.setFormId(form.getId());
                    product.setSortOrder(i);
                    productService.save(product);
                    productIdMap.put(tempId, product.getId()); // 建立映射
                    
                    // 保存申报要素建议
                    List<DeclarationElementValue> elementValues = product.getElementValues();
                    if (elementValues != null && !elementValues.isEmpty()) {
                        for (DeclarationElementValue elementValue : elementValues) {
                            elementValue.setId(null); // 重要：设置为null
                            elementValue.setProductId(product.getId());
                            elementValueService.save(elementValue);
                        }
                    }
                }
            }
            
            // 保存箱子产品关联
            List<DeclarationCartonProduct> cartonProducts = form.getCartonProducts();
            if (cartonProducts != null && !cartonProducts.isEmpty() && !cartonIdMap.isEmpty()) {
                // 先删除原有的关联关系
                cartonProductService.lambdaUpdate()
                        .in(DeclarationCartonProduct::getCartonId, 
                            cartonIdMap.values().toArray())
                        .remove();
                
                // 保存新的关联关系，使用映射后的实际ID
                for (DeclarationCartonProduct cartonProduct : cartonProducts) {
                    Long realCartonId = cartonIdMap.get(cartonProduct.getCartonId());
                    Long realProductId = productIdMap.get(cartonProduct.getProductId());
                    
                    if (realCartonId != null && realProductId != null) {
                        DeclarationCartonProduct newCartonProduct = new DeclarationCartonProduct();
                        newCartonProduct.setCartonId(realCartonId);
                        newCartonProduct.setProductId(realProductId);
                        newCartonProduct.setQuantity(cartonProduct.getQuantity());
                        cartonProductService.save(newCartonProduct);
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("保存申报单失败", e);
            throw new RuntimeException("保存申报单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDeclarationForm(DeclarationForm form) {
        try {
            if (form.getId() == null) {
                return false;
            }

            // 1. 更新主表
            boolean updated = this.updateById(form);
            if (!updated) {
                return false;
            }

            // 2. 清理旧的关联数据
            // 删除产品及相关的申报要素
            List<DeclarationProduct> oldProducts = productService.lambdaQuery()
                    .eq(DeclarationProduct::getFormId, form.getId())
                    .list();
            if (!oldProducts.isEmpty()) {
                Object[] productIds = oldProducts.stream().map(DeclarationProduct::getId).toArray();
                elementValueService.lambdaUpdate()
                        .in(DeclarationElementValue::getProductId, productIds)
                        .remove();
                productService.removeByIds(Arrays.asList(productIds));
            }

            // 删除箱子及相关的关联关系
            List<DeclarationCarton> oldCartons = cartonService.lambdaQuery()
                    .eq(DeclarationCarton::getFormId, form.getId())
                    .list();
            if (!oldCartons.isEmpty()) {
                Object[] cartonIds = oldCartons.stream().map(DeclarationCarton::getId).toArray();
                cartonProductService.lambdaUpdate()
                        .in(DeclarationCartonProduct::getCartonId, cartonIds)
                        .remove();
                cartonService.removeByIds(Arrays.asList(cartonIds));
            }

            // 3. 重新保存新的关联数据 (复用保存逻辑的部分代码，但需要跳过主表保存)
            // 保存箱子信息
            List<DeclarationCarton> cartons = form.getCartons();
            Map<Long, Long> cartonIdMap = new HashMap<>(); 
            if (cartons != null && !cartons.isEmpty()) {
                for (int i = 0; i < cartons.size(); i++) {
                    DeclarationCarton carton = cartons.get(i);
                    Long tempId = carton.getId(); 
                    carton.setId(null); 
                    carton.setFormId(form.getId());
                    carton.setSortOrder(i);
                    cartonService.save(carton);
                    cartonIdMap.put(tempId, carton.getId()); 
                }
            }
            
            // 保存产品明细
            List<DeclarationProduct> products = form.getProducts();
            Map<Long, Long> productIdMap = new HashMap<>(); 
            if (products != null && !products.isEmpty()) {
                for (int i = 0; i < products.size(); i++) {
                    DeclarationProduct product = products.get(i);
                    Long tempId = product.getId(); 
                    product.setId(null); 
                    product.setFormId(form.getId());
                    product.setSortOrder(i);
                    productService.save(product);
                    productIdMap.put(tempId, product.getId()); 
                    
                    List<DeclarationElementValue> elementValues = product.getElementValues();
                    if (elementValues != null && !elementValues.isEmpty()) {
                        for (DeclarationElementValue elementValue : elementValues) {
                            elementValue.setId(null); 
                            elementValue.setProductId(product.getId());
                            elementValueService.save(elementValue);
                        }
                    }
                }
            }
            
            // 保存箱子产品关联
            List<DeclarationCartonProduct> cartonProducts = form.getCartonProducts();
            if (cartonProducts != null && !cartonProducts.isEmpty() && !cartonIdMap.isEmpty()) {
                for (DeclarationCartonProduct cartonProduct : cartonProducts) {
                    Long realCartonId = cartonIdMap.get(cartonProduct.getCartonId());
                    Long realProductId = productIdMap.get(cartonProduct.getProductId());
                    
                    if (realCartonId != null && realProductId != null) {
                        DeclarationCartonProduct newCartonProduct = new DeclarationCartonProduct();
                        newCartonProduct.setCartonId(realCartonId);
                        newCartonProduct.setProductId(realProductId);
                        newCartonProduct.setQuantity(cartonProduct.getQuantity());
                        cartonProductService.save(newCartonProduct);
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("更新申报单失败", e);
            throw new RuntimeException("更新申报单失败: " + e.getMessage());
        }
    }

    @Override
    public DeclarationForm getFullDeclarationForm(Long id) {
        DeclarationForm form = this.getById(id);
        if (form != null) {
            // 查询产品明细
            List<DeclarationProduct> products = productService.lambdaQuery()
                    .eq(DeclarationProduct::getFormId, id)
                    .list();
            form.setProducts(products);
            
            // 查询箱子信息
            List<DeclarationCarton> cartons = cartonService.lambdaQuery()
                    .eq(DeclarationCarton::getFormId, id)
                    .orderByAsc(DeclarationCarton::getSortOrder)
                    .list();
            form.setCartons(cartons);
            
            // 查询箱子产品关联
            if (cartons != null && !cartons.isEmpty()) {
                List<DeclarationCartonProduct> cartonProducts = cartonProductService.lambdaQuery()
                        .in(DeclarationCartonProduct::getCartonId, 
                            cartons.stream().map(DeclarationCarton::getId).toArray())
                        .list();
                form.setCartonProducts(cartonProducts);
            }
            
            // 查询申报要素
            if (products != null && !products.isEmpty()) {
                for (DeclarationProduct product : products) {
                    List<DeclarationElementValue> elementValues = elementValueService.lambdaQuery()
                            .eq(DeclarationElementValue::getProductId, product.getId())
                            .list();
                    product.setElementValues(elementValues);
                }
            }
            
            // 查询水单信息
            List<DeclarationRemittance> remittances = remittanceService.lambdaQuery()
                    .eq(DeclarationRemittance::getFormId, id)
                    .orderByAsc(DeclarationRemittance::getRemittanceDate)
                    .list();
            form.setRemittances(remittances);

            // 查询附件信息
            List<DeclarationAttachment> attachments = attachmentService.lambdaQuery()
                    .eq(DeclarationAttachment::getFormId, id)
                    .orderByDesc(DeclarationAttachment::getCreateTime)
                    .list();
            form.setAttachments(attachments);
        }
        return form;
    }

    /**
     * 生成申报单号
     * @return 申报单号
     */
    private String generateFormNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        return "DEC" + datePrefix + randomSuffix;
    }

    @Override
    public DeclarationStatisticsDTO getStatistics() {
        DeclarationStatisticsDTO dto = new DeclarationStatisticsDTO();
        
        // 1. 统计总申报单数（排除草稿）
        Long totalForms = this.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0)
                .count();
        dto.setTotalForms(totalForms != null ? totalForms : 0L);
        
        // 2. 统计本月申报数
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        Long monthForms = this.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0)
                .ge(DeclarationForm::getCreateTime, firstDayOfMonth.atStartOfDay())
                .count();
        dto.setMonthForms(monthForms != null ? monthForms : 0L);
        
        // 3. 统计总金额和平均金额
        List<DeclarationForm> allForms = this.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0)
                .select(DeclarationForm::getTotalAmount)
                .list();
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DeclarationForm form : allForms) {
            if (form.getTotalAmount() != null) {
                totalAmount = totalAmount.add(form.getTotalAmount());
            }
        }
        dto.setTotalAmount(totalAmount);
        
        if (totalForms != null && totalForms > 0) {
            dto.setAvgAmount(totalAmount.divide(BigDecimal.valueOf(totalForms), 2, java.math.RoundingMode.HALF_UP));
        } else {
            dto.setAvgAmount(BigDecimal.ZERO);
        }
        
        // 4. 按状态统计
        List<DeclarationStatisticsDTO.StatusStat> statusStats = new ArrayList<>();
        Map<Integer, String> statusNameMap = new HashMap<>();
        statusNameMap.put(0, "草稿");
        statusNameMap.put(1, "已提交");
        statusNameMap.put(2, "已审核");
        statusNameMap.put(3, "已完成");
        
        for (int status = 1; status <= 3; status++) {
            final int s = status;
            List<DeclarationForm> statusForms = this.lambdaQuery()
                    .eq(DeclarationForm::getStatus, s)
                    .select(DeclarationForm::getTotalAmount)
                    .list();
            
            long count = statusForms.size();
            BigDecimal amount = BigDecimal.ZERO;
            for (DeclarationForm form : statusForms) {
                if (form.getTotalAmount() != null) {
                    amount = amount.add(form.getTotalAmount());
                }
            }
            
            statusStats.add(new DeclarationStatisticsDTO.StatusStat(
                    statusNameMap.get(status), count, amount));
        }
        dto.setStatusStats(statusStats);
        
        // 5. 按产品统计（前10名热门产品）
        List<DeclarationProduct> allProducts = productService.lambdaQuery()
                .select(DeclarationProduct::getProductName, DeclarationProduct::getHsCode, 
                        DeclarationProduct::getQuantity, DeclarationProduct::getAmount)
                .list();
        
        // 按产品名称分组统计
        Map<String, DeclarationStatisticsDTO.ProductStat> productMap = new HashMap<>();
        for (DeclarationProduct product : allProducts) {
            String key = product.getProductName();
            if (key == null || key.isEmpty()) continue;
            
            DeclarationStatisticsDTO.ProductStat stat = productMap.get(key);
            if (stat == null) {
                stat = new DeclarationStatisticsDTO.ProductStat();
                stat.setProductName(product.getProductName());
                stat.setHsCode(product.getHsCode());
                stat.setCount(0L);
                stat.setTotalAmount(BigDecimal.ZERO);
                productMap.put(key, stat);
            }
            stat.setCount(stat.getCount() + (product.getQuantity() != null ? product.getQuantity() : 0));
            if (product.getAmount() != null) {
                stat.setTotalAmount(stat.getTotalAmount().add(product.getAmount()));
            }
        }
        
        List<DeclarationStatisticsDTO.ProductStat> productStats = new ArrayList<>(productMap.values());
        productStats.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        if (productStats.size() > 10) {
            productStats = productStats.subList(0, 10);
        }
        dto.setProductStats(productStats);
        
        // 6. 按目的地统计
        List<DeclarationForm> formsWithDestination = this.lambdaQuery()
                .ne(DeclarationForm::getStatus, 0)
                .select(DeclarationForm::getDestinationCountry, DeclarationForm::getTotalAmount)
                .list();
        
        Map<String, DeclarationStatisticsDTO.DestinationStat> destMap = new HashMap<>();
        for (DeclarationForm form : formsWithDestination) {
            String dest = form.getDestinationCountry();
            if (dest == null || dest.isEmpty()) {
                dest = "未知";
            }
            
            DeclarationStatisticsDTO.DestinationStat stat = destMap.get(dest);
            if (stat == null) {
                stat = new DeclarationStatisticsDTO.DestinationStat();
                stat.setDestination(dest);
                stat.setCount(0L);
                stat.setTotalAmount(BigDecimal.ZERO);
                destMap.put(dest, stat);
            }
            stat.setCount(stat.getCount() + 1);
            if (form.getTotalAmount() != null) {
                stat.setTotalAmount(stat.getTotalAmount().add(form.getTotalAmount()));
            }
        }
        
        List<DeclarationStatisticsDTO.DestinationStat> destinationStats = new ArrayList<>(destMap.values());
        destinationStats.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        dto.setDestinationStats(destinationStats);
        
        return dto;
    }
}