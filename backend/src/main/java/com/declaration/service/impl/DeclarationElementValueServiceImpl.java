package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationElementValueDao;
import com.declaration.entity.DeclarationElementValue;
import com.declaration.service.DeclarationElementValueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 申报要素填写记录服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
public class DeclarationElementValueServiceImpl extends ServiceImpl<DeclarationElementValueDao, DeclarationElementValue> implements DeclarationElementValueService {
}