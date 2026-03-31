package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.DeclarationProductDao;
import com.declaration.dao.DeliveryOrderDao;
import com.declaration.dto.AuditHistoryDTO;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.*;
import com.declaration.dao.BusinessAuditRecordDao;
import com.declaration.service.*;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import cn.dev33.satoken.stp.StpUtil;
import com.declaration.utils.OrganizationUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final DeliveryOrderDao deliveryOrderDao;
    private final OperationLogService operationLogService;
    private final BusinessAuditRecordDao auditRecordDao;
    private final RuntimeService runtimeService;
    private final UserService userService;

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
            
            // 验证组织权限，确保用户只能修改自己组织的数据
            DeclarationForm existingForm = this.getById(form.getId());
            if (existingForm != null && existingForm.getOrgId() != null) {
                Long currentOrgId = OrganizationUtils.getCurrentUserOrgId();
                if (currentOrgId != null && !existingForm.getOrgId().equals(currentOrgId)) {
                    log.warn("用户尝试修改不属于其组织的申报单: {}", form.getId());
                    return false;
                }
            }
            
            // 确保不更新组织ID字段，保持原有的组织ID
            if (existingForm != null && existingForm.getOrgId() != null) {
                form.setOrgId(existingForm.getOrgId());
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

            // 查询提货单信息
            LambdaQueryWrapper<DeliveryOrder> deliveryOrderQuery = new LambdaQueryWrapper<>();
            deliveryOrderQuery.eq(DeliveryOrder::getFormId, id)
                    .orderByDesc(DeliveryOrder::getCreatedAt);
            List<DeliveryOrder> deliveryOrders = deliveryOrderDao.selectList(deliveryOrderQuery);
            form.setDeliveryOrders(deliveryOrders);
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
        // 状态定义（任务驱动流程）：
        // 0 - 草稿, 1 - 待初审, 2 - 处理中（并行任务进行中）, 8 - 已完成
        // 注：状态3/4/5已移除，并行阶段主状态始终为2
        List<DeclarationStatisticsDTO.StatusStat> statusStats = new ArrayList<>();
        Map<Integer, String> statusNameMap = new HashMap<>();
        statusNameMap.put(0, "草稿");
        statusNameMap.put(1, "待初审");
        statusNameMap.put(2, "处理中");
        statusNameMap.put(8, "已完成");
        
        int[] statuses = {1, 2, 8};
        for (int status : statuses) {
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

    // ================== 提货单相关方法实现 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveDeliveryOrder(DeliveryOrder deliveryOrder) {
        try {
            if (deliveryOrder.getStatus() == null) {
                deliveryOrder.setStatus(0); // 默认待审核状态
            }
            if (deliveryOrder.getCreatedBy() == null && StpUtil.isLogin()) {
                deliveryOrder.setCreatedBy(StpUtil.getLoginIdAsLong());
            }
            deliveryOrder.setCreatedAt(LocalDateTime.now());
            deliveryOrder.setUpdatedAt(LocalDateTime.now());
            
            int result = deliveryOrderDao.insert(deliveryOrder);
            log.info("保存提货单成功, ID: {}", deliveryOrder.getId());
            return result > 0;
        } catch (Exception e) {
            log.error("保存提货单失败", e);
            throw new RuntimeException("保存提货单失败: " + e.getMessage());
        }
    }

    @Override
    public List<DeliveryOrder> getDeliveryOrdersByFormId(Long formId) {
        LambdaQueryWrapper<DeliveryOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeliveryOrder::getFormId, formId)
                .orderByDesc(DeliveryOrder::getCreatedAt);
        return deliveryOrderDao.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDeliveryOrder(DeliveryOrder deliveryOrder) {
        try {
            deliveryOrder.setUpdatedAt(LocalDateTime.now());
            int result = deliveryOrderDao.updateById(deliveryOrder);
            log.info("更新提货单成功, ID: {}", deliveryOrder.getId());
            return result > 0;
        } catch (Exception e) {
            log.error("更新提货单失败", e);
            throw new RuntimeException("更新提货单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDeliveryOrder(Long id) {
        try {
            int result = deliveryOrderDao.deleteById(id);
            log.info("删除提货单成功, ID: {}", id);
            return result > 0;
        } catch (Exception e) {
            log.error("删除提货单失败", e);
            throw new RuntimeException("删除提货单失败: " + e.getMessage());
        }
    }

    @Override
    public DeliveryOrder getDeliveryOrderById(Long id) {
        return deliveryOrderDao.selectById(id);
    }

    // ================== 审核相关方法实现 ==================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditRemittance(Long id, boolean approved, String remark) {
        try {
            DeclarationRemittance remittance = remittanceService.getById(id);
            if (remittance == null) {
                throw new RuntimeException("水单不存在");
            }
            
            // 获取申报单 ID 和水单类型
            Long formId = remittance.getFormId();
            Integer remittanceType = remittance.getRemittanceType(); // 1-定金 2-尾款
            
            remittance.setStatus(approved ? 1 : 2); // 1-已审核 2-已驳回
            remittance.setAuditRemark(remark);
            if (StpUtil.isLogin()) {
                remittance.setAuditBy(StpUtil.getLoginIdAsLong());
            }
            remittance.setAuditTime(LocalDateTime.now());
            
            boolean result = remittanceService.updateById(remittance);
            
            // 注意：审核历史记录应该在提交申请时创建，不是在审核时
            // 这里只需要更新水单状态即可
            recordAuditLog("水单审核", "AUDIT", approved ? "审核通过" : "审核驳回", 
                    "水单ID: " + id + ", 结果: " + (approved ? "通过" : "驳回") + ", 备注: " + remark);
            
            log.info("审核水单成功, ID: {}, approved: {}", id, approved);
            return result;
        } catch (Exception e) {
            log.error("审核水单失败", e);
            throw new RuntimeException("审核水单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditDeliveryOrder(Long id, boolean approved, String remark) {
        try {
            DeliveryOrder deliveryOrder = deliveryOrderDao.selectById(id);
            if (deliveryOrder == null) {
                throw new RuntimeException("提货单不存在");
            }
            
            deliveryOrder.setStatus(approved ? 1 : 2); // 1-已审核 2-已驳回
            deliveryOrder.setAuditRemark(remark);
            if (StpUtil.isLogin()) {
                deliveryOrder.setAuditBy(StpUtil.getLoginIdAsLong());
            }
            deliveryOrder.setAuditTime(LocalDateTime.now());
            deliveryOrder.setUpdatedAt(LocalDateTime.now());
            
            int result = deliveryOrderDao.updateById(deliveryOrder);
            
            // 注意：审核历史记录应该在提交申请时创建，不是在审核时
            // 这里只需要更新提货单状态即可
            recordAuditLog("提货单审核", "AUDIT", approved ? "审核通过" : "审核驳回", 
                    "提货单ID: " + id + ", 结果: " + (approved ? "通过" : "驳回") + ", 备注: " + remark);
            
            log.info("审核提货单成功, ID: {}, approved: {}", id, approved);
            return result > 0;
        } catch (Exception e) {
            log.error("审核提货单失败", e);
            throw new RuntimeException("审核提货单失败: " + e.getMessage());
        }
    }

    /**
     * 实现申请退回草稿功能
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean applyReturnToDraft(Long id, String reason) {
        DeclarationForm form = this.getById(id);
        if (form == null) {
            throw new RuntimeException("申报单不存在");
        }
        
        // 校验状态：仅限初审之后 (status >= 2) 且非已完成 (status != 8) 且非草稿 (status != 0)
        // 注意：status 1 为待初审，用户说初审阶段直接驳回即可，不走此流程
        if (form.getStatus() < 2 || form.getStatus() == 8) {
            throw new RuntimeException("当前申报单状态不支持申请退回草稿 (当前状态: " + form.getStatus() + ")");
        }
        
        // 1. 创建审核历史记录
        BusinessAuditRecord record = new BusinessAuditRecord();
        record.setBusinessId(id);
        record.setBusinessType("DECLARATION_RETURN");
        if (StpUtil.isLogin()) {
            record.setApplicantId(StpUtil.getLoginIdAsLong());
        }
        record.setApplyReason(reason == null || reason.isEmpty() ? "申报错误" : reason);
        record.setApplyTime(LocalDateTime.now());
        record.setAuditStatus(0); // 0-待审核
        record.setPreStatus(form.getStatus()); // 记录申请前的原始状态，用于驳回后恢复
        
        auditRecordDao.insert(record);
        
        // 2. 更新单据状态为 9-退回待审
        form.setStatus(9);
        boolean result = this.updateById(form);
        
        // 3. 记录操作日志
        recordAuditLog("申请退回草稿", "APPLY", "applyReturnToDraft", 
                "申报单ID: " + id + ", 原因: " + record.getApplyReason());
        
        return result;
    }

    /**
     * 实现审核退回草稿功能
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditReturnToDraft(Long id, boolean approved, String remark) {
        DeclarationForm form = this.getById(id);
        if (form == null || form.getStatus() != 9) {
            throw new RuntimeException("申报单不存在或非退回待审状态");
        }
        
        // 1. 获取最新的待审核记录
        BusinessAuditRecord record = auditRecordDao.selectOne(new LambdaQueryWrapper<BusinessAuditRecord>()
                .eq(BusinessAuditRecord::getBusinessId, id)
                .eq(BusinessAuditRecord::getBusinessType, "DECLARATION_RETURN")
                .eq(BusinessAuditRecord::getAuditStatus, 0)
                .orderByDesc(BusinessAuditRecord::getApplyTime)
                .last("limit 1"));
        
        if (record == null) {
            throw new RuntimeException("未找到对应的申请记录");
        }
        
        // 2. 更新审核历史记录
        if (StpUtil.isLogin()) {
            record.setAuditorId(StpUtil.getLoginIdAsLong());
        }
        record.setAuditStatus(approved ? 1 : 2); // 1-通过 2-驳回
        record.setAuditRemark(remark == null || remark.isEmpty() ? (approved ? "通过" : "填写错误") : remark);
        record.setAuditTime(LocalDateTime.now());
        auditRecordDao.updateById(record);
        
        // 3. 更新单据状态
        if (approved) {
            // 通过：重置为草稿状态，并终止工作流
            form.setStatus(0);
            
            // 终止 Flowable 任务
            try {
                List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                        .processInstanceBusinessKey(String.valueOf(id))
                        .list();
                for (ProcessInstance instance : instances) {
                    runtimeService.deleteProcessInstance(instance.getId(), "用户申请退回草稿并通过: " + record.getAuditRemark());
                    log.info("已终止 Flowable 流程实例: {}", instance.getId());
                }
            } catch (Exception e) {
                log.error("终止工作流失败", e);
                // 这里可以选择是否抛出异常回滚状态，通常为了业务闭环建议回滚
                throw new RuntimeException("终止工作流失败: " + e.getMessage());
            }
        } else {
            // 驳回：恢复到之前的状态
            form.setStatus(record.getPreStatus());
        }
        
        boolean result = this.updateById(form);
        
        // 4. 记录操作日志
        recordAuditLog("退回审核", "AUDIT", approved ? "审核通过" : "审核驳回", 
                "申报单ID: " + id + ", 结果: " + (approved ? "通过" : "驳回") + ", 备注: " + record.getAuditRemark());
        
        return result;
    }

    /**
     * 获取退回申请审核历史
     */
    @Override
    public List<AuditHistoryDTO> getReturnAuditHistory(Long id) {
        List<BusinessAuditRecord> records = auditRecordDao.selectList(
            new LambdaQueryWrapper<BusinessAuditRecord>()
                .eq(BusinessAuditRecord::getBusinessId, id)
                // .eq(BusinessAuditRecord::getBusinessType, "DECLARATION_RETURN")
                .orderByDesc(BusinessAuditRecord::getApplyTime)
        );
        
        // 转换为 DTO 并填充用户名称
        return records.stream().map(record -> {
            AuditHistoryDTO dto = new AuditHistoryDTO();
            dto.setId(record.getId());
            dto.setBusinessId(record.getBusinessId());
            dto.setBusinessType(record.getBusinessType());
            dto.setApplicantId(record.getApplicantId());
            dto.setApplyReason(record.getApplyReason());
            dto.setApplyTime(record.getApplyTime());
            dto.setAuditorId(record.getAuditorId());
            dto.setAuditStatus(record.getAuditStatus());
            dto.setAuditRemark(record.getAuditRemark());
            dto.setAuditTime(record.getAuditTime());
            dto.setPreStatus(record.getPreStatus());
            
            // 查询申请人名称
            if (record.getApplicantId() != null) {
                try {
                    User applicant = userService.getById(record.getApplicantId());
                    if (applicant != null) {
                        dto.setApplicantName(applicant.getUsername());
                    }
                } catch (Exception e) {
                    log.warn("查询申请人信息失败：{}", record.getApplicantId(), e);
                }
            }
            
            // 查询审核人名称
            if (record.getAuditorId() != null) {
                try {
                    User auditor = userService.getById(record.getAuditorId());
                    if (auditor != null) {
                        dto.setAuditorName(auditor.getUsername());
                    }
                } catch (Exception e) {
                    log.warn("查询审核人信息失败：{}", record.getAuditorId(), e);
                }
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 记录审核操作日志
     */
    private void recordAuditLog(String businessType, String operationType, String method, String params) {
        try {
            OperationLog log = new OperationLog();
            if (StpUtil.isLogin()) {
                log.setUserId(StpUtil.getLoginIdAsLong());
            }
            log.setBusinessType(businessType);
            log.setOperationType(operationType);
            log.setMethod(method);
            log.setRequestParams(params);
            log.setStatus(0); // 成功
            log.setCreateTime(LocalDateTime.now());
            
            operationLogService.saveOperationLog(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            DeclarationFormServiceImpl.log.warn("记录审核日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    public void saveAuditRecord(BusinessAuditRecord record) {
        try {
            auditRecordDao.insert(record);
            log.info("保存审核历史记录成功，业务 ID: {}, 类型：{}", record.getBusinessId(), record.getBusinessType());
        } catch (Exception e) {
            log.error("保存审核历史记录失败", e);
            throw new RuntimeException("保存审核历史记录失败：" + e.getMessage());
        }
    }
}