package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationProduct;
import com.declaration.entity.DeclarationCarton;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.entity.DeclarationElementValue;
import com.declaration.service.DeclarationFormService;
import com.declaration.service.DeclarationProductService;
import com.declaration.service.DeclarationCartonService;
import com.declaration.service.DeclarationCartonProductService;
import com.declaration.service.DeclarationElementValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private final com.declaration.service.DeclarationRemittanceService remittanceService;
    private final com.declaration.service.DeclarationAttachmentService attachmentService;

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
            if (form.getOrgId() == null && cn.dev33.satoken.stp.StpUtil.isLogin()) {
                Object userOrgId = cn.dev33.satoken.stp.StpUtil.getSession().get("orgId");
                if (userOrgId != null) {
                    form.setOrgId(Long.valueOf(userOrgId.toString()));
                }
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
                productService.removeByIds(java.util.Arrays.asList(productIds));
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
                cartonService.removeByIds(java.util.Arrays.asList(cartonIds));
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
            List<com.declaration.entity.DeclarationRemittance> remittances = remittanceService.lambdaQuery()
                    .eq(com.declaration.entity.DeclarationRemittance::getFormId, id)
                    .orderByAsc(com.declaration.entity.DeclarationRemittance::getRemittanceDate)
                    .list();
            form.setRemittances(remittances);

            // 查询附件信息
            List<com.declaration.entity.DeclarationAttachment> attachments = attachmentService.lambdaQuery()
                    .eq(com.declaration.entity.DeclarationAttachment::getFormId, id)
                    .orderByDesc(com.declaration.entity.DeclarationAttachment::getCreateTime)
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
}