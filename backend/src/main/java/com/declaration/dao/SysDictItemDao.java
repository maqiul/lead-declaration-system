package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统字典项 DAO
 *
 * @author Administrator
 * @since 2026-07-07
 */
@Mapper
public interface SysDictItemDao extends BaseMapper<SysDictItem> {
}
