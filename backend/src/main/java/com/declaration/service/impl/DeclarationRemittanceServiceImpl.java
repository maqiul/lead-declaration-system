package com.declaration.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.DeclarationFormDao;
import com.declaration.dao.DeclarationRemittanceDao;
import com.declaration.dao.RemittanceFormRelationDao;
import com.declaration.entity.BankAccountConfig;
import com.declaration.entity.DeclarationForm;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.entity.RemittanceFormRelation;
import com.declaration.service.BankAccountConfigService;
import com.declaration.service.DeclarationRemittanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 水单信息服务实现类(集成Flowable)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclarationRemittanceServiceImpl extends ServiceImpl<DeclarationRemittanceDao, DeclarationRemittance> implements DeclarationRemittanceService {

    private final RemittanceFormRelationDao relationDao;
    private final BankAccountConfigService bankAccountConfigService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final DeclarationFormDao declarationFormDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeclarationRemittance createRemittance(DeclarationRemittance remittance) {
        // 生成水单编号
        String remittanceNo = generateRemittanceNo();
        remittance.setRemittanceNo(remittanceNo);
        remittance.setStatus(0); // 草稿状态
        remittance.setCreateBy(StpUtil.getLoginIdAsLong());
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());
        
        save(remittance);
        log.info("创建水单成功, 水单编号: {}", remittanceNo);
        return remittance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForAudit(Long remittanceId) {
        DeclarationRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("水单不存在");
        }
        if (remittance.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的水单可以提交审核");
        }

        // 启动Flowable流程
        Map<String, Object> variables = new HashMap<>();
        variables.put("remittanceId", remittanceId.toString());
        variables.put("starterId", StpUtil.getLoginIdAsString());
        
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("remittanceAuditProcess", 
                "remittance-" + remittanceId, variables);
        
        // 保存流程实例ID
        remittance.setProcessInstanceId(processInstance.getProcessInstanceId());
        remittance.setStatus(1); // 待审核状态
        remittance.setSubmitTime(LocalDateTime.now());
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        log.info("水单提交审核成功, 水单ID: {}, 流程实例ID: {}", remittanceId, processInstance.getProcessInstanceId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditRemittance(Long remittanceId, boolean approved, Long bankAccountId, BigDecimal taxRate, BigDecimal manualBankFee, String auditRemark) {
        DeclarationRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("水单不存在");
        }
        if (remittance.getStatus() != 1) {
            throw new RuntimeException("只有待审核状态的水单可以审核");
        }

        // 查找当前待审核任务
        Task task = taskService.createTaskQuery()
                .processInstanceId(remittance.getProcessInstanceId())
                .taskDefinitionKey("financeAudit")
                .singleResult();
        
        if (task == null) {
            throw new RuntimeException("未找到待审核任务,可能流程已结束");
        }

        // 设置流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        variables.put("taxRate", taxRate);
        variables.put("bankAccountId", bankAccountId);
        variables.put("auditRemark", auditRemark);
        
        if (approved && bankAccountId != null && taxRate != null) {
            // 审核通过,计算银行手续费和入账金额
            BankAccountConfig bankAccount = bankAccountConfigService.getById(bankAccountId);
            if (bankAccount == null) {
                throw new RuntimeException("银行账户不存在");
            }

            // 银行手续费由用户手动填写，不再自动计算
            BigDecimal bankFee = (manualBankFee != null) ? manualBankFee : BigDecimal.ZERO;
            BigDecimal creditedAmount = remittance.getRemittanceAmount().subtract(bankFee)
                    .setScale(4, RoundingMode.HALF_UP);

            // 计算内部手续费（人民币）= 汇率 × 收汇金额 × 银行手续费率
            BigDecimal internalBankFee = calculateInternalBankFee(bankAccountId, remittance.getRemittanceAmount(), taxRate);

            // 更新水单信息
            remittance.setStatus(2); // 已审核状态
            remittance.setTaxRate(taxRate);
            remittance.setBankAccountId(bankAccountId);
            remittance.setBankAccountName(bankAccount.getBankName());
            remittance.setBankFeeRate(bankAccount.getServiceFeeRate());
            remittance.setBankFee(bankFee);
            remittance.setCreditedAmount(creditedAmount);
            remittance.setInternalBankFee(internalBankFee);
        } else {
            // 审核驳回
            remittance.setStatus(3); // 已驳回状态
        }
        
        remittance.setAuditBy(StpUtil.getLoginIdAsLong());
        remittance.setAuditByName(StpUtil.getLoginIdAsString());
        remittance.setAuditTime(LocalDateTime.now());
        remittance.setAuditRemark(auditRemark);
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        
        // 完成Flowable任务
        taskService.complete(task.getId(), variables);
        
        log.info("水单审核{}, 水单ID: {}, 任务ID: {}, 备注: {}", approved ? "通过" : "驳回", remittanceId, task.getId(), auditRemark);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeAudit(Long remittanceId) {
        DeclarationRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("水单不存在");
        }
        if (remittance.getStatus() != 2) {
            throw new RuntimeException("只有已审核状态的水单可以反审核");
        }

        // 重置状态为草稿
        remittance.setStatus(0);
        // 清除审核相关数据
        remittance.setTaxRate(null);
        remittance.setBankAccountId(null);
        remittance.setBankAccountName(null);
        remittance.setBankFeeRate(null);
        remittance.setBankFee(null);
        remittance.setCreditedAmount(null);
        remittance.setAuditBy(null);
        remittance.setAuditByName(null);
        remittance.setAuditTime(null);
        remittance.setAuditRemark(null);
        remittance.setSubmitTime(null);
        remittance.setProcessInstanceId(null);
        remittance.setUpdateBy(StpUtil.getLoginIdAsLong());

        boolean result = updateById(remittance);
        log.info("水单反审核成功, 水单ID: {}", remittanceId);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean relateToForm(Long remittanceId, Long formId, BigDecimal amount, Integer relationType) {
        // 检查水单是否存在
        DeclarationRemittance remittance = getById(remittanceId);
        if (remittance == null) {
            throw new RuntimeException("水单不存在");
        }

        // 检查是否已经关联
        LambdaQueryWrapper<RemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemittanceFormRelation::getRemittanceId, remittanceId)
               .eq(RemittanceFormRelation::getFormId, formId);
        Long count = relationDao.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("该水单已关联此申报单");
        }

        // 关联金额必须由调用方明确传入且大于 0；不再兽底为水单全额，避免前端传 0 时被错误扩大
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("关联金额必填且必须大于 0");
        }

        // 校验关联金额不超过水单总金额
        if (amount != null && remittance.getRemittanceAmount() != null) {
            // 查询已有关联的总金额
            LambdaQueryWrapper<RemittanceFormRelation> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(RemittanceFormRelation::getRemittanceId, remittanceId);
            List<RemittanceFormRelation> existingRelations = relationDao.selectList(existWrapper);
            BigDecimal existingTotal = existingRelations.stream()
                    .map(r -> r.getRelationAmount() != null ? r.getRelationAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newTotal = existingTotal.add(amount);
            if (newTotal.compareTo(remittance.getRemittanceAmount()) > 0) {
                throw new RuntimeException(String.format(
                        "关联金额超出水单总金额！水单金额: %s, 已关联: %s, 本次: %s, 合计: %s",
                        remittance.getRemittanceAmount(), existingTotal, amount, newTotal));
            }
        }

        // 创建关联
        RemittanceFormRelation relation = new RemittanceFormRelation();
        relation.setRemittanceId(remittanceId);
        relation.setFormId(formId);
        relation.setRelationType(relationType != null ? relationType : 1);
        relation.setRelationAmount(amount);
        relation.setCreateBy(StpUtil.getLoginIdAsLong());
        
        relationDao.insert(relation);
        log.info("水单关联申报单成功, 水单ID: {}, 申报单ID: {}, 关联金额: {}", remittanceId, formId, amount);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unrelateFromForm(Long remittanceId, Long formId) {
        LambdaQueryWrapper<RemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemittanceFormRelation::getRemittanceId, remittanceId)
               .eq(RemittanceFormRelation::getFormId, formId);
        
        int deleted = relationDao.delete(wrapper);
        log.info("取消水单与申报单关联, 水单ID: {}, 申报单ID: {}, 删除记录数: {}", remittanceId, formId, deleted);
        return deleted > 0;
    }

    @Override
    public List<Map<String, Object>> getRelatedForms(Long remittanceId) {
        LambdaQueryWrapper<RemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemittanceFormRelation::getRemittanceId, remittanceId);
        List<RemittanceFormRelation> relations = relationDao.selectList(wrapper);

        return relations.stream().map(relation -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("relationId", relation.getId());
            map.put("formId", relation.getFormId());
            map.put("relationType", relation.getRelationType());
            map.put("relationAmount", relation.getRelationAmount());
            map.put("createTime", relation.getCreateTime());

            // 使用DAO层查询申报单详情，避免循环依赖
            DeclarationForm form = declarationFormDao.selectById(relation.getFormId());
            if (form != null) {
                map.put("formNo", form.getFormNo());
                map.put("formDate", form.getDeclarationDate());
                map.put("totalAmount", form.getTotalAmount());
                map.put("currency", form.getCurrency());
                map.put("entityId", form.getEntityId());
                // 使用shipperCompany替代不存在的customerName
                map.put("customerName", form.getShipperCompany());
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRemittancesByFormId(Long formId) {
        LambdaQueryWrapper<RemittanceFormRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RemittanceFormRelation::getFormId, formId);
        List<RemittanceFormRelation> relations = relationDao.selectList(wrapper);

        List<Long> remittanceIds = relations.stream()
                .map(RemittanceFormRelation::getRemittanceId)
                .collect(Collectors.toList());

        if (remittanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<DeclarationRemittance> remittances = listByIds(remittanceIds);
        
        // 组装返回数据
        Map<Long, RemittanceFormRelation> relationMap = relations.stream()
                .collect(Collectors.toMap(RemittanceFormRelation::getRemittanceId, r -> r));

        return remittances.stream().map(remittance -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", remittance.getId());
            map.put("remittanceNo", remittance.getRemittanceNo());
            map.put("remittanceType", remittance.getRemittanceType());
            map.put("remittanceName", remittance.getRemittanceName());
            map.put("remittanceDate", remittance.getRemittanceDate());
            map.put("remittanceAmount", remittance.getRemittanceAmount());
            map.put("currency", remittance.getCurrency());
            map.put("status", remittance.getStatus());
            map.put("taxRate", remittance.getTaxRate());
            map.put("bankAccountName", remittance.getBankAccountName());
            map.put("bankFee", remittance.getBankFee());
            map.put("bankFeeRate", remittance.getBankFeeRate());
            map.put("internalBankFee", remittance.getInternalBankFee());
            map.put("creditedAmount", remittance.getCreditedAmount());
            map.put("remarks", remittance.getRemarks());
            map.put("photoUrl", remittance.getPhotoUrl());
            map.put("relationType", relationMap.get(remittance.getId()).getRelationType());
            map.put("relationAmount", relationMap.get(remittance.getId()).getRelationAmount());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public IPage<DeclarationRemittance> getPage(PageParam pageParam, Integer remittanceType, Integer status, String remittanceNo, String relationStatus) {
        LambdaQueryWrapper<DeclarationRemittance> wrapper = new LambdaQueryWrapper<>();

        // 数据权限控制：普通用户只能查看自己创建的水单
        try {
            Long currentUserId = StpUtil.getLoginIdAsLong();
            boolean isAdmin = StpUtil.hasRole("ADMIN");
            boolean isFinance = StpUtil.hasRole("FINANCE");
            boolean isDeptAdmin = StpUtil.hasRole("DEPT_ADMIN");
            if (!isAdmin && !isFinance && !isDeptAdmin) {
                wrapper.eq(DeclarationRemittance::getCreateBy, currentUserId);
            }
        } catch (Exception e) {
            // ignore
        }

        if (remittanceType != null) {
            wrapper.eq(DeclarationRemittance::getRemittanceType, remittanceType);
        }
        if (status != null) {
            wrapper.eq(DeclarationRemittance::getStatus, status);
        }
        if (remittanceNo != null && !remittanceNo.isEmpty()) {
            wrapper.like(DeclarationRemittance::getRemittanceNo, remittanceNo);
        }
        // 关联状态筛选仅对已审核水单有意义
        if (relationStatus != null && !relationStatus.isEmpty()) {
            wrapper.eq(DeclarationRemittance::getStatus, 2);
        }

        wrapper.orderByDesc(DeclarationRemittance::getCreateTime);

        // 如果有关联状态筛选，需要先查全量再内存过滤+手动分页
        boolean needRelationFilter = relationStatus != null && !relationStatus.isEmpty();

        List<DeclarationRemittance> allRecords;
        long totalCount;

        if (needRelationFilter) {
            // 查全量（不分页）
            allRecords = list(wrapper);
            totalCount = allRecords.size();
            // 计算关联金额
            fillTotalRelatedAmount(allRecords);
            // 内存过滤
            allRecords = allRecords.stream().filter(r -> {
                String rs = computeRelationStatus(r);
                return relationStatus.equals(rs);
            }).collect(Collectors.toList());
            totalCount = allRecords.size();
            // 手动分页
            int from = (int) ((pageParam.getCurrent() - 1) * pageParam.getSize());
            int to = Math.min(from + (int) pageParam.getSize(), allRecords.size());
            List<DeclarationRemittance> pageRecords = from < allRecords.size() ? allRecords.subList(from, to) : new ArrayList<>();
            Page<DeclarationRemittance> manualPage = new Page<>(pageParam.getCurrent(), pageParam.getSize(), totalCount);
            manualPage.setRecords(pageRecords);
            return manualPage;
        } else {
            Page<DeclarationRemittance> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
            IPage<DeclarationRemittance> result = page(page, wrapper);
            fillTotalRelatedAmount(result.getRecords());
            return result;
        }
    }

    /**
     * 为水单列表填充已关联金额合计
     */
    private void fillTotalRelatedAmount(List<DeclarationRemittance> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> remittanceIds = records.stream().map(DeclarationRemittance::getId).collect(Collectors.toList());
        LambdaQueryWrapper<RemittanceFormRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.in(RemittanceFormRelation::getRemittanceId, remittanceIds);
        List<RemittanceFormRelation> allRelations = relationDao.selectList(relWrapper);
        Map<Long, BigDecimal> totalAmountMap = new HashMap<>();
        for (RemittanceFormRelation rel : allRelations) {
            BigDecimal amt = rel.getRelationAmount() != null ? rel.getRelationAmount() : BigDecimal.ZERO;
            totalAmountMap.merge(rel.getRemittanceId(), amt, BigDecimal::add);
        }
        for (DeclarationRemittance r : records) {
            r.setTotalRelatedAmount(totalAmountMap.getOrDefault(r.getId(), BigDecimal.ZERO));
        }
    }

    /**
     * 计算单条水单的关联状态
     */
    private String computeRelationStatus(DeclarationRemittance r) {
        if (r.getStatus() == null || r.getStatus() != 2) return "N/A";
        BigDecimal related = r.getTotalRelatedAmount() != null ? r.getTotalRelatedAmount() : BigDecimal.ZERO;
        BigDecimal total = r.getRemittanceAmount() != null ? r.getRemittanceAmount() : BigDecimal.ZERO;
        if (related.compareTo(BigDecimal.ZERO) <= 0) return "UNRELATED";
        if (related.compareTo(total) >= 0) return "RELATED";
        return "PARTIAL";
    }

    @Override
    public BigDecimal calculateBankFee(Long bankAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BankAccountConfig bankAccount = bankAccountConfigService.getById(bankAccountId);
        if (bankAccount == null || bankAccount.getServiceFeeRate() == null) {
            return BigDecimal.ZERO;
        }

        // 手续费 = 金额 * 手续费率（serviceFeeRate已是小数，如 0.02 表示 2%）
        BigDecimal calculatedFee = amount.multiply(bankAccount.getServiceFeeRate()).setScale(4, RoundingMode.HALF_UP);

        // 如果设置了最低操作费，且计算出的手续费低于最低操作费，则按最低操作费收取
        if (bankAccount.getMinServiceFee() != null && calculatedFee.compareTo(bankAccount.getMinServiceFee()) < 0) {
            return bankAccount.getMinServiceFee();
        }

        return calculatedFee;
    }

    /**
     * 计算内部手续费（人民币）
     * 公式：汇率 × 收汇金额 × 银行手续费率
     */
    private BigDecimal calculateInternalBankFee(Long bankAccountId, BigDecimal amount, BigDecimal taxRate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || taxRate == null) {
            return BigDecimal.ZERO;
        }

        BankAccountConfig bankAccount = bankAccountConfigService.getById(bankAccountId);
        if (bankAccount == null || bankAccount.getServiceFeeRate() == null) {
            return BigDecimal.ZERO;
        }

        // 内部手续费 = 汇率 × 收汇金额 × 银行手续费率
        return taxRate.multiply(amount)
                .multiply(bankAccount.getServiceFeeRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成水单编号: SL + yyyyMMdd + 4位序号
     */
    private String generateRemittanceNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SL" + dateStr;
        
        // 查询当天最大序号
        LambdaQueryWrapper<DeclarationRemittance> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(DeclarationRemittance::getRemittanceNo, prefix)
               .orderByDesc(DeclarationRemittance::getRemittanceNo)
               .last("LIMIT 1");
        
        DeclarationRemittance lastRemittance = getOne(wrapper);
        int seq = 1;
        if (lastRemittance != null && lastRemittance.getRemittanceNo() != null) {
            String lastNo = lastRemittance.getRemittanceNo();
            String lastSeq = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(lastSeq) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        
        return prefix + String.format("%04d", seq);
    }

    @Override
    public String getProcessInstanceId(Long remittanceId) {
        DeclarationRemittance remittance = getById(remittanceId);
        return remittance != null ? remittance.getProcessInstanceId() : null;
    }

    @Override
    public List<Map<String, Object>> getPendingAuditTasks() {
        // 查询FINANCE_MANAGER角色的待审核任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskDefinitionKey("financeAudit")
                .orderByTaskCreateTime()
                .desc()
                .list();
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            String remittanceIdStr = (String) taskService.getVariable(task.getId(), "remittanceId");
            if (remittanceIdStr != null) {
                Long remittanceId = Long.parseLong(remittanceIdStr);
                DeclarationRemittance remittance = getById(remittanceId);
                if (remittance != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("taskId", task.getId());
                    map.put("taskName", task.getName());
                    map.put("processInstanceId", task.getProcessInstanceId());
                    map.put("remittanceId", remittance.getId());
                    map.put("remittanceNo", remittance.getRemittanceNo());
                    map.put("remittanceType", remittance.getRemittanceType());
                    map.put("remittanceName", remittance.getRemittanceName());
                    map.put("remittanceAmount", remittance.getRemittanceAmount());
                    map.put("currency", remittance.getCurrency());
                    map.put("submitTime", remittance.getSubmitTime());
                    map.put("createTime", task.getCreateTime());
                    result.add(map);
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasApprovedRemittance(Long formId) {
        // 查询关联表中该申报单关联的所有收汇水单 ID
        LambdaQueryWrapper<RemittanceFormRelation> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(RemittanceFormRelation::getFormId, formId);
        List<RemittanceFormRelation> relations = relationDao.selectList(relWrapper);

        if (relations.isEmpty()) {
            return false;
        }

        List<Long> remittanceIds = relations.stream()
                .map(RemittanceFormRelation::getRemittanceId)
                .collect(Collectors.toList());

        // 查询是否有 status=2（已审核）的收汇水单
        LambdaQueryWrapper<DeclarationRemittance> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DeclarationRemittance::getId, remittanceIds)
               .eq(DeclarationRemittance::getStatus, 2);
        return count(wrapper) > 0;
    }
}
