package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.entity.ProductTypeConfig;

import java.util.List;

/**
 * HS商品类型配置服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface ProductTypeConfigService extends IService<ProductTypeConfig> {

    /**
     * 分页查询商品类型列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词
     * @return 分页结果
     */
    IPage<ProductTypeConfig> getPage(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 获取所有启用的商品类型
     *
     * @return 商品类型列表
     */
    List<ProductTypeConfig> getEnabledList();

    /**
     * 根据HS编码获取商品类型
     *
     * @param hsCode HS编码
     * @return 商品类型
     */
    ProductTypeConfig getByHsCode(String hsCode);
}
