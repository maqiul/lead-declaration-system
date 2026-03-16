package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationProductDao;
import com.declaration.entity.DeclarationProduct;
import com.declaration.service.DeclarationProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 申报单产品明细服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
public class DeclarationProductServiceImpl extends ServiceImpl<DeclarationProductDao, DeclarationProduct> implements DeclarationProductService {
}