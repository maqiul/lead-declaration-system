package com.declaration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.TradeTerm;

import java.util.List;

/**
 * 贸易方式(Incoterms)配置服务接口
 */
public interface TradeTermService extends IService<TradeTerm> {

    /**
     * 分页查询贸易方式
     */
    Page<TradeTerm> getPage(int page, int size, String keyword, Integer status);

    /**
     * 获取启用的贸易方式列表(含关联运输方式)
     */
    List<TradeTerm> getEnabledList();

    /**
     * 保存贸易方式及关联的运输方式
     */
    boolean saveTradeTerm(TradeTerm tradeTerm);

    /**
     * 更新贸易方式及关联的运输方式
     */
    boolean updateTradeTerm(TradeTerm tradeTerm);

    /**
     * 根据贸易方式代码获取关联的运输方式代码列表
     */
    List<String> getTransportModesByCode(String tradeTermCode);
}
