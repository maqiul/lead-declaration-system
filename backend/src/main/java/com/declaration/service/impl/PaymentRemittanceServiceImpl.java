package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.PaymentRemittanceDao;
import com.declaration.dao.PaymentRemittanceFormRelationDao;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.PaymentRemittance;
import com.declaration.entity.PaymentRemittanceFormRelation;
import com.declaration.service.PaymentRemittanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 出款水单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRemittanceServiceImpl extends ServiceImpl<PaymentRemittanceDao, PaymentRemittance> implements PaymentRemittanceService {

    private final PaymentRemittanceFormRelationDao relationDao;
    private final DeclarationFormDao declarationFormDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRemittance createRemittance(PaymentRemittance remittance) {
        String paymentNo = generatePaymentNo();
        remittance.setPaymentNo(paymentNo);
        remittance.setStatus(0); // 草稿
        remittance.setCreateBy(StpUtil.getLoginIdAsLong());
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());
        save(remittance);
        log.info("创建出款水单成功, 编号: {}", paymentNo);
        return remittance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForAudit(Long remittanceId) {
        PaymentRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("出款水单不存在");
        }
        if (remittance.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的出款水单可以提交审核");
        }

        remittance.setStatus(1); // 待审核
        remittance.setSubmitTime(LocalDateTime.now());
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        log.info("出款水单提交审核成功, ID: {}", remittanceId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditRemittance(Long remittanceId, boolean approved, Long bankAccountId, String auditRemark) {
        PaymentRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("出款水单不存在");
        }
        if (remittance.getStatus() != 1) {
            throw new RuntimeException("只有待审核状态的出款水单可以审核");
        }

        if (approved) {
            remittance.setStatus(2); // 已审核
            if (bankAccountId != null) {
                remittance.setBankAccountId(bankAccountId);
            }
        } else {
            remittance.setStatus(3); // 已驳回
        }

        remittance.setAuditBy(StpUtil.getLoginIdAsLong());
        remittance.setAuditByName(StpUtil.getLoginIdAsString());
        remittance.setAuditTime(LocalDateTime.now());
        remittance.setAuditRemark(auditRemark);
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        log.info("出款水单审核{}, ID: {}, 备注: {}", approved ? "通过" : "驳回", remittanceId, auditRemark);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeAudit(Long remittanceId) {
        PaymentRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("出款水单不存在");
        }
        if (remittance.getStatus() != 2) {
            throw new RuntimeException("只有已审核状态的出款水单可以反审核");
        }

        remittance.setStatus(0);
        remittance.setBankAccountId(null);
        remittance.setBankAccountName(null);
        remittance.setAuditBy(null);
        remittance.setAuditByName(null);
        remittance.setAuditTime(null);
        remittance.setAuditRemark(null);
        remittance.setSubmitTime(null);
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        log.info("出款水单反审核成功, ID: {}", remittanceId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean relateToForm(Long remittanceId, Long formId, BigDecimal amount, Integer relationType) {
        PaymentRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("出款水单不存在");
        }

        // 检查是否已经关联
        LambdaQueryWrapper<PaymentRemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRemittanceFormRelation::getRemittanceId, remittanceId)
               .eq(PaymentRemittanceFormRelation::getFormId, formId);
        Long count = relationDao.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该出款水单已关联此申报单");
        }

        // 关联金额必填且大于 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("关联金额必填且必须大于 0");
        }

        // 校验关联金额不超过出款水单总金额
        if (remittance.getPaymentAmount() != null) {
            LambdaQueryWrapper<PaymentRemittanceFormRelation> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(PaymentRemittanceFormRelation::getRemittanceId, remittanceId);
            List<PaymentRemittanceFormRelation> existingRelations = relationDao.selectList(existWrapper);
            BigDecimal existingTotal = existingRelations.stream()
                    .map(r -> r.getRelationAmount() != null ? r.getRelationAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotal = existingTotal.add(amount);
            if (newTotal.compareTo(remittance.getPaymentAmount()) > 0) {
                throw new RuntimeException(String.format(
                        "关联金额超出出款水单总金额！水单金额: %s, 已关联: %s, 本次: %s, 合计: %s",
                        remittance.getPaymentAmount(), existingTotal, amount, newTotal));
            }
        }

        PaymentRemittanceFormRelation relation = new PaymentRemittanceFormRelation();
        relation.setRemittanceId(remittanceId);
        relation.setFormId(formId);
        relation.setRelationType(relationType != null ? relationType : 1);
        relation.setRelationAmount(amount);
        relation.setCreateBy(StpUtil.getLoginIdAsLong());

        relationDao.insert(relation);
        log.info("出款水单关联申报单成功, 水单ID: {}, 申报单ID: {}, 关联金额: {}", remittanceId, formId, amount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unrelateFromForm(Long remittanceId, Long formId) {
        LambdaQueryWrapper<PaymentRemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRemittanceFormRelation::getRemittanceId, remittanceId)
               .eq(PaymentRemittanceFormRelation::getFormId, formId);

        int deleted = relationDao.delete(wrapper);
        log.info("取消出款水单与申报单关联, 水单ID: {}, 申报单ID: {}, 删除: {}", remittanceId, formId, deleted);
        return deleted > 0;
    }

    @Override
    public List<Map<String, Object>> getRelatedForms(Long remittanceId) {
        LambdaQueryWrapper<PaymentRemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRemittanceFormRelation::getRemittanceId, remittanceId);
        List<PaymentRemittanceFormRelation> relations = relationDao.selectList(wrapper);

        return relations.stream().map(relation -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("relationId", relation.getId());
            map.put("formId", relation.getFormId());
            map.put("relationType", relation.getRelationType());
            map.put("relationAmount", relation.getRelationAmount());
            map.put("createTime", relation.getCreateTime());

            DeclarationForm form = declarationFormDao.selectById(relation.getFormId());
            if (form != null) {
                map.put("formNo", form.getFormNo());
                map.put("formDate", form.getDeclarationDate());
                map.put("totalAmount", form.getTotalAmount());
                map.put("currency", form.getCurrency());
                map.put("entityId", form.getEntityId());
                map.put("customerName", form.getShipperCompany());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRemittancesByFormId(Long formId) {
        LambdaQueryWrapper<PaymentRemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentRemittanceFormRelation::getFormId, formId);
        List<PaymentRemittanceFormRelation> relations = relationDao.selectList(wrapper);

        List<Long> remittanceIds = relations.stream()
                .map(PaymentRemittanceFormRelation::getRemittanceId)
                .collect(Collectors.toList());

        if (remittanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<PaymentRemittance> remittances = listByIds(remittanceIds);

        Map<Long, PaymentRemittanceFormRelation> relationMap = relations.stream()
                .collect(Collectors.toMap(PaymentRemittanceFormRelation::getRemittanceId, r -> r));

        return remittances.stream().map(remittance -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", remittance.getId());
            map.put("paymentNo", remittance.getPaymentNo());
            map.put("payeeName", remittance.getPayeeName());
            map.put("paymentDate", remittance.getPaymentDate());
            map.put("paymentAmount", remittance.getPaymentAmount());
            map.put("currency", remittance.getCurrency());
            map.put("bankAccountName", remittance.getBankAccountName());
            map.put("photoUrl", remittance.getPhotoUrl());
            map.put("remarks", remittance.getRemarks());
            map.put("status", remittance.getStatus());
            map.put("relationType", relationMap.get(remittance.getId()).getRelationType());
            map.put("relationAmount", relationMap.get(remittance.getId()).getRelationAmount());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<PaymentRemittance> getPage(PageParam pageParam, Integer status, String paymentNo, String relationStatus) {
        LambdaQueryWrapper<PaymentRemittance> wrapper = new LambdaQueryWrapper<>();

        // 数据权限控制
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            boolean isAdmin = StpUtil.hasRole("ADMIN");
            boolean isFinance = StpUtil.hasRole("FINANCE_AUDITOR");
            boolean isDeptAdmin = StpUtil.hasRole("DEPT_ADMIN");
            if (!isAdmin && !isFinance && !isDeptAdmin) {
                wrapper.eq(PaymentRemittance::getCreateBy, currentUserId);
            }
        } catch (Exception e) {
            // ignore
        }

        if (status != null) {
            wrapper.eq(PaymentRemittance::getStatus, status);
        }
        if (paymentNo != null && !paymentNo.isEmpty()) {
            wrapper.like(PaymentRemittance::getPaymentNo, paymentNo);
        }

        wrapper.orderByDesc(PaymentRemittance::getCreateTime);

        boolean needRelationFilter = relationStatus != null && !relationStatus.isEmpty();

        if (needRelationFilter) {
            wrapper.eq(PaymentRemittance::getStatus, 2);
            List<PaymentRemittance> allRecords = list(wrapper);
            fillTotalRelatedAmount(allRecords);
            allRecords = allRecords.stream().filter(r -> {
                String rs = computeRelationStatus(r);
                return relationStatus.equals(rs);
            }).collect(Collectors.toList());
            long totalCount = allRecords.size();
            int from = (int) ((pageParam.getCurrent() - 1) * pageParam.getSize());
            int to = Math.min(from + (int) pageParam.getSize(), allRecords.size());
            List<PaymentRemittance> pageRecords = from < allRecords.size() ? allRecords.subList(from, to) : new ArrayList<>();
            Page<PaymentRemittance> manualPage = new Page<>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
            manualPage.setRecords(pageRecords);
            return manualPage;
        } else {
            Page<PaymentRemittance> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
            IPage<PaymentRemittance> result = page(page, wrapper);
            fillTotalRelatedAmount(result.getRecords());
            return result;
        }
    }

    @Override
    public boolean hasApprovedRemittance(Long formId) {
        // 查询关联表中该申报单关联的所有出款水单ID
        LambdaQueryWrapper<PaymentRemittanceFormRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(PaymentRemittanceFormRelation::getFormId, formId);
        List<PaymentRemittanceFormRelation> relations = relationDao.selectList(relWrapper);

        if (relations.isEmpty()) {
            return false;
        }

        List<Long> remittanceIds = relations.stream()
                .map(PaymentRemittanceFormRelation::getRemittanceId)
                .collect(Collectors.toList());

        // 查询是否有 status=2（已审核）的出款水单
        LambdaQueryWrapper<PaymentRemittance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PaymentRemittance::getId, remittanceIds)
               .eq(PaymentRemittance::getStatus, 2);
        return count(wrapper) > 0;
    }

    private void fillTotalRelatedAmount(List<PaymentRemittance> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> remittanceIds = records.stream().map(PaymentRemittance::getId).collect(Collectors.toList());
        LambdaQueryWrapper<PaymentRemittanceFormRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.in(PaymentRemittanceFormRelation::getRemittanceId, remittanceIds);
        List<PaymentRemittanceFormRelation> allRelations = relationDao.selectList(relWrapper);
        Map<Long, BigDecimal> totalAmountMap = new HashMap<>();
        for (PaymentRemittanceFormRelation rel : allRelations) {
            BigDecimal amt = rel.getRelationAmount() != null ? rel.getRelationAmount() : BigDecimal.ZERO;
            totalAmountMap.merge(rel.getRemittanceId(), amt, BigDecimal::add);
        }
        for (PaymentRemittance r : records) {
            r.setTotalRelatedAmount(totalAmountMap.getOrDefault(r.getId(), BigDecimal.ZERO));
        }
    }

    private String computeRelationStatus(PaymentRemittance r) {
        if (r.getStatus() == null || r.getStatus() != 2) return "N/A";
        BigDecimal related = r.getTotalRelatedAmount() != null ? r.getTotalRelatedAmount() : BigDecimal.ZERO;
        BigDecimal total = r.getPaymentAmount() != null ? r.getPaymentAmount() : BigDecimal.ZERO;
        if (related.compareTo(BigDecimal.ZERO) <= 0) return "UNRELATED";
        if (related.compareTo(total) >= 0) return "RELATED";
        return "PARTIAL";
    }

    /**
     * 生成出款水单编号: CK + yyyyMMdd + 4位序号
     */
    private String generatePaymentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "CK" + dateStr;

        LambdaQueryWrapper<PaymentRemittance> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(PaymentRemittance::getPaymentNo, prefix)
               .orderByDesc(PaymentRemittance::getPaymentNo)
               .last("LIMIT 1");

        PaymentRemittance last = getOne(wrapper);
        int seq = 1;
        if (last != null && last.getPaymentNo() != null) {
            String lastNo = last.getPaymentNo();
            String lastSeq = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }
}

