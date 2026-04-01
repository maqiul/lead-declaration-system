package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.BankAccountConfig;

import java.util.List;

/**
 * 银行账户配置服务接口
 *
 * @author Administrator
 * @since 2026-03-17
 */
public interface BankAccountConfigService extends IService<BankAccountConfig> {

    /**
     * 根据币种获取启用状态的银行账户列表
     * @param currency 币种 (可选)
     * @return 银行账户列表
     */
    List<BankAccountConfig> getEnabledList(String currency);
}