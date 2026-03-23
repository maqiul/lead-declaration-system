package com.declaration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.TransportMode;

import java.util.List;

/**
 * 运输方式配置服务接口
 *
 * @author Administrator
 * @since 2026-03-23
 */
public interface TransportModeService extends IService<TransportMode> {
    
    /**
     * 分页查询运输方式
     *
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键词
     * @param status  状态
     * @return 分页结果
     */
    Page<TransportMode> getPage(int page, int size, String keyword, Integer status);
    
    /**
     * 获取启用的运输方式列表
     *
     * @return 启用的运输方式列表
     */
    List<TransportMode> getEnabledList();
}
