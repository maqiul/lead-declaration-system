package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.ContractTemplateDao;
import com.declaration.entity.ContractTemplate;
import com.declaration.service.ContractTemplateService;
import org.springframework.stereotype.Service;

/**
 * 合同模板服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class ContractTemplateServiceImpl extends ServiceImpl<ContractTemplateDao, ContractTemplate> implements ContractTemplateService {
}