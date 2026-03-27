package com.declaration.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.declaration.entity.CityInfo;
import com.declaration.entity.UserOrg;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户组织关联DAO接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface UserOrgDao extends BaseMapper<UserOrg> {

}