package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.DeclarationProductDao;
import com.declaration.dao.DeliveryOrderDao;
import com.declaration.dao.RemittanceFormRelationDao;
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
import java.util.concurrent.CompletableFuture;

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
    private final RemittanceFormRelationDao remittanceFormRelationDao;
    private final OperationLogService operationLogService;
    private final BusinessAuditRecordDao auditRecordDao;
    private final RuntimeService runtimeService;
    private final UserService userService;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

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
            
            // 自动计算并在保存前填充箱子总数
            if (form.getCartons() != null && !form.getCartons().isEmpty()) {
                int totalCartons = 0;
                for (DeclarationCarton carton : form.getCartons()) {
                    if (carton.getQuantity() != null) {
                        totalCartons += carton.getQuantity();
                    }
                }
                form.setTotalCartons(totalCartons);
            } else {
                form.setTotalCartons(0);
            }
            
            // 保存申报单主表
            boolean saved = this.save(form);
            if (!saved) {
                return false;
            }
            
            // 保存主表后批量保存所有子数据
            saveNestedData(form);
            
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

            // 自动计算并在更新前填充箱子总数
            if (form.getCartons() != null && !form.getCartons().isEmpty()) {
                int totalCartons = 0;
                for (DeclarationCarton carton : form.getCartons()) {
                    if (carton.getQuantity() != null) {
                        totalCartons += carton.getQuantity();
                    }
                }
                form.setTotalCartons(totalCartons);
            } else {
                form.setTotalCartons(0);
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

            // 3. 重新合并批量保存子数据
            saveNestedData(form);

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
            try {
                // ====== 第一波并发：产品 + 箱子 + 水单 + 附件 + 提货单 同时查询 ======
                CompletableFuture<List<DeclarationProduct>> productsFuture = CompletableFuture.supplyAsync(() ->
                    productService.lambdaQuery()
                            .eq(DeclarationProduct::getFormId, id)
                            .list()
                );

                CompletableFuture<List<DeclarationCarton>> cartonsFuture = CompletableFuture.supplyAsync(() ->
                    cartonService.lambdaQuery()
                            .eq(DeclarationCarton::getFormId, id)
                            .orderByAsc(DeclarationCarton::getSortOrder)
                            .list()
                );

                CompletableFuture<List<DeclarationRemittance>> remittancesFuture = CompletableFuture.supplyAsync(() -> {
                    // 通过关联表查询申报单关联的水单
                    List<RemittanceFormRelation> relations = remittanceFormRelationDao.selectList(
                        new LambdaQueryWrapper<RemittanceFormRelation>()
                            .eq(RemittanceFormRelation::getFormId, id)
                            .orderByAsc(RemittanceFormRelation::getCreateTime)
                    );
                    
                    if (relations.isEmpty()) {
                        log.info("💰 查询申报单ID={} 的水单关联，结果: 0条", id);
                        return new ArrayList<>();
                    }
                    
                    // 获取所有关联的水单ID
                    List<Long> remittanceIds = relations.stream()
                        .map(RemittanceFormRelation::getRemittanceId)
                        .collect(Collectors.toList());
                    
                    log.info("💰 申报单ID={} 关联的水单ID列表: {}", id, remittanceIds);
                    
                    // 查询水单详情
                    List<DeclarationRemittance> remittances = remittanceService.lambdaQuery()
                        .in(DeclarationRemittance::getId, remittanceIds)
                        .orderByAsc(DeclarationRemittance::getRemittanceDate)
                        .list();
                    
                    // 填充关联金额到水单对象（使用动态字段）
                    Map<Long, BigDecimal> amountMap = relations.stream()
                        .collect(Collectors.toMap(
                            RemittanceFormRelation::getRemittanceId,
                            r -> r.getRelationAmount() != null ? r.getRelationAmount() : BigDecimal.ZERO,
                            BigDecimal::add
                        ));
                    
                    for (DeclarationRemittance remittance : remittances) {
                        // 通过设置 totalRelatedAmount 字段传递关联金额
                        remittance.setTotalRelatedAmount(amountMap.get(remittance.getId()));
                    }
                    
                    log.info("💰 查询申报单ID={} 的水单，结果数量: {}", id, remittances.size());
                    if (!remittances.isEmpty()) {
                        log.info("💰 水单详情: {}", remittances);
                    }
                    return remittances;
                });
                
                CompletableFuture<List<DeclarationAttachment>> attachmentsFuture = CompletableFuture.supplyAsync(() ->
                    attachmentService.lambdaQuery()
                            .eq(DeclarationAttachment::getFormId, id)
                            .orderByDesc(DeclarationAttachment::getCreateTime)
                            .list()
                );
                
                CompletableFuture<List<DeliveryOrder>> deliveryOrdersFuture = CompletableFuture.supplyAsync(() -> {
                    LambdaQueryWrapper<DeliveryOrder> deliveryOrderQuery = new LambdaQueryWrapper<>();
                    deliveryOrderQuery.eq(DeliveryOrder::getFormId, id)
                            .orderByDesc(DeliveryOrder::getCreatedAt);
                    return deliveryOrderDao.selectList(deliveryOrderQuery);
                });
                
                // 等待第一波全部查完
                CompletableFuture.allOf(productsFuture, cartonsFuture, remittancesFuture, attachmentsFuture, deliveryOrdersFuture).join();
                
                List<DeclarationProduct> products = productsFuture.get();
                List<DeclarationCarton> cartons = cartonsFuture.get();
                
                form.setProducts(products);
                form.setCartons(cartons);
                form.setRemittances(remittancesFuture.get());
                form.setAttachments(attachmentsFuture.get());
                form.setDeliveryOrders(deliveryOrdersFuture.get());
                
                // ====== 第二波并发：基于第一波结果的依赖查询 ======
                CompletableFuture<Void> elementsFuture = CompletableFuture.runAsync(() -> {
                    if (products != null && !products.isEmpty()) {
                        // 一次性查出所有产品的申报要素，避免 N+1 问题
                        List<Long> productIds = products.stream()
                                .map(DeclarationProduct::getId)
                                .collect(Collectors.toList());
                        List<DeclarationElementValue> allValues = elementValueService.lambdaQuery()
                                .in(DeclarationElementValue::getProductId, productIds)
                                .list();
                        // 按 productId 分组后回填
                        Map<Long, List<DeclarationElementValue>> valueMap = allValues.stream()
                                .collect(Collectors.groupingBy(DeclarationElementValue::getProductId));
                        for (DeclarationProduct product : products) {
                            product.setElementValues(valueMap.getOrDefault(product.getId(), new ArrayList<>()));
                        }
                    }
                });
                
                CompletableFuture<Void> cartonProductsFuture = CompletableFuture.runAsync(() -> {
                    if (cartons != null && !cartons.isEmpty()) {
                        List<DeclarationCartonProduct> cartonProducts = cartonProductService.lambdaQuery()
                                .in(DeclarationCartonProduct::getCartonId,
                                    cartons.stream().map(DeclarationCarton::getId).toArray())
                                .list();
                        form.setCartonProducts(cartonProducts);
                    }
                });
                
                // 等待第二波全部查完
                CompletableFuture.allOf(elementsFuture, cartonProductsFuture).join();
                
            } catch (Exception e) {
                log.error("并发查询申报单详情异常，降级为空数据: formId={}", id, e);
            }
        }
        return form;
    }

    /**
     * 生成申报单号
     * @return 申报单号
     */
    private String generateFormNo() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String redisKey = "sys:seq:formno:" + datePrefix;
        
        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1L) {
            stringRedisTemplate.expire(redisKey, 24, java.util.concurrent.TimeUnit.HOURS);
        }
        
        String suffix = String.format("%04d", seq != null ? seq : 1L);
        return "DEC" + datePrefix + suffix;
    }

    /**
     * 高性能批量保存关联数据
     */
    private void saveNestedData(DeclarationForm form) {
        List<DeclarationCarton> cartons = form.getCartons();
        Map<Long, Long> cartonIdMap = new HashMap<>(); 
        if (cartons != null && !cartons.isEmpty()) {
            List<Long> tempIds = new ArrayList<>();
            for (int i = 0; i < cartons.size(); i++) {
                DeclarationCarton carton = cartons.get(i);
                tempIds.add(carton.getId()); 
                carton.setId(null); 
                carton.setFormId(form.getId());
                carton.setSortOrder(i);
            }
            cartonService.saveBatch(cartons);
            for (int i = 0; i < cartons.size(); i++) {
                cartonIdMap.put(tempIds.get(i), cartons.get(i).getId());
            }
        }
        
        List<DeclarationProduct> products = form.getProducts();
        Map<Long, Long> productIdMap = new HashMap<>(); 
        if (products != null && !products.isEmpty()) {
            List<Long> tempIds = new ArrayList<>();
            List<DeclarationElementValue> allElementValues = new ArrayList<>();
            for (int i = 0; i < products.size(); i++) {
                DeclarationProduct product = products.get(i);
                tempIds.add(product.getId()); 
                product.setId(null); 
                product.setFormId(form.getId());
                product.setSortOrder(i);
            }
            productService.saveBatch(products);
            
            for (int i = 0; i < products.size(); i++) {
                DeclarationProduct product = products.get(i);
                productIdMap.put(tempIds.get(i), product.getId());
                
                List<DeclarationElementValue> elementValues = product.getElementValues();
                if (elementValues != null && !elementValues.isEmpty()) {
                    for (DeclarationElementValue elementValue : elementValues) {
                        elementValue.setId(null); 
                        elementValue.setProductId(product.getId());
                        allElementValues.add(elementValue);
                    }
                }
            }
            if (!allElementValues.isEmpty()) {
                elementValueService.saveBatch(allElementValues);
            }
        }
        
        List<DeclarationCartonProduct> cartonProducts = form.getCartonProducts();
        if (cartonProducts != null && !cartonProducts.isEmpty() && !cartonIdMap.isEmpty()) {
            List<DeclarationCartonProduct> newCartonProducts = new ArrayList<>();
            for (DeclarationCartonProduct cartonProduct : cartonProducts) {
                Long realCartonId = cartonIdMap.get(cartonProduct.getCartonId());
                Long realProductId = productIdMap.get(cartonProduct.getProductId());
                
                if (realCartonId != null && realProductId != null) {
                    DeclarationCartonProduct newCartonProduct = new DeclarationCartonProduct();
                    newCartonProduct.setCartonId(realCartonId);
                    newCartonProduct.setProductId(realProductId);
                    newCartonProduct.setQuantity(cartonProduct.getQuantity());
                    newCartonProducts.add(newCartonProduct);
                }
            }
            if (!newCartonProducts.isEmpty()) {
                cartonProductService.saveBatch(newCartonProducts);
            }
        }
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

    /**
     * 审核水单
     * @deprecated 已废弃,请使用 DeclarationRemittanceService.auditRemittance()
     */
    @Override
    @Deprecated
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
            
            // 处理水单关联关系：取消所有已关联的水单
            try {
                handleRemittanceRelations(id);
            } catch (Exception e) {
                log.error("处理水单关联关系失败", e);
                // 水单处理失败不影响主流程，继续执行
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
     * 处理水单关联关系：取消申报单与所有水单的关联
     * @param formId 申报单ID
     */
    private void handleRemittanceRelations(Long formId) {
        log.info("开始处理申报单 {} 的水单关联关系", formId);
        
        // 查询该申报单关联的所有水单
        List<RemittanceFormRelation> relations = remittanceFormRelationDao.selectList(
            new LambdaQueryWrapper<RemittanceFormRelation>()
                .eq(RemittanceFormRelation::getFormId, formId)
        );
        
        if (relations.isEmpty()) {
            log.info("申报单 {} 没有关联的水单", formId);
            return;
        }
        
        log.info("申报单 {} 关联了 {} 个水单，开始取消关联", formId, relations.size());
        
        // 删除所有关联关系
        for (RemittanceFormRelation relation : relations) {
            remittanceFormRelationDao.deleteById(relation.getId());
            log.info("已取消水单 {} 与申报单 {} 的关联", relation.getRemittanceId(), formId);
        }
        
        log.info("申报单 {} 的水单关联关系处理完成", formId);
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