package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.ContractGenerationDao;
import com.declaration.entity.ContractGeneration;
import com.declaration.service.ContractGenerationService;
import org.springframework.stereotype.Service;

/**
 * 合同生成记录服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class ContractGenerationServiceImpl extends ServiceImpl<ContractGenerationDao, ContractGeneration> implements ContractGenerationService {
}