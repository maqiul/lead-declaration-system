package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.EntityConfig;

import java.util.List;

/**
 * 主体配置服务接口
 *
 * @author Administrator
 * @since 2026-04-28
 */
public interface EntityConfigService extends IService<EntityConfig> {

    /**
     * 获取启用状态的主体列表（下拉选择用）
     * @return 启用的主体列表
     */
    List<EntityConfig> getEnabledList();

    /**
     * 获取默认主体
     * @return 默认主体，如无则返回第一个启用的主体
     */
    EntityConfig getDefault();
}
