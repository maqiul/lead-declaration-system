package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.CustomerConfig;

import java.util.List;

/**
 * 常用客户配置服务接口
 */
public interface CustomerConfigService extends IService<CustomerConfig> {

    /**
     * 获取当前用户启用状态的客户列表（下拉选择用）
     * @param userId 用户ID
     * @return 启用的客户列表
     */
    List<CustomerConfig> getEnabledListByUserId(Long userId);
}
