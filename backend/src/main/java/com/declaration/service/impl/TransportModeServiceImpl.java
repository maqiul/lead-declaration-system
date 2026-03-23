package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.TransportModeDao;
import com.declaration.entity.TransportMode;
import com.declaration.service.TransportModeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 运输方式配置服务实现类
 *
 * @author Administrator
 * @since 2026-03-23
 */
@Service
public class TransportModeServiceImpl extends ServiceImpl<TransportModeDao, TransportMode> implements TransportModeService {

    @Override
    public Page<TransportMode> getPage(int page, int size, String keyword, Integer status) {
        Page<TransportMode> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<TransportMode> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（按name/chineseName/code模糊搜索）
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                .like(TransportMode::getName, keyword)
                .or()
                .like(TransportMode::getChineseName, keyword)
                .or()
                .like(TransportMode::getCode, keyword)
            );
        }
        
        // 状态过滤
        if (status != null) {
            wrapper.eq(TransportMode::getStatus, status);
        }
        
        // 排序
        wrapper.orderByAsc(TransportMode::getSort)
               .orderByDesc(TransportMode::getCreateTime);
        
        return this.page(pageParam, wrapper);
    }

    @Override
    public List<TransportMode> getEnabledList() {
        LambdaQueryWrapper<TransportMode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransportMode::getStatus, 1)
               .orderByAsc(TransportMode::getSort)
               .orderByAsc(TransportMode::getChineseName);
        return this.list(wrapper);
    }
}
