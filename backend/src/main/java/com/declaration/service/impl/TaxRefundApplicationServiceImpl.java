package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.TaxRefundApplicationDao;
import com.declaration.entity.TaxRefundApplication;
import com.declaration.service.TaxRefundApplicationService;
import org.springframework.stereotype.Service;

/**
 * 税务退费申请服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class TaxRefundApplicationServiceImpl extends ServiceImpl<TaxRefundApplicationDao, TaxRefundApplication> implements TaxRefundApplicationService {
}