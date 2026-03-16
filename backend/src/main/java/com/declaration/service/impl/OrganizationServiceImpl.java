package com.declaration.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.declaration.common.PageParam;
import com.declaration.dao.OrganizationDao;
import com.declaration.dao.UserOrgDao;
import com.declaration.entity.Organization;
import com.declaration.entity.UserOrg;
import com.declaration.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织机构服务实现类
 *
 * @author Administrator
 * @since 2026-03-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl extends ServiceImpl<OrganizationDao, Organization> implements OrganizationService {

    private final UserOrgDao userOrgDao;

    @Override
    public List<Organization> getOrgTree() {
        // 查询所有有效的组织
        List<Organization> organizations = this.list(
            new LambdaQueryWrapper<Organization>()
                .eq(Organization::getStatus, 1)
                .orderByAsc(Organization::getSort)
        );
        
        // 构建组织树
        return buildOrgTree(organizations);
    }

    @Override
    public IPage<Organization> getOrgPage(PageParam pageParam, Organization organization) {
        LambdaQueryWrapper<Organization> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (organization != null) {
            if (organization.getParentId() != null) {
                queryWrapper.eq(Organization::getParentId, organization.getParentId());
            }
            if (organization.getOrgName() != null && !organization.getOrgName().isEmpty()) {
                queryWrapper.like(Organization::getOrgName, organization.getOrgName());
            }
            if (organization.getOrgCode() != null && !organization.getOrgCode().isEmpty()) {
                queryWrapper.like(Organization::getOrgCode, organization.getOrgCode());
            }
            if (organization.getStatus() != null) {
                queryWrapper.eq(Organization::getStatus, organization.getStatus());
            }
        }
        
        queryWrapper.orderByAsc(Organization::getSort);
        
        // 分页查询
        Page<Organization> page = new Page<>(pageParam.getCurrent(), pageParam.getSize());
        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrg(Organization organization) {
        // 设置层级
        if (organization.getParentId() == null || organization.getParentId() == 0) {
            organization.setLevel(1);
        } else {
            Organization parentOrg = this.getById(organization.getParentId());
            if (parentOrg != null) {
                organization.setLevel(parentOrg.getLevel() + 1);
            }
        }
        
        return this.save(organization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrg(Organization organization) {
        // 更新层级信息
        if (organization.getParentId() != null) {
            if (organization.getParentId() == 0) {
                organization.setLevel(1);
            } else {
                Organization parentOrg = this.getById(organization.getParentId());
                if (parentOrg != null) {
                    organization.setLevel(parentOrg.getLevel() + 1);
                }
            }
        }
        
        return this.updateById(organization);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrg(Long id) {
        // 检查是否有子组织
        long childCount = this.count(new LambdaQueryWrapper<Organization>().eq(Organization::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("该组织下存在子组织，无法删除");
        }
        
        // 检查是否有用户关联
        long userCount = userOrgDao.selectCount(new LambdaQueryWrapper<UserOrg>().eq(UserOrg::getOrgId, id));
        if (userCount > 0) {
            throw new RuntimeException("该组织下存在用户，无法删除");
        }
        
        return this.removeById(id);
    }

    @Override
    public List<Organization> getUserOrgs(Long userId) {
        List<UserOrg> userOrgs = userOrgDao.selectList(
            new LambdaQueryWrapper<UserOrg>().eq(UserOrg::getUserId, userId)
        );
        
        if (CollUtil.isEmpty(userOrgs)) {
            return CollUtil.newArrayList();
        }
        
        List<Long> orgIds = userOrgs.stream()
            .map(UserOrg::getOrgId)
            .collect(Collectors.toList());
        
        return this.listByIds(orgIds);
    }

    /**
     * 构建组织树
     */
    private List<Organization> buildOrgTree(List<Organization> organizations) {
        List<Organization> tree = organizations.stream()
            .filter(org -> org.getParentId() == null || org.getParentId() == 0)
            .peek(org -> org.setChildren(getChildren(org, organizations)))
            .collect(Collectors.toList());
        
        return tree;
    }

    /**
     * 获取子组织
     */
    private List<Organization> getChildren(Organization parent, List<Organization> organizations) {
        return organizations.stream()
            .filter(org -> org.getParentId() != null && org.getParentId().equals(parent.getId()))
            .peek(org -> org.setChildren(getChildren(org, organizations)))
            .sorted((o1, o2) -> {
                int sort1 = o1.getSort() == null ? 0 : o1.getSort();
                int sort2 = o2.getSort() == null ? 0 : o2.getSort();
                return Integer.compare(sort1, sort2);
            })
            .collect(Collectors.toList());
    }
}