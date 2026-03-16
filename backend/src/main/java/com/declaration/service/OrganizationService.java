package com.declaration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.declaration.common.PageParam;
import com.declaration.entity.Organization;

import java.util.List;

/**
 * 组织机构服务接口
 *
 * @author Administrator
 * @since 2026-03-13
 */
public interface OrganizationService extends IService<Organization> {

    /**
     * 获取组织树
     *
     * @return 组织树
     */
    List<Organization> getOrgTree();

    /**
     * 分页查询组织列表
     *
     * @param pageParam 分页参数
     * @param organization 查询条件
     * @return 组织分页数据
     */
    IPage<Organization> getOrgPage(PageParam pageParam, Organization organization);

    /**
     * 新增组织
     *
     * @param organization 组织信息
     * @return 是否成功
     */
    boolean saveOrg(Organization organization);

    /**
     * 修改组织
     *
     * @param organization 组织信息
     * @return 是否成功
     */
    boolean updateOrg(Organization organization);

    /**
     * 删除组织
     *
     * @param id 组织ID
     * @return 是否成功
     */
    boolean deleteOrg(Long id);

    /**
     * 获取用户所属组织
     *
     * @param userId 用户ID
     * @return 组织列表
     */
    List<Organization> getUserOrgs(Long userId);
}