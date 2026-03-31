package com.declaration.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.PaymentMethod;

import java.util.List;

/**
 * 支付方式配置服务接口
 */
public interface PaymentMethodService extends IService<PaymentMethod> {

    /**
     * 分页查询支付方式
     *
     * @param page    页码
     * @param size    每页大小
     * @param keyword 关键词(名称/代码)
     * @param status  状态
     * @return 分页结果
     */
    Page<PaymentMethod> getPage(Integer page, Integer size, String keyword, Integer status);

    /**
     * 获取所有启用的支付方式列表
     *
     * @return 启用的支付方式列表
     */
    List<PaymentMethod> getEnabledList();
}
