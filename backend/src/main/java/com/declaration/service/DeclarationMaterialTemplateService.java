package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationMaterialTemplate;

import java.util.List;

public interface DeclarationMaterialTemplateService extends IService<DeclarationMaterialTemplate> {
    /** 获取启用的模板列表（按排序升序） */
    List<DeclarationMaterialTemplate> listEnabled();

    /** 获取指定环节的启用模板列表（按排序升序） */
    List<DeclarationMaterialTemplate> listByStage(String stage);

    /** 合法的 stage 枚举值集合（用于 Controller 校验） */
    List<String> validStages();
}
