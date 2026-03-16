package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationCartonProductDao;
import com.declaration.entity.DeclarationCartonProduct;
import com.declaration.service.DeclarationCartonProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 箱子产品关联服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
public class DeclarationCartonProductServiceImpl extends ServiceImpl<DeclarationCartonProductDao, DeclarationCartonProduct> implements DeclarationCartonProductService {
}