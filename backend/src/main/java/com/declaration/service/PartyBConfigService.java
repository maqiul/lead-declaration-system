package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.PartyBConfig;

import java.util.List;

/**
 * 乙方配置服务接口
 */
public interface PartyBConfigService extends IService<PartyBConfig> {

    /**
     * 获取当前用户启用状态的乙方列表（下拉选择用）
     * @param userId 用户ID
     * @return 启用的乙方列表
     */
    List<PartyBConfig> getEnabledListByUserId(Long userId);
}
