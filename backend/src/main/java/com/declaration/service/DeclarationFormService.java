package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.dto.DeclarationStatisticsDTO;
import com.declaration.entity.DeclarationForm;

/**
 * 出口申报单服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface DeclarationFormService extends IService<DeclarationForm> {

    /**
     * 保存申报单及其关联数据
     * @param form 申报单数据
     * @return 是否保存成功
     */
    boolean saveDeclarationForm(DeclarationForm form);

    /**
     * 根据ID获取完整的申报单信息（包含产品和箱子信息）
     * @param id 申报单ID
     * @return 完整的申报单信息
     */
    DeclarationForm getFullDeclarationForm(Long id);

    /**
     * 更新申报单及其关联数据
     * @param form 申报单数据
     * @return 是否更新成功
     */
    boolean updateDeclarationForm(DeclarationForm form);

    /**
     * 获取申报单统计数据
     * @return 统计数据
     */
    DeclarationStatisticsDTO getStatistics();
}