package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.BankAccountConfigDao;
import com.declaration.entity.BankAccountConfig;
import com.declaration.service.BankAccountConfigService;
import org.springframework.stereotype.Service;

/**
 * 银行账户配置服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class BankAccountConfigServiceImpl extends ServiceImpl<BankAccountConfigDao, BankAccountConfig> implements BankAccountConfigService {
}