package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationMaterialTemplateDao;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.service.DeclarationMaterialTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class DeclarationMaterialTemplateServiceImpl
        extends ServiceImpl<DeclarationMaterialTemplateDao, DeclarationMaterialTemplate>
        implements DeclarationMaterialTemplateService {

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
}
