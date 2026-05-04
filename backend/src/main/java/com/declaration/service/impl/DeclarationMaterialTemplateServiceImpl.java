package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationMaterialTemplateDao;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.service.DeclarationMaterialTemplateService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeclarationMaterialTemplateServiceImpl
        extends ServiceImpl<DeclarationMaterialTemplateDao, DeclarationMaterialTemplate>
        implements DeclarationMaterialTemplateService {

    @Override
    public List<DeclarationMaterialTemplate> listEnabled() {
        LambdaQueryWrapper<DeclarationMaterialTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeclarationMaterialTemplate::getEnabled, 1)
               .orderByAsc(DeclarationMaterialTemplate::getSort)
               .orderByAsc(DeclarationMaterialTemplate::getId);
        return this.list(wrapper);
    }
}
