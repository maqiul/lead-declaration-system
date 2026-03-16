package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationCartonDao;
import com.declaration.entity.DeclarationCarton;
import com.declaration.service.DeclarationCartonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 申报单箱子信息服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
public class DeclarationCartonServiceImpl extends ServiceImpl<DeclarationCartonDao, DeclarationCarton> implements DeclarationCartonService {
}