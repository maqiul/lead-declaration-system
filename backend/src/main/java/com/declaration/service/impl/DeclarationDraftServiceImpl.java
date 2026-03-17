package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.DeclarationDraftDao;
import com.declaration.entity.DeclarationDraft;
import com.declaration.service.DeclarationDraftService;
import org.springframework.stereotype.Service;

/**
 * 申报单草稿服务实现类
 */
@Service
public class DeclarationDraftServiceImpl extends ServiceImpl<DeclarationDraftDao, DeclarationDraft> implements DeclarationDraftService {
}
