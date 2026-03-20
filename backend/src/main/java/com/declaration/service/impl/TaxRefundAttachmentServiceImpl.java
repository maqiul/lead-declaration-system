package com.declaration.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.dao.TaxRefundAttachmentDao;
import com.declaration.entity.TaxRefundAttachment;
import com.declaration.service.TaxRefundAttachmentService;
import org.springframework.stereotype.Service;

/**
 * 税务退费申请附件服务实现类
 *
 * @author Administrator
 * @since 2026-03-17
 */
@Service
public class TaxRefundAttachmentServiceImpl extends ServiceImpl<TaxRefundAttachmentDao, TaxRefundAttachment> implements TaxRefundAttachmentService {
}