package com.declaration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.CurrencyInfo;
import java.util.List;

/**
 * 货币信息服务接口
 *
 * @author Administrator
 * @since 2026-03-23
 */
public interface CurrencyInfoService extends IService<CurrencyInfo> {
    
    /**
     * 根据货币代码获取货币信息
     * @param code 货币代码
     * @return 货币信息
     */
    CurrencyInfo getByCurrencyCode(String code);
    
    /**
     * 根据中文名称获取货币信息
     * @param chineseName 中文名称
     * @return 货币信息
     */
    CurrencyInfo getByChineseName(String chineseName);

    /**
     * 获取所有启用的货币信息
     * @return 货币信息列表
     */
    List<CurrencyInfo> getEnabledList();
}
