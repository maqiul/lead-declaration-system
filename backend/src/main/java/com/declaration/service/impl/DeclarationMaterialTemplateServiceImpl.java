package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationMaterialTemplateDao;
import com.declaration.dao.MaterialTemplateBindingDao;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.MaterialTemplateBinding;
import com.declaration.service.DeclarationMaterialTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeclarationMaterialTemplateServiceImpl
        extends ServiceImpl<DeclarationMaterialTemplateDao, DeclarationMaterialTemplate>
        implements DeclarationMaterialTemplateService {

    private final MaterialTemplateBindingDao bindingDao;

    /** 合法的环节枚举值 */
    public static final List<String> VALID_STAGES = Arrays.asList(
            "MATERIAL_SUBMIT", "SUPPLEMENT", "INVOICE"
    );

    @Override
    public List<DeclarationMaterialTemplate> listEnabled() {
        LambdaQueryWrapper<DeclarationMaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationMaterialTemplate::getEnabled, 1)
               .orderByAsc(DeclarationMaterialTemplate::getSort)
               .orderByAsc(DeclarationMaterialTemplate::getId);
        return this.list(wrapper);
    }

    @Override
    public List<DeclarationMaterialTemplate> listByStage(String stage) {
        LambdaQueryWrapper<DeclarationMaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationMaterialTemplate::getEnabled, 1);
        if (StringUtils.hasText(stage)) {
            wrapper.eq(DeclarationMaterialTemplate::getStage, stage);
        }
        wrapper.orderByAsc(DeclarationMaterialTemplate::getSort)
               .orderByAsc(DeclarationMaterialTemplate::getId);
        return this.list(wrapper);
    }

    @Override
    public List<String> validStages() {
        return VALID_STAGES;
    }

    @Override
    public List<MaterialTemplateBinding> getBindings(Long templateId) {
        if (templateId == null) return Collections.emptyList();
        LambdaQueryWrapper<MaterialTemplateBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MaterialTemplateBinding::getTemplateId, templateId);
        return bindingDao.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBindings(Long templateId, List<MaterialTemplateBinding> bindings) {
        if (templateId == null) return;
        // 先删除原有绑定
        bindingDao.delete(new LambdaQueryWrapper<MaterialTemplateBinding>()
                .eq(MaterialTemplateBinding::getTemplateId, templateId));
        // 插入新绑定
        if (bindings != null) {
            for (MaterialTemplateBinding b : bindings) {
                b.setId(null);
                b.setTemplateId(templateId);
                // 空字符串转 null
                if (b.getFlowTemplateCode() != null && b.getFlowTemplateCode().trim().isEmpty()) {
                    b.setFlowTemplateCode(null);
                }
                if (b.getTransportModeCode() != null && b.getTransportModeCode().trim().isEmpty()) {
                    b.setTransportModeCode(null);
                }
                bindingDao.insert(b);
            }
        }
    }

    @Override
    public Map<Long, List<MaterialTemplateBinding>> batchGetBindings(List<Long> templateIds) {
        Map<Long, List<MaterialTemplateBinding>> result = new HashMap<>();
        if (templateIds == null || templateIds.isEmpty()) return result;
        LambdaQueryWrapper<MaterialTemplateBinding> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(MaterialTemplateBinding::getTemplateId, templateIds);
        List<MaterialTemplateBinding> list = bindingDao.selectList(wrapper);
        for (MaterialTemplateBinding b : list) {
            result.computeIfAbsent(b.getTemplateId(), k -> new ArrayList<>()).add(b);
        }
        return result;
    }
}
