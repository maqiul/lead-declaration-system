package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.DeclarationMaterialTemplate;
import com.declaration.entity.MaterialTemplateBinding;

import java.util.List;
import java.util.Map;

public interface DeclarationMaterialTemplateService extends IService<DeclarationMaterialTemplate> {
    /** 获取启用的模板列表（按排序升序） */
    List<DeclarationMaterialTemplate> listEnabled();

    /** 获取指定环节的启用模板列表（按排序升序） */
    List<DeclarationMaterialTemplate> listByStage(String stage);

    /** 合法的 stage 枚举值集合（用于 Controller 校验） */
    List<String> validStages();

    /** 查询模板的绑定规则 */
    List<MaterialTemplateBinding> getBindings(Long templateId);

    /** 保存模板的绑定规则（先删后插） */
    void saveBindings(Long templateId, List<MaterialTemplateBinding> bindings);

    /** 批量查询所有模板的绑定规则（templateId -> bindings） */
    Map<Long, List<MaterialTemplateBinding>> batchGetBindings(List<Long> templateIds);
}
