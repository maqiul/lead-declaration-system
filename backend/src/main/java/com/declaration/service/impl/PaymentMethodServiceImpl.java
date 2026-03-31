package com.declaration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.PaymentMethodDao;
import com.declaration.entity.PaymentMethod;
import com.declaration.service.PaymentMethodService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付方式配置服务实现类
 */
@Service
public class PaymentMethodServiceImpl extends ServiceImpl<PaymentMethodDao, PaymentMethod> implements PaymentMethodService {

    @Override
    public Page<PaymentMethod> getPage(Integer pageNum, Integer size, String keyword, Integer status) {
        Page<PaymentMethod> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(PaymentMethod::getName, keyword)
                    .or().like(PaymentMethod::getChineseName, keyword)
                    .or().like(PaymentMethod::getCode, keyword));
        }

        if (status != null) {
            wrapper.eq(PaymentMethod::getStatus, status);
        }

        // 默认按排序号升序排列，如果排序号相同则按ID升序
        wrapper.orderByAsc(PaymentMethod::getSort).orderByAsc(PaymentMethod::getId);

        return this.page(page, wrapper);
    }

    @Override
    public List<PaymentMethod> getEnabledList() {
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentMethod::getStatus, 1);
        wrapper.orderByAsc(PaymentMethod::getSort).orderByAsc(PaymentMethod::getId);
        return this.list(wrapper);
    }
}
