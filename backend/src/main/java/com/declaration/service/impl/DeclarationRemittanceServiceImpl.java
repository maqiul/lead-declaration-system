package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationRemittanceDao;
import com.declaration.entity.DeclarationRemittance;
import com.declaration.service.DeclarationRemittanceService;
import org.springframework.stereotype.Service;

/**
 * 水单信息服务实现类
 */
@Service
public class DeclarationRemittanceServiceImpl extends ServiceImpl<DeclarationRemittanceDao, DeclarationRemittance> implements DeclarationRemittanceService {
}
