package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.TradeTermDao;
import com.declaration.dao.TradeTermTransportModeDao;
import com.declaration.entity.TradeTerm;
import com.declaration.service.TradeTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;

/**
 * 贸易方式(Incoterms)配置服务实现类
 */
@Service
public class TradeTermServiceImpl extends ServiceImpl<TradeTermDao, TradeTerm> implements TradeTermService {

    @Autowired
    private TradeTermTransportModeDao tradeTermTransportModeDao;

    @Override
    public Page<TradeTerm> getPage(int page, int size, String keyword, Integer status) {
        Page<TradeTerm> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TradeTerm> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(TradeTerm::getCode, keyword)
                .or()
                .like(TradeTerm::getName, keyword)
                .or()
                .like(TradeTerm::getChineseName, keyword)
                .or()
                .like(TradeTerm::getGroupName, keyword)
            );
        }

        if (status != null) {
            wrapper.eq(TradeTerm::getStatus, status);
        }

        wrapper.orderByAsc(TradeTerm::getSort)
               .orderByDesc(TradeTerm::getCreateTime);

        Page<TradeTerm> result = this.page(pageParam, wrapper);
        // 填充关联运输方式
        for (TradeTerm term : result.getRecords()) {
            term.setTransportModes(tradeTermTransportModeDao.selectTransportModesByTradeTermCode(term.getCode()));
        }
        return result;
    }

    @Override
    @Cacheable(value = "sys:dict:trade-terms")
    public List<TradeTerm> getEnabledList() {
        LambdaQueryWrapper<TradeTerm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TradeTerm::getStatus, 1)
               .orderByAsc(TradeTerm::getSort)
               .orderByAsc(TradeTerm::getChineseName);
        List<TradeTerm> list = this.list(wrapper);
        // 填充关联运输方式
        for (TradeTerm term : list) {
            term.setTransportModes(tradeTermTransportModeDao.selectTransportModesByTradeTermCode(term.getCode()));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 内部用 super.save 写入，不会触发 save() 上的 @CacheEvict，必须在入口方法上重复声明
    @CacheEvict(value = "sys:dict:trade-terms", allEntries = true)
    public boolean saveTradeTerm(TradeTerm tradeTerm) {
        boolean saved = super.save(tradeTerm);
        if (saved && tradeTerm.getTransportModes() != null) {
            for (String transportModeCode : tradeTerm.getTransportModes()) {
                tradeTermTransportModeDao.insert(tradeTerm.getCode(), transportModeCode);
            }
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 同理：super.updateById 绕过代理，关联运输方式的变更必须靠这里的缓存清除才能反映到下拉框
    @CacheEvict(value = "sys:dict:trade-terms", allEntries = true)
    public boolean updateTradeTerm(TradeTerm tradeTerm) {
        boolean updated = super.updateById(tradeTerm);
        if (updated) {
            // 先删除旧关联
            tradeTermTransportModeDao.deleteByTradeTermCode(tradeTerm.getCode());
            // 再插入新关联
            if (tradeTerm.getTransportModes() != null) {
                for (String transportModeCode : tradeTerm.getTransportModes()) {
                    tradeTermTransportModeDao.insert(tradeTerm.getCode(), transportModeCode);
                }
            }
        }
        return updated;
    }

    @Override
    public List<String> getTransportModesByCode(String tradeTermCode) {
        return tradeTermTransportModeDao.selectTransportModesByTradeTermCode(tradeTermCode);
    }

    @Override
    @CacheEvict(value = "sys:dict:trade-terms", allEntries = true)
    public boolean save(TradeTerm entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:trade-terms", allEntries = true)
    public boolean updateById(TradeTerm entity) {
        return super.updateById(entity);
    }

    @Override
    @CacheEvict(value = "sys:dict:trade-terms", allEntries = true)
    public boolean removeById(Serializable id) {
        TradeTerm term = this.getById(id);
        if (term != null) {
            tradeTermTransportModeDao.deleteByTradeTermCode(term.getCode());
        }
        return super.removeById(id);
    }
}
